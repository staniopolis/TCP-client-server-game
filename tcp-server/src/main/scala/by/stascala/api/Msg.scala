package by.stascala.api

sealed trait Msg
sealed trait SrvMsg extends Msg
sealed trait ClnMsg extends Msg
sealed trait GameEnded extends SrvMsg

// ---------server-client messages-----------------------------
case class LogIn(playerName: String, password: String) extends ClnMsg
case class SignUp(playerName: String, password: String) extends ClnMsg
case class JoinGame(gameType: String, player: String) extends ClnMsg
case class PlayerDecision(playerDecision: (String, Int)) extends ClnMsg

case class AuthSuccess(authMsg: String) extends SrvMsg
case class AuthFailed(msg: String) extends SrvMsg
case class CurrentBalance(balance: Long) extends SrvMsg
case class DealtHand(round: Int, hand: List[(String, String)]) extends SrvMsg
case class Hand(round: Int, card: List[(Int, Char)]) extends SrvMsg
case class Info(msg: String) extends SrvMsg
case class Failed(msg: String) extends SrvMsg
case class Success(msg: String) extends SrvMsg
case class Win(strongestCard: (Int,Char), balance: Long, delta: Long) extends SrvMsg
case class Loss(strongestCard: Int, balance: Long, delta: Long) extends SrvMsg
case class Fold(balance: Long, delta: Long) extends SrvMsg
case class CompleteGameSession(playerName: String, strongestCard: (Int, Char))extends GameEnded
