# GENERIC MULTIPLAYER CONNECTOR

[Downloads here](https://bitbucket.org/SteveSmith16384/genericmultiplayerconnector/downloads)


## Overview
The Generic Multiplayer Connector (GMC) is a library to simplify networking between clients in a game.  The library will handle all the networking between the clients and server (including sockets, threading etc..).  Once a client is connected to the server, they can broadcast data to all the other clients with a single function call.

I designed the GMC originally so I could convert simple single-player games into multiplayer challenges (see Multiplayer Tetris).  However, it can be used for any networking requirements.


## Why should I use GMC?
That's a very good question.  There are lots of other networking libraries out there.  However, the real advantage of GMC is that it requires minimal setup, configuration and handling: 

* Running the server is simple a case of running a jar.  The server is entirely generic, meaning a single server can be used for any number of different games and types of games.
* Once a client is connected, it can easily send data, and will automatically receive data, from all the other clients playing in the same game.  All you (the game developer) need to do is decide what to send and what to do when it's received.
* You don't need to worry about handling network connections, multithreading, keeping track of other players etc..  Just send and receive data.
*  There are also built-in methods for accessing the current status of the game (waiting for players, started, finished), notification when a player has joined/left, and who the winner of the game was.
* There is also a free publicly-accessible server available to use.


## Example Code

Running the server:-
```java
	// Run this in a command prompt
	java -jar GMCServer.jar
```

As you can see, no configuration is required.  The only setting that can be changed is the default port number if required.


### All the following code is run on the clients.

Connecting to the server:-
```java
	// This helper function will bring up a simple form for the user to enter an IP address etc..
	ConnectorMain connector = StartGameOptions.ShowOptionsAndConnect(this);

	// Alternatively, if you have your own method of getting the connection details:
	ConnectorMain connector = new ConnectorMain(this, "127.0.0.1", 9996, "Players Name", "MyGame", 2, 99);
	connector.connect();
```

The parameters are `<ip to connect to>`, `<port#>`, `<player's name>`, `<game code>`, `<minimum players>`, and `<maximum players>`.

Joining a game:-
```java
	connector.joinGame();
```

Sending data to all other clients:-
```java
	// There are other methods for sending different types of data, e.g. byte arrays, objects; this method sends a key/value pair by TCP.  When data is sent, it is automatically received by all the other clients.
	connector.sendKeyValueDataByTCP(code, score);
```

Receving data
```java
	// The 'game' object is your class that implements the IGameClient interface.
	game.keyValueReceivedByTCP(int fromplayerid, int code, int value) { 
	// Do stuff with the data 
}
```

The following functions are not required to be used, but the GMC can also keep track and inform the clients if a player has won the game.

Damn, I've been killed in the game.  (If there's only one player left, they will be declared the winner).
```java
	connector.sendOutOfGame();
```

I've got to the end!  Was I the first?
```java
	connector.sendIAmTheWinner();
```

Who has the server confirmed as the winner?
```java
	System.out.println("The winner was " + connector.getWinnersName() + "!");
```

For further help, please see the [Wiki](https://github.com/MajickTek/GenericMultiplayerConnector/wiki/Home).


## Games using this library
Multiplayer Tetris is included in the GMC library; a player's game speeds up when an opponent completes a row.  

I've also retrofitted a version of Tempest to be multi-player.  This can be found [here](https://github.com/SteveSmith16384/wbt-multiplayer).

Also, a multiplayer maze game called [MazEvolution](https://bitbucket.org/SteveSmith16384/mazeevolution).

Here's a tutorial on converting a standard singleplayer Tetris into multiplayer with this library: [Tutorial](https://javagmc.blogspot.com/p/blog-page.html)
### Credits

Designed and programmed by Stephen Carlyle-Smith (stephen.carlylesmith@googlemail.com)

Licenced under MIT licence.
