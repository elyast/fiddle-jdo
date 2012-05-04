package org.elyast.orbit.system.tests.scala

import javax.jdo.annotations.PersistenceCapable
import javax.jdo.annotations.PrimaryKey

@PersistenceCapable
case class TestingBunny(@PrimaryKey name: String, age: Int) {

}