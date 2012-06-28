package org.elyast.orbit.system.tests.scala

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import akka.actor.{ Props, ActorSystem }
import cc.spray.{ SprayCanRootService, HttpService }
import cc.spray.io.IoWorker
import cc.spray.can.server.HttpServer
import cc.spray.io.pipelines.MessageHandlerDispatch
import cc.spray._
import com.typesafe.config.ConfigFactory
import dispatch._
import cc.spray.http.HttpProtocol
import java.lang.Thread
import akka.util.duration._
import akka.util.Timeout
import cc.spray.test.SprayTest
import cc.spray.http.HttpRequest
import cc.spray.http.HttpMethods._

trait HelloService extends Directives {

  val helloService = {
    path("") {
      get { _.complete("Say hello to Spray!") }
    }
  }

}

@RunWith(classOf[JUnitRunner])
class SprayComponentTest extends WordSpec with ShouldMatchers with SprayTest with HelloService {

  val reference = ConfigFactory.defaultReference(classOf[ActorSystem].getClassLoader)
  val spraybase = ConfigFactory.defaultReference(classOf[HttpProtocol].getClassLoader)
  val sprayio = ConfigFactory.defaultReference(classOf[IoWorker].getClassLoader)
  val spraycan = ConfigFactory.defaultReference(classOf[HttpServer].getClassLoader)
  val sprayserver = ConfigFactory.defaultReference(classOf[HttpService].getClassLoader)

  "Spray server" should {

    "test with testing" in {
      testService(HttpRequest(GET, "/")) {
        helloService
      }.response.content.as[String] should equal(Right("Say hello to Spray!"))
    }
    
    "publish web service through spray-can" in {
      println(sprayio.entrySet)
      val config = sprayserver.withFallback(sprayio).withFallback(spraycan).withFallback(spraybase).withFallback(reference)
      println(config.getConfig("spray.servlet"))
      val system = ActorSystem("default", config, getClass.getClassLoader)
      val mainModule = new HelloService {
        implicit def actorSystem = system
      }
      val httpService = system.actorOf(props = Props(new HttpService(mainModule.helloService)), name = "http-service")
      val rootService = system.actorOf(props = Props(new CanRootService(config, httpService)), name = "root-service")
      val ioWorker = new IoWorker(system, config).start()
      val sprayCanServer = system.actorOf(Props(new HttpServer(ioWorker, MessageHandlerDispatch.SingletonHandler(rootService), config)), name = "http-server")
      
      println("SprayService yupi")
        
//      val th = new Thread(new Runnable() {
//
//        override def run(): Unit = {
          sprayCanServer ! HttpServer.Bind("localhost", 8213)
//        }
//      })
//      println("Before starting thread")
//      th.start

      println("Binded")
      system.registerOnTermination {
        ioWorker.stop()
      }
      
      println("After register termination")
      Http(url("http://localhost:8213") >>> System.out)
      println("After consuming")
      system.shutdown
      system.awaitTermination(1 second)
      println("Await termination")
    }
  }

  "Spray client" should {
    "consume web service" in {

    }
  }
}