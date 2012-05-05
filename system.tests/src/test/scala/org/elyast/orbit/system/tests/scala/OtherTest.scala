package org.elyast.orbit.system.tests.scala

import org.scalatest.WordSpec
import scalaj.collection.Imports._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import dispatch._
import org.eclipse.jetty.server.Server

@RunWith(classOf[JUnitRunner])
class OtherTest extends WordSpec {
  "A Stack" should {

    "pop values" in {
      val javalist = new java.util.ArrayList[String];
      javalist.add("Some");
      val scalaOne = javalist.asScala
      assert(scalaOne.tail.size === 0) 
    }
    
    "call ambulance" in {
      val jetty = new Server(56784);
	  jetty.setHandler(new SomeHandler());
	  jetty.start();
      val localUrl = url("http://localhost:56784")
      Http(localUrl >>> System.out)
      jetty.stop();
    }

  }
}