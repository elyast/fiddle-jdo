package org.elyast.orbit.system.tests.scala

import javax.jdo.PersistenceManagerFactory

trait TestingMock {

  def getPersistenceManager(properties: java.util.Map[String, Object], cl: ClassLoader): PersistenceManagerFactory
}