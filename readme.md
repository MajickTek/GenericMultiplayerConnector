# GENERIC MULTIPLAYER CONNECTOR

Turn any single-player game into a multiplayer game quickly and easily.

Designed and programmed by Stephen Carlyle-Smith (stephen.carlylesmith@googlemail.com)

Licenced under GPLv3.


## Overview
The Generic Multiplayer Connector (GMC) is basically a way to easily connect clients to a shared server so they can pass simple data to each other.  The GMC library includes both a server and client components, and there is also (as at the time of writing this) a publicly available ready-to-use server available (see further below).

There is also a simple MultiplayerTetris included in this project which utilises this library.

## Quickstart Usage
* Run the server GameConnectorServer.jar, or decide to use the public server described below.
* implement the IGameClient interface in a class in your game.
* Create instance of ConnectorMain().
* Call ConnectorMain.connect() to connect to the server
* Call ConnectorMain.joinGame() to join a game.
* Wait until "ConnectorMain.getGameStage() == GameStage.IN_PROGRESS", and then start your main game loop.
* Send data to other clients with any of the ConnectorMain.sendKeyValue... or ConnectorMain.sendString... methods.
* Receive data using any of the IGameClient.dataReceived... interface methods.
* Call ConnectorMain.sendIAmTheWinner() if your client won the game, or call ConnectorMain,sendOutOfGame() if your client is out of the game.
* End your game when "ConnectorMain..getGameStage() == GameStage.FINISHED".
* Call ConnectorMain.waitForGameToFinish().
* Get the winner's name with ConnectorMain.GetWinnersName().
* Call ConnectorMain.joinGame() to join another game, or ConnectorMain.disconnect() to close all network connections.

See further below for more details and features.


## Usage
There is an example application called TestClient.java which shows the very simple usage.

To use the client library:-

* Implement the IGameClient interface in the most appropriate class in your game.

* Create a new instance of the ConnectorMain.class in your game.  This takes various parameters, including the player's name, server ip and port, the game code (so the server knows which clients are in the same game), the minimum number of players (so the server knows when to start the game) and and the maximum number of players.  

There is also a helpful class called StartGameOptions.java which has methods for bringing up a simple window for the player to input their details.

* In your game, call the ConnectorMain.connect() method to make the network connection to the server.  This returns a boolean of whether it was successful or not.  If you receive a false, you can call ConnectorMain.getLastError() to determine what the problem was.

* Call ConnectorMain.joinGame() to join the game.  The game will be created on the server if you are the first player of the game to connect.  This returns a boolean of whether it was successful or not.  If you receive a false, you can call ConnectorMain.getLastError() to determine what the problem was.

* Wait until ConnectorMain.getGameStage() == GameStage.IN_PROGRESS.  This is the game stage at which enough players have connected.  You can now start playing your actual game proper.

* To communicate with other clients, there are currently 4 methods:-

ConnectorMain.sendKeyValueDataByTCP(int code, int value);
ConnectorMain.sendStringDataByTCP(String data);
ConnectorMain.sendKeyValueDataByUDP(int code, int value)
ConnectorMain.sendStringDataByUDP(String data);

As you can see, there are two version of each method, one using TCP and another using UDP.  TCP is slightly slower but guaranteed to arrive, while UDP is faster but not guaranteed to arrive (or arrive in the same order it was sent).  

Clients are are informed of data being received by methods in the IGameClient interface, e.g. method "dataReceivedByTCP(int fromplayerid, int code, int value)".  You will need to design your own simple protocol for what to do with the data; for example, you could send a key/value pair where the key is the code for a players current score, and the value is the score itself.  Other clients can then display this score on-screen.  

* All clients should periodically check the value of ConnectorMain.getGameStage().  If it has changed to GameStage.FINISHED, it means the game has finished, either because one player has won, or all other players have lost or disconnected.

* If an instance of your game thinks it has won, it should call ConnectorMain.sendIAmTheWinner() and then ConnectorMain.waitForGameToFinish() (to ensure the server has confirmed the winner and broadcast the winning player to everyone).

* Once the game status has changed to "GameStatus.FINISHED", the winning player can be queried with ConnectorMain.getWinnersName().  Also, the function ConnectorMain.areWeTheWinner() will return true if this player is the winner, and victory fireworks can be displayed as appropriate.

* Once a game has finished, you can then either call ConnectorMain.joinGame() when you're ready to start/join another game, or ConnectorMain.disconnect() to close all network connections.


## Other Notes
* There are also various helper methods in IGameClient which will inform your game of certain events, such as when a player joins/leaves, when the game has started/ended, and when data has been received from other clients.

* If a player's game decides it has lost, it can call ConnectorMain.sendOutOfGame() to inform the server.  The winner will be the last player standing.

* During a game, an array of the current players can be accessed with ConnectorMain.getCurrentPlayers().

* Since UDP is not guaranteed to arrive, nor arrive in the correct order, whenever a UDP packet is received it also includes the time it was sent (taken from the computer of the player who originally sent it).  If the order of the packets is important, you can use this field to determine if a packet has been received out-of-order.


### Ideas For Multiplayer games
The real fun of implementing a multi-player aspect to your games is designing the actual inter-player game mechanics.  At the simplest level, using the key/value functions, you can send player's scores to the other players, so the key is your own internal code for score, and the value is the players score.

You could also use the sendStringData() functions to implement a simple chat system.

Further to that, it all depends on the type of game, but you could, for example, trigger more enemies on an opponents game when a player completes a level.  If you're feeling particularly adventurous, you could transmit your avatar's co-odinates to other players which could then be used to display opponents on the same screen.


## Public Server
I run a basic server at 178.62.91.22 that can be used for inter-game communication.  (See the source file Statics.java for the port).  Note that this server is not guaranteed to be available 24/7 forever though, and should not be used for anything mission-critical.


## Running the Server
If you wish to run the server, simply run com.scs.gmc.ServerMain.class or the GMCServer.jar.  Depending on the complexity of your network, you may have to configure firewalls and/or routers to enable port forwarding and allow connections.


