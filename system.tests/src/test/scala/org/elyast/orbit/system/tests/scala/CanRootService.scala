package org.elyast.orbit.system.tests.scala
import cc.spray.can.model.{ ChunkedResponseStart, ChunkedMessageEnd }
import cc.spray.can.server.HttpServer
import cc.spray.http._
import cc.spray.io.ConnectionClosedReason
import cc.spray.typeconversion.ChunkSender
import cc.spray.SprayCanConversions._
import akka.dispatch.{ Promise, Future }
import akka.util.Duration
import akka.actor.ActorRef
import akka.spray.UnregisteredActorRef
import cc.spray._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import collection.JavaConverters._
import java.util.concurrent.atomic.{ AtomicInteger, AtomicBoolean }
import akka.actor.{ ActorLogging, Actor, ActorRef }

import cc.spray.http._
import cc.spray.util._

/**
 * A specialized [[cc.spray.RootService]] for connector-less deployment on top of the ''spray-can'' `HttpServer`.
 */
class CanRootService(val config: Config, firstService: ActorRef, moreServices: ActorRef*)
  extends Actor with ActorLogging with ErrorHandling {

  val rootPathConfig = config.getString("root-actor-path")
  val timeoutActorPathConfig = config getString "timeout-actor-path"

  protected val handler: RequestContext => Unit = moreServices.toList match {
    case Nil => handleOneService(firstService)
    case services => handleMultipleServices(firstService :: services)
  }

  protected val initialUnmatchedPath: String => String = rootPathConfig match {
    case "" => identityFunc
    case rootPath => { path =>
      if (path.startsWith(rootPath)) {
        path.substring(rootPath.length)
      } else {
        log.warning("Received request outside of configured root-path, request uri '{}', configured root path '{}'", path, rootPath)
        path
      }
    }
  }

  override def preStart() {
    log.debug("Starting spray RootService ...")
    cc.spray.http.warmUp()
    super.preStart()
  }

  override def postStop() {
    log.info("spray RootService stopped")
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.info("Restarting spray RootService because of previous {} ...", reason.getClass.getName)
  }

  override def postRestart(reason: Throwable) {
    log.info("spray RootService restarted");
  }

  protected def receive = {
    
    case request: can.model.HttpRequest => {
      try {
        handler(contextForSprayCanRequest(request))
      } catch handleExceptions(request)
    }
    case HttpServer.RequestTimeout(request) => {
      try {
        if (timeoutActorPathConfig == "")
          sender ! toSprayCanResponse(timeoutResponse(fromSprayCanRequest(request)))
        else
          context.actorFor(timeoutActorPathConfig) ! Timeout(contextForSprayCanRequest(request))
      } catch handleExceptions(request)
    }
    
  }

  protected def handleExceptions(context: RequestContext): PartialFunction[Throwable, Unit] = {
    case e: Exception => context.complete(responseForException(context.request, e))
  }

  protected def handleOneService(service: ActorRef)(context: RequestContext) {
    import context._
    log.debug("Received {} with one attached service, dispatching...", request)
    val newResponder = responder.withReject { rejections =>
      if (!rejections.isEmpty) log.warning("Non-empty rejection set received in RootService, ignoring ...")
      responder.complete(noServiceResponse(request))
    }
    service ! context.copy(responder = newResponder, unmatchedPath = initialUnmatchedPath(request.path))
  }

  protected def handleMultipleServices(services: List[ActorRef])(context: RequestContext) {
    import context._
    log.debug("Received {} with {} attached services, dispatching...", request, services.size)
    val responded = new AtomicBoolean(false)
    val rejected = new AtomicInteger(services.size)
    val newResponder = responder.copy(
      complete = { response =>
        if (responded.compareAndSet(false, true)) responder.complete(response)
        else log.warning("Received a second response for request '{}':\n\n{}\n\nIgnoring the additional response...", request, response)
      },
      reject = { rejections =>
        if (!rejections.isEmpty) log.warning("Non-empty rejection set received in RootService, ignoring ...")
        if (rejected.decrementAndGet() == 0) responder.complete(noServiceResponse(request))
      })
    val outContext = context.copy(responder = newResponder, unmatchedPath = initialUnmatchedPath(request.path))
    services.foreach(_ ! outContext)
  }

  protected def noServiceResponse(request: HttpRequest) =
    HttpResponse(404, "No service available for [" + request.uri + "]")

  protected def timeoutResponse(request: HttpRequest) =
    HttpResponse(500, "The server could not handle the request in the appropriate time frame (async timeout)")

  protected def handleExceptions(request: can.model.HttpRequest): PartialFunction[Throwable, Unit] = {
    case e: Exception => sender ! toSprayCanResponse(responseForException(request, e))
  }

  protected def contextForSprayCanRequest(request: can.model.HttpRequest) = {
    RequestContext(
      request = fromSprayCanRequest(request),
      remoteHost = "127.0.0.1", // TODO: extract from X-Remote-Addr header (see issue #95)
      responder = sprayCanAdapterResponder(sender))
  }

  protected def sprayCanAdapterResponder(client: ActorRef): RequestResponder = {
    RequestResponder(
      complete = response => client ! toSprayCanResponse(response),
      startChunkedResponse = { response =>
        client ! ChunkedResponseStart(toSprayCanResponse(response))
        new ChunkSender {
          def sendChunk(chunk: MessageChunk): Future[Unit] = {
            val promise = Promise[Unit]()(context.dispatcher)
            val ref = new UnregisteredActorRef(context) {
              def handle(message: Any, sender: ActorRef) {
                message match {
                  case _: HttpServer.AckSend => promise.success(())
                  case HttpServer.Closed(_, reason) =>
                    if (!promise.isCompleted) promise.tryComplete(Left(new ClientClosedConnectionException(reason)))
                }
              }
            }
            client.tell(toSprayCanMessageChunk(chunk), ref)
            promise
          }
          def close(extensions: List[ChunkExtension], trailer: List[HttpHeader]) {
            client ! ChunkedMessageEnd(extensions.map(toSprayCanChunkExtension), trailer.map(toSprayCanHeader))
          }
        }
      },
      resetConnectionTimeout = () => client ! HttpServer.SetIdleTimeout(Duration.Zero))
  }
}

