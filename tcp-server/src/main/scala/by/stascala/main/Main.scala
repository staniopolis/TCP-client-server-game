package by.stascala.main

import java.net.InetSocketAddress
import akka.actor.{ActorSystem}
import by.stascala.server.TcpServer
import com.typesafe.config.ConfigFactory

object Main {
  def main(args: Array[String]): Unit = {
    val serverConfig = ConfigFactory.load("server.conf").getConfig("server-props")

    val host = serverConfig.getString("host")
    val port = serverConfig.getInt("port")


    val serverProps = TcpServer.props(new InetSocketAddress(host, port))
    val actorSystem: ActorSystem = ActorSystem.create("TcpServer")
    actorSystem.actorOf(serverProps, "TCP_Server")
  }
}