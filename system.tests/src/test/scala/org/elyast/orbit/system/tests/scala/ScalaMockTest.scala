package org.elyast.orbit.system.tests.scala

import org.scalamock.ProxyMockFactory
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import org.scalamock.scalatest.MockFactory
import javax.jdo.PersistenceManagerFactory
import scalaj.collection.Imports._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalamock.ClassLoaderStrategy
import org.scalamock.classClassLoaderStrategy

object currentClassClassLoaderStrategy extends ClassLoaderStrategy {
  def getClassLoader(interfaces: Class[_]*) = currentClassClassLoaderStrategy.getClass.getClassLoader
}

@RunWith(classOf[JUnitRunner])
class ScalaMockTest extends WordSpec with ShouldMatchers with MockFactory with ProxyMockFactory {
  ClassLoaderStrategy.default = currentClassClassLoaderStrategy;

  "ScalaMock" should {
    "mock function" in {
      val callMe = mockFunction[Int, String]
      callMe expects (42) returning "Forty two" once
      val result = callMe(42)
      result should equal("Forty two")
    }

    "mock interface" in {

      val jdo = mock[TestingMock]
      val underTest = new Usage(jdo)
      val pmf = mock[PersistenceManagerFactory]
      val properties: java.util.Map[String, Object] = Map("datanucleus.ConnectionDriverName" -> "org.h2.Driver", "xxx" -> "yyy").asJava
      val cl = getClass.getClassLoader
      jdo expects 'getPersistenceManager withArgs (properties, cl) returning pmf
      val pmf2 = underTest.sayHi(properties, cl)
      pmf2 should not equal (null)
    }
  }
}