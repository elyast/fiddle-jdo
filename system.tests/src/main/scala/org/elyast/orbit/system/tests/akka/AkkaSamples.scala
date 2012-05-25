package org.elyast.orbit.system.tests.akka

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.duration._
import akka.util.Timeout
import akka.dispatch.Await
import akka.event.LoggingAdapter
import akka.event.Logging

case class Status(number: Int)
case class AkkaMessage(name: String, status: Status)

class LocalActorConsumer extends Actor {
  val log = Logging.getLogger(context.system, this);
  
  def receive = {
    case Status(s) => 
      log.info("LocalActorConsumer {}", s)
      sender ! Status(s*10)
  }
}

class LocalActor extends Actor {
  
  val log = Logging.getLogger(context.system, this);
  val passe = context.actorOf(Props[LocalActorConsumer], name = "localActorConsumer")
  
  def receive = {
    case AkkaMessage("xxx", status) => 
      val future = ask(passe, status)(1 second).mapTo[Status]
      log.info("LocalActor {}",  future)
      val stats = Await.result(future, 2 second)
      sender ! stats
    case Status(s) => log.info("Got status {}", s)
    case _ => log.info("anything")
  }
}