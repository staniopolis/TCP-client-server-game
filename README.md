## Important

1. The project is still under construction (I would like to add test suites), but already applicable.
2. This is my first time I develop both sides (client and server) of an application. In all Scala courses I have passed, I havn't encountered such assigment.
3. Persistent implemented in form of actor state. It means, that in case you restart server, all persisted data will be lost.


## Installation info

To compile and run project you must have sbt 1.4.4 installed

## Games configuration info
To change games configuration, switch to tcp-server\src\main\resources directory and open resources.conf file.
For each game type you can change:
- ```numberOfPlayers``` <- the number of players required to start a game session
- ```handSize``` <- the number of cards server deal at game start
- ```foldCost``` <- the number of tokens that will be withdraw in case you fold your hand
- ```playCost``` <- the number of tokens that will be withdraw or credited in case you lose or win the game session


## Run

1. Run sbt server from tcp-server project root directory and then ```run``` project in sbt shell. Or type ```sbt run``` from tcp-server project root directort in terminal
2. For each player, run sbt client from tcp-client project root directory and then ```run``` project in sbt shell. Or type ```sbt run``` from tcp-client project root directort in terminal.

## Play

Follow the prompts in the terminal

