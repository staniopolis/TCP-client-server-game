package by.stascala.server

import java.net.InetSocketAddress
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import by.stascala.services.{Authentication, LobbyRoom, Persistent}

object TcpServer {
  def props(remote: InetSocketAddress): Props =
    Props(new TcpServer(remote))
}

class TcpServer(remote: InetSocketAddress) extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Starting tcp server...")

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, remote)

  override def receive: Receive = {
    case _ @Bound(localAddress) =>
      log.info("Server started at (localAddress: {}, port: {})", localAddress.getAddress, localAddress.getPort)
      val persistent = system.actorOf(Persistent.props(),"Persistent")
      val authentication = system.actorOf(Authentication.props(persistent), "Authentication")
      val lobbyRoom = system.actorOf(LobbyRoom.props(persistent), "LobbyRoom")
      context.become(startedServer(authentication, lobbyRoom))
    case CommandFailed(_: Bind) ⇒ context.stop(self)
  }

  def startedServer(authentication: ActorRef, gameLobby: ActorRef): Receive = {
    case _ @Connected(remote, local) =>
      log.info("Client connected - Remote(Client): {} port: {} | Local(Server): {} port: {}",
        remote.getAddress, remote.getPort, local.getAddress, local.getPort)
      log.info("Starting new session")
      val playerSession = context.actorOf(PlayerSession.props(sender(), authentication, gameLobby))
      sender() ! Register(playerSession)
  }
}