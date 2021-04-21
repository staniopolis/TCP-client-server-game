package by.stascala.services

import akka.actor.{Actor, ActorLogging, Props}
import by.stascala.api.CurrentBalance

object Persistent {
  def props(): Props = Props(new Persistent)

  final case class InitialCharge(name: String)

  final case class GetBalance(name: String)

  final case class Hold(playerName: String, tokensToHold: Long)

  final case class ResultBalance(playerName: String, holdAmount: Long, gameResult: Long)

  final case class BalanceUpdated(updatedBalance: (String, Long))

  final case class PlayerDisconnected(playerName: String)
}

class Persistent extends Actor with ActorLogging {
  log.info("Persistent service started")

  import Persistent._

  var balance: Map[String, (Long, Long)] = Map.empty

  override def receive: Receive = {
    case GetBalance(playerName) => authorizationHandler(playerName)
    case InitialCharge(playerName) => initialChargeHandler(playerName)
    case Hold(playerName, tokens) => holdTokensHandler(playerName, tokens)
    case ResultBalance(playerName, holdAmount, gameResult) => gameResultHandler(playerName, holdAmount, gameResult)
    case PlayerDisconnected(playerName) => playerDisconnectedHandler(playerName)
  }

  private def authorizationHandler(playerName: String): Unit = {
    println(balance(playerName))
    sender() ! CurrentBalance(balance(playerName)._1)
  }

  private def initialChargeHandler(playerName: String): Unit = {
    balance += (playerName -> (1000L, 0L))
    sender() ! CurrentBalance(balance(playerName)._1)
  }


  private def holdTokensHandler(playerName: String, tokens: Long): Unit = {
    if (balance(playerName)._1 >= tokens) {
      balance = balance.updated(playerName, (balance(playerName)._1 - tokens, balance(playerName)._2 + tokens))
      sender() ! LobbyRoom.TokensHold
    } else sender() ! LobbyRoom.InsufficientTokens(balance(playerName)._1)
  }

  private def gameResultHandler(playerName: String, holdAmount: Long, gameResult: Long): Unit = {
    balance = balance.updated(playerName, (balance(playerName)._1 + holdAmount + gameResult, balance(playerName)._2 - holdAmount))
    sender() ! BalanceUpdated(playerName, balance(playerName)._1)
  }

  private def playerDisconnectedHandler(playerName: String): Unit = {
    balance = balance.updated(playerName, (balance(playerName)._1 + balance(playerName)._2, 0L))
  }

}