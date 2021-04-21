package by.stascala.services

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import by.stascala.GameSession
import by.stascala.api.{Failed, Info}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object LobbyRoom {
  def props(persistent: ActorRef): Props = Props(new LobbyRoom(persistent))

  final case class JoinGame(gameType: String, player: String, session: ActorRef)

  final case class PlayerDisconnected(playerSession: ActorRef)

  final case class InsufficientTokens(balance: Long)

  final case object TokensHold
}

class LobbyRoom(persistent: ActorRef) extends Actor with ActorLogging {
  log.info("LobbyRoom service started")

  implicit val timeout: Timeout = 1.second

  import LobbyRoom._
  import context.dispatcher

  var gameWaitList: Map[String, Map[String, ActorRef]] = Map.empty

  override def receive: Receive = {
    case JoinGame(gameOption, playerName, playerSession) =>
      newGameRequestHandler(gameOption, playerName, playerSession)
    case PlayerDisconnected(playerSession) => playerDisconnectedHandler(playerSession)
  }


  private def newGameRequestHandler(gameOption: String, playerName: String, playerSession: ActorRef): Unit = {
    // Load game config
    val gameConfig = ConfigFactory.load("game.conf").getConfig(gameOption)
    // Hold tokens
    persistent ? Persistent.Hold(playerName, gameConfig.getLong("playCost")) onComplete {
      case Success(result) => result match {
        case LobbyRoom.TokensHold =>
          gameWaitList.get(gameOption) match {
            case Some(_) =>
              gameWaitList = gameWaitList.updated(gameOption, gameWaitList(gameOption) + (playerName -> playerSession))
              // If number of players in wait list is enough to start game session, we start GameSession actor
              if (gameWaitList(gameOption).size == gameConfig.getInt("numberOfPlayers")) {
                gameWaitList(gameOption).foreach(player =>
                  player._2 ! Info(s"Staring $gameOption game session...\n"))
                context.actorOf(GameSession.props(
                  gameWaitList(gameOption),
                  gameConfig.getInt("handSize"),
                  gameConfig.getLong("foldCost"),
                  gameConfig.getLong("playCost"),
                  persistent))
                gameWaitList -= gameOption
                // Else just inform player
              } else {
                playerSession ! Info(s"Waiting for other players...")
              }
            case None =>
              gameWaitList += (gameOption -> Map(playerName -> playerSession))
              playerSession ! Info(s"Waiting for other player(s)...")

          }
        case InsufficientTokens(balance) =>
          playerSession ! Failed(s"Insufficient tokens. Balance must be over ${gameConfig.getLong("playCost")} tokens." +
            s" Current balance $balance tokens")
      }
      case Failure(exception) => playerSession ! Failed(exception.getMessage)
    }
  }

  // Release tokens and delete disconnected player from wait list
  private def playerDisconnectedHandler(playerSession: ActorRef): Unit = {
    val playerGames = gameWaitList.map(_._2.filter(_._2 == playerSession))
    if (playerGames.nonEmpty)
      persistent ! Persistent.PlayerDisconnected(playerGames.head.head._1)
    gameWaitList = gameWaitList.map(gameOption =>
      (gameOption._1, gameOption._2.filter(_._2 != playerSession)))
  }
}
