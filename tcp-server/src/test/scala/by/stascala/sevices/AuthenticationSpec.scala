package by.stascala.sevices

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import by.stascala.GameSession
import by.stascala.api.CurrentBalance
import by.stascala.services.Authentication.{LogIn, SingUp}
import by.stascala.services.LobbyRoom.TokensHold
import by.stascala.services.Persistent.Hold
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

    "receive initial balance" in {
      testAuth ! SingUp("TestPlayer", "", testActor)
      expectMsg(CurrentBalance(1000L))
    }
    "receive player's current balance" in {
      testPers ! Hold("TestPlayer", 100L)
      expectMsg(TokensHold)
      testAuth ! LogIn("TestPlayer", "", testActor)
      expectMsg(CurrentBalance(900L))
    }
  }
}
