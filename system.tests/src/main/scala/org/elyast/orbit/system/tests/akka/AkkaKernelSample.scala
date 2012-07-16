package org.elyast.orbit.system.tests.akka

import akka.kernel.Bootable
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory

class AkkaKernelSample extends Bootable {

  val reference = ConfigFactory.defaultReference(classOf[ActorSystem].getClassLoader)
  
  val system = ActorSystem("hellokernel", reference)
 
  def startup = {
    system.actorOf(Props[LocalActor]) ! Status(10)
  }
 
  def shutdown = {
    system.shutdown()
  }
}