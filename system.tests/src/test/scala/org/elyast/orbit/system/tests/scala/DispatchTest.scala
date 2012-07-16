package org.elyast.orbit.system.tests.scala

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import org.eclipse.jetty.server.Server
import dispatch._
import oauth._
import oauth.OAuth._
import dispatch.Request._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DispatchTest extends WordSpec with ShouldMatchers with BeforeAndAfter {
  
  val jetty = new Server(56784);
  val localUrl = url("http://localhost:56784")
  val bunnyJson = """[{"surname":"Name","class":"org.elyast.orbit.system.tests.Bunny"}]"""
  val bunnyXml = """<bunny><surname>Name</surname><class>org.elyast.orbit.system.tests.Bunny</class></bunny>"""

  before {
    jetty.setHandler(new JsonContentHandler());
    jetty.start();
  }
  
  after {
     jetty.stop();
  }
  
  "Dispatch" should {
    "send http request in synchronous way" in {
      val result = Http(localUrl as_str)
      result should equal(bunnyJson)
    }

    "send http request in asynchronous way" in {
      val http = new nio.Http
      val future = http(localUrl as_str)
      val result = future()
      http.shutdown()
      result should equal(bunnyJson)
    }

    "send oauth request" in {
      val consumer = Consumer("23", "123")
      val token = Token("xxx", "yyyy")
      val request = localUrl <@ (consumer, token)
      val result = Http(request as_str)
      result should equal(bunnyJson)
    }
    
    "parse xml response" in {
      val jetty = new Server(56789);
      jetty.setHandler(new XmlContentHandler());
      jetty.start();
      val response = Http(url("http://localhost:56789") <> { node =>
         node \ "bunny"
      })
      response.toString should equal(bunnyXml) 
      jetty.stop();
    }
    
    "post json data" in {
      val result = Http(localUrl << bunnyJson as_str)
      result should equal(bunnyJson)
    }

  }
}