package org.elyast.orbit.system.tests.scala

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec

@RunWith(classOf[JUnitRunner])
class AkkaTest extends WordSpec with ShouldMatchers with MockitoSugar {

  "Akka" should {
    "bootstrap akka with mongo backend" in {

    }

    "bootstrap akka with file backend" in {

    }

    "bootstrap akka with zookeeper backend" in {

    }
    
    "bootstrap kernel" in {
      
    }
  }
  
  "Akka actor" should {

    "send to remote actor system" in {

    }

    "send message through typed actor to remote actor system with round robin" in {

    }
    
    "send with different serialization mechanism" in {
      
    }
    
    "show dataflow" in {
      
    }
    
    "run state machine" in {
      
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

}