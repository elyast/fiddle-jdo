package org.elyast.orbit.system.tests.scala

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import org.eclipse.jetty.server.Server
import dispatch._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DispatchTest extends WordSpec with ShouldMatchers with BeforeAndAfter {
  
  val jetty = new Server(56784);
  val localUrl = url("http://localhost:56784")
  
  before {
    jetty.setHandler(new SomeHandler());
    jetty.start();
  }
  
  after {
     jetty.stop();
  }
  
  "Dispatch" should {
    "send http request in synchronous way" in {
      Http(localUrl >>> System.out)
    }

    "send http request in asynchronous way" in {

    }

    "send oauth request" in {

    }

   
  }
}