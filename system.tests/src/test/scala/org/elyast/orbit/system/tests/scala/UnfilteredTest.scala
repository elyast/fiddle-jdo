package org.elyast.orbit.system.tests.scala

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import dispatch._
import dispatch.Request._

@RunWith(classOf[JUnitRunner])
class UnfilteredTest extends WordSpec with ShouldMatchers {

  "Unfiltered" should {
    "serve web services through servlet" in {
      val localUrl = url("http://localhost:8080") / "unfiltered"
      val result = Http(localUrl as_str)
      result should include("""<p> What say you? </p>""")
    }
    
    "serve web services through netty" in {
      
    }
  }
}