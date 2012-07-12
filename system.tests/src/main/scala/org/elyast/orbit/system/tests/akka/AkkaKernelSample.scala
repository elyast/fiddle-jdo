package org.elyast.orbit.system.tests.akka

import akka.kernel.Bootable
import akka.actor.ActorSystem
import akka.actor.Props

class AkkaKernelSample extends Bootable {

  val system = ActorSystem("hellokernel")
 
  def startup = {
    system.actorOf(Props[LocalActor]) ! Status(10)
  }
 
  def shutdown = {
    system.shutdown()
  }
}