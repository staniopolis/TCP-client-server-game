package by.stascala.server

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.util.ByteString
import by.stascala.api.{GameEnded, Hand, Msg}
import by.stascala.services.CommandHandler
import by.stascala.services.LobbyRoom.PlayerDisconnected

object PlayerSession {
  def props(connection: ActorRef, authentication: ActorRef, lobbyRoom: ActorRef): Props =
    Props(new PlayerSession(connection, authentication, lobbyRoom))
}


class PlayerSession(connection: ActorRef,
                    authentication: ActorRef,
                    lobbyRoom: ActorRef)
  extends Actor with Serialization with ActorLogging {

  log.info("New player session started")

  import Tcp._

  val commandHandler: ActorRef = context.actorOf(CommandHandler.props(self, authentication, lobbyRoom))


  def receive: Receive = {
    case Received(data) =>
      commandHandler ! deSerialise(data)
    case hand: Hand =>
      connection ! Write(serialise(hand))
      context.become(gameInProgress(sender()))
    case msg: Msg =>
      connection ! Write(serialise(msg))
    case PeerClosed =>
      lobbyRoom ! PlayerDisconnected(self)
      log.info(s"Peer closed. Terminating session")
      context.stop(self)
  }

  def gameInProgress(gameSession: ActorRef): Receive = {
    case Received(data) =>
      gameSession ! deSerialise(data)
    case msg: GameEnded =>
      connection ! Write(serialise(msg))
      context.become(receive)
    case msg: Msg =>
      connection ! Write(serialise(msg))
    case PeerClosed =>
      lobbyRoom ! PlayerDisconnected(self)
      log.info(s"Peer closed. Terminating session")
      context.stop(self)
  }

}