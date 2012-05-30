package org.elyast.orbit.system.tests.scala

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import akka.dispatch.Await
import org.elyast.orbit.system.tests.akka.LocalActor
import org.elyast.orbit.system.tests.akka.AkkaMessage
import org.elyast.orbit.system.tests.akka.Status
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import org.scalatest.BeforeAndAfter

@RunWith(classOf[JUnitRunner])
class AkkaTest extends WordSpec with ShouldMatchers with BeforeAndAfter {

  implicit val timeout = Timeout(5 seconds)
  
  val log = LoggerFactory.getLogger("AkkaTest")
  
  val reference = ConfigFactory.defaultReference(classOf[ActorSystem].getClassLoader)
  val system1 = ConfigFactory.parseResources(getClass.getClassLoader, "/system1.conf").withFallback(reference)
  val system2 = ConfigFactory.parseResources(getClass.getClassLoader, "/system2.conf").withFallback(reference)

  "Akka" should {

    "bootstrap akka with file backend" in {

    }

    "bootstrap kernel" in {

    }
    
    "bootstrap with zeromq" in {      
      
    }
  }

  "Akka actor" should {

    "send to remote actor system" in {
      log.info("Remote start")
      val sys = ActorSystem("system1", system1, getClass.getClassLoader)
      val remotesys = ActorSystem("system2", system2, getClass.getClassLoader)
      calculateStatus(sys) should equal(Status(100))
      log.info("Remote stop")
    }

    "send message through typed actor to remote actor system with round robin" in {

    }

    "send with different serialization mechanism" in {

    }

    "show dataflow" in {

    }

    "run state machine" in {

    }

    "send to local actor" in {
      log.info("Local start")
      val sys = ActorSystem("default", reference)
      calculateStatus(sys) should equal(Status(100))
      log.info("local stop")
    }
  }

  "Akka future" should {
    "compose long running tasks" in {

    }
  }

  "Akka transactor" should {
    "access shared memory" in {

    }
  }

  "Akka agent" should {
    "communicate through integers" in {

    }
  }

  def calculateStatus(system: ActorSystem) = {
    val actor = system.actorOf(Props[LocalActor], name = "localActor")
    val future = ask(actor, AkkaMessage("xxx", Status(10))).mapTo[Status]
    Await.result(future, 2 seconds)
  }
}