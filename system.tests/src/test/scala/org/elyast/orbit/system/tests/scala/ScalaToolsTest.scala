package org.elyast.orbit.system.tests.scala

import org.scalatest.WordSpec
import scalaj.collection.Imports._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.eclipse.jetty.server.Server
import scalaz._
import Scalaz._
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import javax.jdo.PersistenceManagerFactory
import org.mockito.Mockito._
import org.mockito.Matchers.{ anyMap, any }
import org.slf4j.LoggerFactory
import javax.jdo.JDOHelper
import java.util.UUID

@RunWith(classOf[JUnitRunner])
class ScalaToolsTest extends WordSpec with ShouldMatchers with MockitoSugar {

  val LOGGER = LoggerFactory.getLogger(classOf[ScalaToolsTest])

  "ScalaJ" should {

    "transform between scala and java collections" in {
      val javalist = new java.util.ArrayList[String];
      javalist.add("Some");
      val scalaOne = javalist.asScala
      assert(scalaOne.tail.size === 0)
    }
  }

  "Scalaz" should {
    "fold sth" in {
      (List(1, 2, 3) ∗ (List(7, _))) assert_=== List(7, 1, 7, 2, 7, 3)
    }

  }

  "Case classes" should {
    "work in Datanucleus" in {
      val bunny = TestingBunny("john", 10)
      LOGGER.info("in the method")
      val cl = getClass.getClassLoader
      val properties = Map("datanucleus.ConnectionDriverName" -> "org.h2.Driver",
        "datanucleus.ConnectionURL" -> "jdbc:h2:mem:test",
        "datanucleus.ConnectionUserName" -> "sa",
        "datanucleus.ConnectionPassword" -> "",
        "datanucleus.autoCreateSchema" -> "true",
        "javax.jdo.PersistenceManagerFactoryClass" -> "org.datanucleus.api.jdo.JDOPersistenceManagerFactory",
        "datanucleus.primaryClassLoader" -> cl)
      val pmf = JDOHelper.getPersistenceManagerFactory(properties asJava, cl)
      val pm = pmf.getPersistenceManager()
      val tx = pm.currentTransaction()
      tx.begin()
      pm.makePersistent(TestingBunny("Name" + UUID.randomUUID().toString(), 10))
      tx.commit()
      pm.close()
      val pm2 = pmf.getPersistenceManager()
      val tx1 = pm2.currentTransaction()
      tx1.begin();
      for (i <- 1 to 3) {
        val newQuery = pm2.newQuery(classOf[TestingBunny])
        val result = newQuery.execute() match {
          case execute: List[TestingBunny] => Some(execute(0))
          case _ => None
        }
        LOGGER.info("Bunny list: {}", result);
      }
      tx1.commit();
      pmf.close();
    }
  }

  "ScalaTest" should {
    "work with Mockito" in {
      val jdo = mock[TestingMock]
      val underTest = new Usage(jdo)
      val pmf = mock[PersistenceManagerFactory]
      when(jdo.getPersistenceManager(any(classOf[java.util.Map[String, Object]]), any())) thenReturn (pmf)
      val cl = getClass.getClassLoader
      val properties: java.util.Map[String, Object] = Map("datanucleus.ConnectionDriverName" -> "org.h2.Driver", "xxx" -> "yyy") asJava
      val pmf2 = underTest.sayHi(properties, cl)
      pmf2 should not equal (null)
      verify(jdo) getPersistenceManager (properties, cl)
    }

  }

}