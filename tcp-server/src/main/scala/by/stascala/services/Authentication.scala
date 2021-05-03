package by.stascala.services

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import by.stascala.api.{AuthFailed, CurrentBalance, Failed}

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Authentication {
  def props(persisActor: ActorRef): Props =
    Props(new Authentication(persisActor))

  final case class LogIn(playerName: String, password: String, playerSession: ActorRef)

  final case class SignUp(playerName: String, password: String, playerSession: ActorRef)
}

class Authentication(persistent: ActorRef) extends Actor with ActorLogging {
  log.info("Authentication service started")
  import Authentication._
  import context.dispatcher

  implicit val timeout: Timeout = 1.second
  var authenticationData: Map[String, String] = Map.empty

  override def receive: Receive = {
    case LogIn(playerName, password, playerSession) => logInHandler(playerName, password, playerSession)
    case SignUp(playerName, password, playerSession) => singUpHandler(playerName, password, playerSession)
  }


  private def singUpHandler(playerName: String, password: String, playerSession: ActorRef): Unit = {
    if (authenticationData.contains(playerName))
      playerSession ! AuthFailed(s"Player with this name: $playerName already exist. Please choose another name or LogIn")
    else {
      authenticationData += (playerName-> password)
//      playerSession ! AuthSuccess(s"Player $playerName successfully registered")
      (persistent ? Persistent.InitialCharge(playerName)).mapTo[CurrentBalance] onComplete{
        case Success(result) => playerSession ! CurrentBalance(result.balance)
        case Failure(exception) => playerSession ! Failed(exception.getMessage)
      }
    }
  }

  private def logInHandler(playerName: String, password: String, playerSession: ActorRef): Unit = {
    if (authenticationData.contains(playerName) && authenticationData(playerName) == password) {
      (persistent ? Persistent.GetBalance(playerName)). mapTo[CurrentBalance] onComplete{
        case Success(result) => playerSession ! CurrentBalance(result.balance)
        case Failure(exception) => playerSession ! Failed(exception.getMessage)
      }
    } else playerSession ! AuthFailed(s"Wrong login or password. Please try again or SingUp")
  }
}
