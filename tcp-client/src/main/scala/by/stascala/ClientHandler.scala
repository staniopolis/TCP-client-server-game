package by.stascala

import akka.actor.{Actor, ActorLogging, Props}
import by.stascala.api._
import by.stascala.client.Serialization

import scala.annotation.tailrec
import scala.io.StdIn.readLine

object ClientHandler {
  def props(): Props =
    Props(new ClientHandler)

  final case class ConnectionFailed(failMsg: String)

  final case class SessionClosed(closeMsg: String)

  final case object Start

  final case object Exit
}


class ClientHandler extends Actor with Serialization with ActorLogging {

  import ClientHandler._

  val cardName: Map[Int, String] = Map(
    14 -> "Ace",
    13 -> "King",
    12 -> "Queen",
    11 -> "Jack",
    10 -> "Ten",
    9 -> "Nine",
    8 -> "Eight",
    7 -> "Seven",
    6 -> "Six",
    5 -> "Five",
    4 -> "Four",
    3 -> "Three",
    2 -> "Two")
  val cardSuit: Map[Char, String] = Map(
    'h' -> "Hearts",
    'd' -> "Diamonds",
    'c' -> "Clubs",
    's' -> "Spades")

  def receive: Receive = {
    case Start => authHandler()
    case ConnectionFailed(msg) =>
      log.info(msg + s", terminating ${this.getClass.getName} actor")
      context.stop(self)
    case SessionClosed(msg) =>
      log.info(s"Session closed. Error: $msg. Terminating ${this.getClass.getName} actor")
      println("Buy buy")
      context.system.terminate()
  }

  @tailrec
  private def authHandler(): Unit = {
    println("\n" +
      "---------------------\n" +
      "Enter 1 to LogIn\n" +
      "Enter 2 to SignUp\n" +
      "Enter 3 to Exit\n"+
      "---------------------\n"
    )
    val authOption = readLine("Enter your choice: ")
    if (authOption.equals("3"))
      sender() ! Exit
    else if (authOption.equals("2") || authOption.equals("1")) {
      val playerName = readLine("Please, enter your name: ")
      context.become(authorized(playerName))
      val password = readLine("Please, enter password: ")
      val authData = (authOption, playerName, password)
      authData._1 match {
        case "1" => sender() ! LogIn(authData._2, authData._3)
        case "2" => sender() ! SingUp(authData._2, authData._3)
      }
    }
    else {
      println(s"Wrong choice. Try again\n")
      authHandler()
    }
  }

  def authorized(playerName: String): Receive = {
    case AuthSuccess(msg) =>
      println(msg)
    case AuthFailed(msg) =>
      println(msg + "\n")
      println("Please make your choice:\n")
      authHandler()
    case CurrentBalance(balance) =>
      println(s"\nYou current balance is: $balance tokens\n")
      println("Please choose game type or exit:\n")
      gameOptionHandler(playerName)
    case Info(msg) =>
      println(msg)
    case Failed(msg) =>
      println(msg)
      gameOptionHandler(playerName)
    case Hand(round, hand) =>
      val cards = hand.map(card => (cardName(card._1), cardSuit(card._2)))
      val strongestCardValue = hand.map(card => card._1).max
      println(s"Round #$round \n")
      println(s"Your hand: ${cards.map(card => card._1 + " of " + card._2).mkString(", ")}.\n" +
        s"Strongest card(s) value: $strongestCardValue\n")
      println("Please, make your choice:\n")
      gameSessionChoice(playerName)
    case Fold(balance, delta) =>
      println(s"You fold you cards and loose ($delta) tokens.\n" +
        s"You current balance: $balance tokens\n")

    case Loss(_, balance, delta) =>
      println(s"You loose $delta tokens.\n" +
        s"Your current balance: $balance tokens\n")

    case Win(strongestCard, balance, delta) =>
      println(s"Congratulations! You win $delta tokens. Game strongest card: ${cardName(strongestCard._1) + " of " + cardSuit(strongestCard._2)}.\n" +
        s"Your current balance: $balance tokens\n")
    case CompleteGameSession(winner, strongestCard) =>
      if (winner.isEmpty) println("!!!Game session ended. No winner defined!!!\n")
      else
        println(s"Game session ended. " +
          s"!!! The winner is $winner with card:" +
          s" ${cardName(strongestCard._1) + " of " + cardSuit(strongestCard._2)}!!!\n")
      gameOptionHandler(playerName)
    case SessionClosed(msg) =>
      log.info(s"Session closed. Error: $msg. Terminating ${this.getClass.getName} actor\n")
      println("Buy buy\n")
      context.system.terminate()
  }

  @tailrec
  private def gameOptionHandler(playerName: String): Unit = {
    println(
      "Enter 1 to start Single card game\n" +
        "Enter 2 to start Double card game\n" +
        "Enter 3 start Other card game\n" +
        "Enter 4 to Exit\n")
    val choice = readLine("Game type / exit: ")
    choice match {
      case "1" => sender() ! JoinGame("single-card-game", playerName)
      case "2" => sender() ! JoinGame("double-card-game", playerName)
      case "3" => sender() ! JoinGame("other-card-game", playerName)
      case "4" => sender() ! Exit
      case _ =>
        println(s"Wrong choice. Try again\n")
        gameOptionHandler(playerName)
    }
  }

  @tailrec
  private def gameSessionChoice(playerName: String): Unit = {
    println(
      "Enter 1 to Fold hand\n" +
        "Enter 2 to Play hand\n")
    val choice = readLine("Your choice: ")
    if (choice.equals("1") || choice.equals("2"))
      sender() ! PlayerDecision((playerName, choice.toInt))
    else {
      println(s"Wrong choice. Try again\n")
      gameSessionChoice(playerName)
    }
  }
}