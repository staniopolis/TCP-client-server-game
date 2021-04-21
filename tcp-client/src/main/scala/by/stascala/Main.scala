package by.stascala

import akka.actor.{ActorRef, ActorSystem}
import by.stascala.client.TcpClient

import java.net.InetSocketAddress

object Main {
  def main(args: Array[String]): Unit = {
    val host = "localhost"
    val port = 9900



    val clientActorSystem: ActorSystem = ActorSystem.create("ClientActorSystem")
    val clientHandler: ActorRef = clientActorSystem.actorOf(ClientHandler.props(), "clientHandler")
    val tcpConnectionProps = TcpClient.props(new InetSocketAddress(host, port), clientHandler)

    clientActorSystem.actorOf(tcpConnectionProps, "tcpService")

  }
}
