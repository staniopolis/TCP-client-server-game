package by.stascala

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.pattern.ask
import akka.util.Timeout
import by.stascala.api.{CompleteGameSession, Failed, Fold, Hand, Info, Loss, PlayerDecision, Win}
import by.stascala.services.Persistent

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Random, Success}

object GameSession {
  def props(players: Map[String, ActorRef],
            handSize: Int,
            foldCost: Long,
            playCost: Long,
            persistent: ActorRef): Props =
    Props(new GameSession(players, handSize, foldCost, playCost, persistent))
}


class GameSession(players: Map[String, ActorRef],
                  handSize: Int,
                  foldCost: Long,
                  playCost: Long,
                  persistent: ActorRef) extends Actor with ActorLogging {

  type Card = (Int, Char)
  type Hand = List[Card]


  log.info(s"${
    handSize match {
      case 1 => "Single-card"
      case 2 => "Double-card"
      case _ => "Some"
    }
  } game started")

  players.foreach(session => context.watch(session._2))

  import context.dispatcher


  implicit val timeout: Timeout = 1.second

  // Build card deck
  private def buildCardDeck: List[Card] = {
    for {
      suit <- List('h', 'd', 'c', 's')
      card <- 2 to 14
    } yield (card, suit)
  }

  var cardDeck: List[Card] = buildCardDeck


  // Round counter
  var roundCount = 1

  /*
  This function deal cards to players.
  Include nested function that choose random card from the card deck.
  In case when card deck runs out of cards it build another card deck and continue dealing.
    */
  @tailrec
  private def handToDeal(handSize: Int, hand: Hand): Hand = {
    def dealCard: Card = {
      if (cardDeck.isEmpty) {
        cardDeck = buildCardDeck
        val randomCard = cardDeck(Random.nextInt(cardDeck.length))
        cardDeck = cardDeck.filter(_ != randomCard)
        randomCard
      }
      else {
        val randomCard = cardDeck(Random.nextInt(cardDeck.length))
        cardDeck = cardDeck.filter(_ != randomCard)
        randomCard
      }
    }

    if (handSize == 0) hand
    else handToDeal(handSize - 1, hand :+ dealCard)
  }

  // Players hands variable
  var playersHand: Map[String, Hand] =
    players.map(player => (player._1, handToDeal(handSize, List())))

  // Send hands data to players and wait for players decisions
  playersHand.foreach(hand => players(hand._1) ! Hand(roundCount, hand._2))

  // Variable to accumulate players decisions.
  var playersDecisions: Map[String, Int] = Map.empty


  // Players incoming decision messages handler
  override def receive: Receive = {
    case PlayerDecision(playerDecision) =>
      // In case this decision message is the last, we start handling them.
      if (playersDecisions.size == players.size - 1) {
        playersDecisions += playerDecision
        handleDecisions
      }
      // Otherwise we add it to the playerDecisions variable
      else {playersDecisions += playerDecision
      players(playerDecision._1) ! Info("Waiting for other player(s) decision...")}

    case Terminated(player) => persistent ! Persistent.ResultBalance(players
      .filter(_._2 == player).head._1, playCost, -playCost)
  }

  // Handle players decisions
  private def handleDecisions: Unit = {
    // At first we handle all "folds", if exist
    val folPlayers = playersDecisions.filter(_._2 == 1)
    folPlayers.filter(_._2 == 1).foreach(player =>
      (persistent ? Persistent.ResultBalance(player._1, playCost, -foldCost))
        .mapTo[Persistent.BalanceUpdated] onComplete {
        case Success(result) =>
          players(player._1) ! Fold(result.updatedBalance._2, -foldCost)
        case Failure(exception) => Failed(exception.getMessage)
      })
    folPlayers.keySet.foreach(key => playersHand -= key)
    // Check if there any player left and terminate GameSession actor if not
    if (playersDecisions.exists(_._2 == 2)) handlePlayDecisions
    else {
      players.foreach(_._2 ! CompleteGameSession("", (0, '_')))
      context.stop(self)
    }
  }

  // Function to handle players hands who choose "play" option
  private def handlePlayDecisions: Unit = {

    /*
    At first me must check if players hands contains cards
    and deal 1 card to each left player if not.
    Then we filter players who do not contain the strongest card and handle them.
    After that we try to define and handle winner.
     */
    playersHand.head._2 match {
      case Nil =>
        playersHand.foreach(player=> players(player._1)! Info("Your opponent(s) has the same hand. Deal Addition card"))
        roundCount += 1
        playersHand = playersHand.map(player => (player._1, handToDeal(1, List())))
        playersHand.foreach(playerHand => players(playerHand._1) ! Hand(roundCount, playerHand._2))
        handlePlayDecisions
      case _ =>
        val strongestCard = playersHand.map(_._2.map(_._1).max).max
        val losers = playersHand.filter(player => !player._2.map(_._1).contains(strongestCard))
        losers.keySet.foreach(key => playersHand -= key)
        handleLosers(losers.keySet, strongestCard)

    }

    // Nested function to handle losers
    def handleLosers(losers: Set[String], strongestCard: Int): Unit = {
      losers.foreach(player =>
        (persistent ? Persistent.ResultBalance(player, playCost, -playCost))
          .mapTo[Persistent.BalanceUpdated] onComplete {
          case Success(result) =>
            players(player) ! Loss(strongestCard, result.updatedBalance._2, -playCost)
          case Failure(exception) => Failed(exception.getMessage)
        })
      defineWinner
    }

    /*
    Nested function to define and handle winner.
    In case we have more then one player with strongest card,
    we drop 1 strongest card from each player hand and start handling players hands again.
    Otherwise, we handle winner and terminate GameSession actor
     */
    def defineWinner: Unit = {
      if (playersHand.size == 1)
        (persistent ? Persistent.ResultBalance(playersHand.head._1, playCost, playCost))
          .mapTo[Persistent.BalanceUpdated] onComplete {
          case Success(result) =>
            val winner = playersHand.head._1
            val strongestCard = playersHand.head._2.filter(card => card._1 == playersHand.head._2.map(_._1).max).head
            players(winner) ! Win(strongestCard, result.updatedBalance._2, playCost)
            context.stop(self)
            players.foreach(_._2 ! CompleteGameSession(winner, strongestCard))
            log.info(s"${
              handSize match {
                case 1 => "Single-card"
                case 2 => "Double-card"
                case _ => "Some"
              }
            } game session ended. The winner is $winner")
          case Failure(exception) => Failed(exception.getMessage)
        } else {
        playersHand.foreach(player =>
          players(player._1) ! Info(s"Your opponent(s) has the same value of the strongest card. Drop strongest card"))
        playersHand.map(hand => (hand._1,hand._2.sortBy(_._1).init))
          .foreach(newHand => playersHand = playersHand.updated(newHand._1, newHand._2))
        handlePlayDecisions
      }
    }
  }
}
