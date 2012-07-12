package org.elyast.orbit.system.tests.scala

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import akka.dispatch.Await
import org.elyast.orbit.system.tests.akka.LocalActor
import org.elyast.orbit.system.tests.akka.AkkaMessage
import org.elyast.orbit.system.tests.akka.Status
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import org.scalatest.BeforeAndAfter
import org.elyast.orbit.system.tests.akka.DBRepository
import org.elyast.orbit.system.tests.akka.DBRepositoryDummy
import akka.routing.FromConfig
import akka.dispatch.Promise
import akka.dispatch._
import Future.flow
import akka.zeromq.SocketType
import akka.zeromq.Bind
import org.elyast.orbit.system.tests.akka.DBRepositoryRemote
import java.io.File
import org.elyast.orbit.system.tests.akka.AkkaKernelSample

@RunWith(classOf[JUnitRunner])
class AkkaTest extends WordSpec with ShouldMatchers with BeforeAndAfter {

  implicit val timeout = Timeout(5 seconds)

  val log = LoggerFactory.getLogger("AkkaTest")

  val reference = ConfigFactory.defaultReference(classOf[ActorSystem].getClassLoader)
  val system1 = ConfigFactory.parseResources(getClass.getClassLoader, "/system1.conf").withFallback(reference)
  val system2 = ConfigFactory.parseResources(getClass.getClassLoader, "/system2.conf").withFallback(reference)

  val fileBased = ConfigFactory.parseResources(getClass.getClassLoader, "/fileBased.conf").withFallback(reference)
  val routing = ConfigFactory.parseResources(getClass.getClassLoader, "/routing.conf").withFallback(reference)

  "Akka" should {

    "bootstrap akka with file backend" in {
      val sys = ActorSystem("fileBased", fileBased, getClass.getClassLoader)
      val myActor = sys.actorOf(Props[LocalActor].withDispatcher("my-dispatcher"), name = "myactor")
      val future = ask(myActor, AkkaMessage("xxx", Status(10))).mapTo[Status]
      val result = Await.result(future, 2 seconds)
      result should equal(Status(100))
      new File("target/mailbox_user_myactor").exists should equal(true)   
 }

    "bootstrap kernel" in {
    	val kernel = new AkkaKernelSample()
    	kernel.startup
    	kernel.shutdown
    }

  }

  "Akka actor" should {

    "send to remote actor system" in {
      log.info("Remote start")
      val sys = ActorSystem("system1", system1, getClass.getClassLoader)
      val remotesys = ActorSystem("system2", system2, getClass.getClassLoader)
      calculateStatus(sys) should equal(Status(100))
      log.info("Remote stop")
      sys.shutdown
      sys.awaitTermination(10 seconds)
      remotesys.shutdown
      remotesys.awaitTermination(10 seconds)
    }

    "send message through typed actor with round robin" in {
      val sys = ActorSystem("systemRouter", routing, getClass.getClassLoader)
      val extension = TypedActor(sys)
      val props = TypedProps(classOf[DBRepository], new DBRepositoryDummy("foo"))
      val routingProps = Props[LocalActor].withRouter(FromConfig())
      val actorx = sys.actorOf(routingProps, "router")
      val repo: DBRepository = extension.typedActorOf(props, actorx)

      for (i <- 0 to 10) {
        repo.save(i)
      }
    }

    "send to remote typed actor" in {
      val sys = ActorSystem("system1", system1, getClass.getClassLoader)
      val remotesys = ActorSystem("system2", system2, getClass.getClassLoader)
      val propsRemote = TypedProps(classOf[DBRepository], new DBRepositoryDummy("bar"))
      val remoteRepo: DBRepository = TypedActor(remotesys).typedActorOf(propsRemote)
      val props = TypedProps(classOf[DBRepository], new DBRepositoryRemote(remoteRepo, "foo"))
      val repo: DBRepository = TypedActor(sys).typedActorOf(props)
      
      repo.save(10)
      sys.shutdown
      sys.awaitTermination(10 seconds)
      remotesys.shutdown
      remotesys.awaitTermination(10 seconds)
    }

    "show dataflow" in {
      implicit val sys = ActorSystem("default", reference)
      val a = Promise[Int]()
      val b = Promise[Int]()
      val c = flow {
        a() + b()
      }
      flow {
        a << 3
        b << 4
      }
      val result = Await.result(c, 2 seconds)
      println("Data flow result: " + result)
      result should equal(7)
    }

    "send to local actor" in {
      log.info("Local start")
      val sys = ActorSystem("default", reference)
      calculateStatus(sys) should equal(Status(100))
      log.info("local stop")
    }
  }
  import akka.agent.Agent
  import akka.util.duration._
  import akka.util.Timeout
  import scala.concurrent.stm._

  def transfer(from: Agent[Int], to: Agent[Int], amount: Int): Boolean = {
    atomic { txn =>
      if (from.get < amount) false
      else {
        from send (_ - amount)
        to send (_ + amount)
        true
      }
    }
  }

  "Akka agent" should {
    "communicate through integers" in {
      implicit val sys = ActorSystem("default", reference)
      val from = Agent(100)
      val to = Agent(20)
      val ok = transfer(from, to, 50)

      implicit val timeout = Timeout(5 seconds)
      val fromValue = from.await // -> 50
      val toValue = to.await // -> 70
      fromValue should equal(50)
      toValue should equal(70)
      from.close()
      to.close()
    }
  }
  import akka.zeromq._

  //assumption is to have zeromq installed
  "Akka zeromq" should {
    "communicate through zeromq sockets" in {
      val system = ActorSystem("default", reference)
      val listener = system.actorOf(Props(new Actor {
        def receive: Receive = {
          case Connecting => //...
          case m: ZMQMessage => println(m)
          case _ => //...
        }
      }))
      val pubSocket = system.newSocket(SocketType.Pub, Bind("tcp://127.0.0.1:21231"))

      val subSocket = system.newSocket(SocketType.Sub, Listener(listener), Connect("tcp://127.0.0.1:21231"), SubscribeAll)
      val payload = "Hello world"

      pubSocket ! ZMQMessage(Seq(Frame("foo.bar"), Frame(payload)))
    }
  }

  def calculateStatus(system: ActorSystem) = {
    val actor = system.actorOf(Props[LocalActor], name = "localActor")
    val future = ask(actor, AkkaMessage("xxx", Status(10))).mapTo[Status]
    Await.result(future, 2 seconds)
  }
}