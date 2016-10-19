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

public class Statics {

	public static final boolean DEBUG = false; // todo - make option
	public static final boolean CHECK_SERVER_IS_ALIVE = true;
	
	public static final int CODE_VERSION = 1;
	public static final int COMMS_VERSION = 1;
	public static final int DEF_PORT = 9996;
	public static final byte CHECK_BYTE = 66;
	public static final String SERVER_PROPS = "server.props";
	public static final String TITLE = "GMC";
	
	public static final long SERVER_DIED_DURATION = 1000*60;
	public static final long CHECK_SERVER_ALIVE_INTERVAL = 5000;
	
}
