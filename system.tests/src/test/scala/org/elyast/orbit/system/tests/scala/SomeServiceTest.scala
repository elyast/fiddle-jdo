package org.elyast.orbit.system.tests.scala
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.slf4j.LoggerFactory
import javax.jdo.JDOHelper
import collection.JavaConversions._
import java.util.UUID
import java.util.List
import javax.jdo.JDOHelper
import javax.jdo.PersistenceManagerFactory
import javax.jdo.JDOHelper
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers.{anyMap, any}

@RunWith(classOf[JUnitRunner])
class SomeServiceTest extends FlatSpec with ShouldMatchers with MockitoSugar {

  val LOGGER = LoggerFactory.getLogger(classOf[SomeServiceTest])

  "SomeService" should "say Hi in return to hello" in {
    "Hi" should equal("Hi")
  }

  it should "say 'See you' in return to bye" in {
    val bunny = TestingBunny("john", 10)
    bunny.name should equal("john")
  }

  "Datanucleus for RDBMS" should "write and fetch bunnies" in {
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
    val pmf = JDOHelper.getPersistenceManagerFactory(properties, cl)
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
			  case execute: List[TestingBunny] => Some(execute.iterator().next())
			  case _ => None
			}
			LOGGER.info("Bunny list: {}", result);
		}
	tx1.commit();
    pmf.close();
  }
  
  "mock" should "just work" in {
     
     
    val jdo = mock[TestingMock]
    val underTest = new Usage(jdo)
    val pmf = mock[PersistenceManagerFactory]
    when(jdo.getPersistenceManager(any(classOf[java.util.Map[String, Object]]), any())) thenReturn(pmf)
   
    val cl = getClass.getClassLoader
    val properties = Map("datanucleus.ConnectionDriverName" -> "org.h2.Driver", "xxx" -> "yyy")
    val pmf2 = underTest.sayHi(properties, cl)
    pmf2 should not equal (null)
    verify(jdo) getPersistenceManager(properties, cl)
  }
}