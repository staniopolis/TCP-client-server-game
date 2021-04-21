package by.stascala.client

import java.net.InetSocketAddress
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import by.stascala.api._
import by.stascala.ClientHandler.{ConnectionFailed, Exit, SessionClosed, Start}

object TcpClient {
  def props(remote: InetSocketAddress, listener: ActorRef) =
    Props(new TcpClient(remote, listener))

}

class TcpClient(remote: InetSocketAddress,
                listener: ActorRef) extends Actor with Serialization with ActorLogging {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive: Receive = {
    case _: CommandFailed =>
      val msg = s"Connection failed"
      listener ! ConnectionFailed(msg)
      context.stop(self)

    case _@Connected(remote, _) =>
      val connection = sender()
      connection ! Register(self)
      listener ! Start
      context.become(connected(connection))
  }

  private def connected(connection: ActorRef): Receive = {
    case Received(data) =>
      listener ! deSerialise(data)
    case data: Msg =>
      connection ! Write(ByteString(serialise(data)))
    case failed: CommandFailed =>
      listener ! Failed(failed.toString())
    case Exit =>
      connection ! Close
    case closed: ConnectionClosed =>
      listener ! SessionClosed(if (closed.isErrorClosed) closed.getErrorCause else "no error")
      context.stop(self)
  }
}