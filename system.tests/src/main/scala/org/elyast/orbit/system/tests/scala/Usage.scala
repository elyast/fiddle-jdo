package org.elyast.orbit.system.tests.scala

class Usage(val pmf: TestingMock) {

  def sayHi(properties: java.util.Map[String, Object], cl: ClassLoader): Unit = pmf.getPersistenceManager(properties, cl);
}