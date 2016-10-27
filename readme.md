# GENERIC MULTIPLAYER CONNECTOR

## Overview
The Generic Multiplayer Connector (GMC) is a library to help turn any single-player game into a realtime multi-player game by allowing clients to easily connect to a shared server and pass data to each other without having to worry about all the usual networking complications.  It uses a client/server model, and when a client sends out any data, it is automatically received by all other clients in the same game.  Clients also receive notifications of when a game has been won and who the winner was.

## Why should I use GMC?
That's a very good question.  There are lots of other networking libraries out there.  However, the real advantage of GMC is that it requires minimal setup, configuration and handling: 

* Running the server is simple a case of running a jar.  Because the server is generic, a single server can be used for any number of different games.
* Once a client is connected, it can send data, and will automatically receive data, from all the other clients playing in the same game.  All you need to do is decide when to send data, what data to send, and what to do with the data when it's received.
* You don't need to worry about handling network connections, multithreading, keeping track of other players etc..  Just send and receive data.
*  There are built-in methods for accessing the current status of the game (waiting for players, started, finished), notification when a player has joined/left, and who the winner of the game was.
* There is a free publicly-accessible server waiting for you to use.


## Example Code

Running the server:-
```java
	// Run this in a command prompt
	java -jar GMCServer.jar
```

### All the following code is run on the clients.

Connecting to the server:-
```java
	// This will bring up a form for the user to enter an IP address etc..
	ConnectorMain connector = StartGameOptions.ShowOptionsAndConnect(this);

	// Alternatively, if you have your own method of getting the connection details:
	ConnectorMain connector = new ConnectorMain(this, "127.0.0.1", 9996, "Steve", "MultiplayerTetris", 2, 99);
	connector.connect();
```

Joining a game:-
```java
	connector.joinGame();
```

Sending data to all other clients:-
```java
	// There are other ways, this sends a key/value pair by TCP.
	connector.sendKeyValueDataByTCP(code, score);
```

Receving data
```java
	// 'game' is your class that implements the IGameClient interface.
	game.dataReceivedByTCP(int fromplayerid, int code, int value);
```

The following functions are not required to be used, but if the winner of your game is either the last player standing or the firstv player to reach a certain point, the server will keep track of this for you if you use these functions.

Damn, I've been killed in the game.  (If there's only one player left, they will be declared the winner).
```java
	connector.sendOutOfGame();
```

I've got to the end!  Was I the first?
```java
	connector.sendIAmTheWinner();
```

But who has the server confirmed as the winner?
```java
	System.out.println("The winner was " + connector.getWinnersName() + "!");
```


## Quickstart Guide
These are step-by-step instructions on how to incorporate it into your project:-

* Run the server GMCServer.jar, or decide to use the public server described below.
* Add the GMCClient.jar to your project.
* implement the IGameClient interface in a class in your game.
* Create instance of ConnectorMain().
* Call ConnectorMain.connect() to connect to the server
* Call ConnectorMain.joinGame() to join a game.
* Wait until all players have connected ("ConnectorMain.getGameStage() == GameStage.IN_PROGRESS") and then start your main game loop.
* Send data to other clients with any of the ConnectorMain.sendKeyValue..., ConnectorMain.sendString... or ConnectorMain.sendByteArray... methods.  All the other clients will receive any data sent.
* Receive data using any of the IGameClient.dataReceived... interface methods.
* Call ConnectorMain.sendIAmTheWinner() if your client won the game, or call ConnectorMain,sendOutOfGame() if your client is out of the game.
* End your game when "ConnectorMain.getGameStage() == GameStage.FINISHED", or if your client has finished (i.e. the player has died), call ConnectorMain.waitForGameToFinish().
* Once the game has finished, you can get the winner's name with ConnectorMain.GetWinnersName().
* You can then call ConnectorMain.joinGame() to join/start another game, or ConnectorMain.disconnect() to close all network connections.


## More Detailed Guide
An example application called [TestClient.java](https://bitbucket.org/SteveSmith16384/genericmultiplayerconnector/src/master/GenericMultiplayerClient/src/com/scs/gmc/exampleapp/TestClient.java?at=master&fileviewer=file-view-default) is in the source which shows the very simplist usage.  There is also a [Multiplayer Tetris](https://bitbucket.org/SteveSmith16384/genericmultiplayerconnector/src/master/GenericMultiplayerClient/src/com/scs/gmc/exampleapp/MultiplayerTetris.java?at=master&fileviewer=file-view-default) game included which utilises this library in a real game.

To use the client library:-

* Add the GMCClient.jar to your project.

* Implement the IGameClient interface in the most appropriate class in your game.

* Create a new instance of the ConnectorMain.class in your game.  This takes various parameters, including the player's name, server ip and port, the game code (so the server knows which clients are in the same game), the minimum number of players (so the server knows when to start the game) and and the maximum number of players.  

There is also a helpful class called StartGameOptions.java which has methods for bringing up a simple window for the player to input their details.

* In your game, call the ConnectorMain.connect() method to make the network connection to the server.  This returns a boolean of whether it was successful or not.  If you receive a false, you can call ConnectorMain.getLastError() to determine what the problem was.

* Call ConnectorMain.joinGame() to join the game.  The game will be created on the server if you are the first player of the game to connect.  This returns a boolean of whether it was successful or not.  If you receive a false, you can call ConnectorMain.getLastError() to determine what the problem was.

* Wait until ConnectorMain.getGameStage() == GameStage.IN_PROGRESS.  This is the game stage at which enough players have connected.  You can now start playing your actual game proper.

* To communicate with other clients, there are currently 6 methods:-

1. ConnectorMain.sendKeyValueDataByTCP(int code, int value);
2. ConnectorMain.sendStringDataByTCP(String data);
3. ConnectorMain.sendByteArrayByTCP(byte[] data);
4. ConnectorMain.sendKeyValueDataByUDP(int code, int value);
5. ConnectorMain.sendStringDataByUDP(String data);
6. ConnectorMain.sendByteArrayByUDP(byte[] data);

As you can see, there are two version of each method, one using TCP and another using UDP.  TCP is slightly slower but guaranteed to arrive, while UDP is faster but not guaranteed to arrive (or arrive in the same order it was sent).  

Clients are are informed of data being received by methods in the IGameClient interface, e.g. method "dataReceivedByTCP(int fromplayerid, int code, int value)".  You will need to design your own simple protocol for what to do with the data; for example, you could send a key/value pair where the key is the code for a players current score, and the value is the score itself.  Other clients can then display this score on-screen.  

* All clients should periodically check the value of ConnectorMain.getGameStage().  If it has changed to GameStage.FINISHED, it means the game has finished, either because one player has won, or all other players have lost or disconnected.

* If an instance of your game thinks it has won, it should call ConnectorMain.sendIAmTheWinner() and then ConnectorMain.waitForGameToFinish() (to ensure the server has confirmed the winner and broadcast the winning player to everyone).

* Once the game status has changed to "GameStatus.FINISHED", the winning player can be queried with ConnectorMain.getWinnersName().  Also, the function ConnectorMain.areWeTheWinner() will return true if this player is the winner, and victory fireworks can be displayed as appropriate.

* Once a game has finished, you can then either call ConnectorMain.joinGame() when you're ready to start/join another game, or ConnectorMain.disconnect() to close all network connections.


### Other Notes
* There are also various helper methods in IGameClient which will inform your game of certain events, such as when a player joins/leaves, when the game has started/ended, and when data has been received from other clients.

* If a player's game decides it has lost, it can call ConnectorMain.sendOutOfGame() to inform the server.  The winner will be the last player standing.

* During a game, a map of the current players by id can be accessed with ConnectorMain.getCurrentPlayers().

* Since UDP is not guaranteed to arrive, nor arrive in the correct order, whenever a UDP packet is received it also includes the time it was sent (taken from the computer of the player who originally sent it).  If the order of the packets is important, you can use this field to determine if a packet has been received out-of-order.


## Public Server
I run a basic server at 178.62.91.22 that can be used for inter-game communication.  (See the source file Statics.java for the port).  Note that this server is not guaranteed to be available 24/7 forever though, and should not be used for anything mission-critical.


## Games using this library
Multiplayer Tetris is included in the library, where the game speeds up when an opponent completes a row.  

I've also retrofitted a version of Tempest to be multi-player.  This can be found [here](https://github.com/SteveSmith16384/wbt-multiplayer).

Also, a multiplayer maze game called [MazEvolution](https://bitbucket.org/SteveSmith16384/mazeevolution).


### Credits

Designed and programmed by Stephen Carlyle-Smith (stephen.carlylesmith@googlemail.com)

Licenced under GPLv3.
