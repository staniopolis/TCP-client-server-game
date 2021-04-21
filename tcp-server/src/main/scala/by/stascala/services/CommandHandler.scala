package by.stascala.services

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import by.stascala.api.{Failed, JoinGame, LogIn, SingUp, Success}

object CommandHandler {
  def props(sessionActor: ActorRef, authActor: ActorRef, gameLobbyActor: ActorRef): Props =
    Props(new CommandHandler(sessionActor, authActor, gameLobbyActor))
}

class CommandHandler(playerSession: ActorRef, authentication: ActorRef, gameLobbyActor: ActorRef)
  extends Actor with ActorLogging {
  log.info("Command handle service started")
  override def receive: Receive = {
    case LogIn(playerName, password) =>
      authentication ! Authentication.LogIn(playerName, password, playerSession)
    case SingUp(playerName, password) =>
      authentication ! Authentication.SingUp(playerName, password, playerSession)
    case JoinGame(gameType, playerName) =>
      gameLobbyActor ! LobbyRoom.JoinGame(gameType, playerName, playerSession)
    case cmd: Failed =>
      playerSession ! cmd
    case cmd: Success =>
      playerSession! cmd
    case _ =>
      playerSession ! Failed("Something goes wrong")
  }
}
