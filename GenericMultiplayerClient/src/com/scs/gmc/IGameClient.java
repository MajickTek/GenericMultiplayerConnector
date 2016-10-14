/*
 *  This file is part of GenericMultiplayerConnector.

    GenericMultiplayerConnector is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GenericMultiplayerConnector is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GenericMultiplayerConnector.  If not, see <http://www.gnu.org/licenses/>.

 */

package com.scs.gmc;

public interface IGameClient {

	void playerLeft(String name);
	
	void playerJoined(String name);
	
	void gameStarted();
	
	void gameEnded(String winner);
	
	void dataReceivedByTCP(int fromplayerid, int code, int value);
	
	void dataReceivedByUDP(long time, int fromplayerid, int code, int value);
	
	void dataReceivedByTCP(int fromplayerid, String data);
	
	void dataReceivedByTCP(int fromplayerid, byte[] data);
	
	void dataReceivedByUDP(long time, int fromplayerid, String data);
	
	void dataReceivedByUDP(long time, int fromplayerid, byte[] data);
	
	void error(int error_code, String msg);
	
	void serverDown(long ms_since_response);
}
