package org.elyast.orbit.system.tests.akka

trait DBRepository {

  def save(value: Int) : Unit
}

class DBRepositoryDummy(val name:String) extends DBRepository {
  override def save(value: Int) : Unit = {
    println("Value being sent by " + name + " : " + value)
  }
}