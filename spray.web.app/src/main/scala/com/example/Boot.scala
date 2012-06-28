package com.example

import akka.actor.{Props, ActorSystem}
import cc.spray.{RootService, HttpService}


// this class is instantiated by the servlet initializer,
// which also creates and shuts down the ActorSystem passed
// as an argument to this constructor
class Boot(system: ActorSystem) {

  val mainModule = new HelloService {
    implicit def actorSystem = system
    // bake your module cake here
  }
  val service = system.actorOf(
    props = Props(new HttpService(mainModule.helloService)),
    name = "service1"
  )
  val rootService = system.actorOf(
    props = Props(new RootService(service)),
    name = "spray-root-service" // must match the name in the config so the ConnectorServlet can find the actor
  )
  system.registerOnTermination {
    // put additional cleanup code here
    system.log.info("Application shut down")
  }

}