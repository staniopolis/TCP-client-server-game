package by.stascala.sevices

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import by.stascala.api.{CurrentBalance, SingUp}
import by.stascala.services.{Authentication, Persistent}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AuthenticationSpec extends TestKit(ActorSystem("MySpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val testPers = system.actorOf(Persistent.props())
  val testAuth = system.actorOf(Authentication.props(testPers))


  "Authentication actor" must {

    "send initial balance 1000 tokens" in {
      testAuth ! SingUp("TestPlayer", "123")
      expectMsg(CurrentBalance(1000L))
    }

  }

}
