package com.scs.gmc.shared;

/**
 * Commands for communication.  S2C = Server to client, C2S = Client to server.
 *
 */
public enum DataCommand {

	C2S_VERSION(1), 
	S2C_VERSION_OK(2), 
	S2C_ERROR(3),
	C2S_CHECK_ALIVE(4),
	S2C_PLAYER_ID(5),
	C2S_UDP_CONN(6), // To set up the UDP conn
	S2C_UDP_CONN_OK(7),
	S2C_TCP_KEYVALUE_DATA(8),
	C2S_TCP_KEYVALUE_DATA(9),
	S2C_UDP_KEYVALUE_DATA(10),
	C2S_UDP_KEYVALUE_DATA(11),
	S2C_I_AM_ALIVE(12),
	S2C_CURRENT_PLAYERS(13),
	S2C_PING_ME(14),
	C2S_PING_RESPONSE(15),
	C2S_OUT_OF_GAME(16),
	C2S_WINNER(17),
	C2S_DISCONNECTING(18),
	S2C_TCP_STRING_DATA(19),
	C2S_TCP_STRING_DATA(20),
	S2C_UDP_STRING_DATA(21),
	C2S_UDP_STRING_DATA(22),
	C2S_JOIN_GAME(24), 
	//C2S_SEND_ERROR(39),
	S2C_NEW_PLAYER(40),
	S2C_GAME_STARTED(41),
	S2C_PLAYER_LEFT(42),
	S2C_GAME_OVER(43),
	S2C_TCP_BYTEARRAY_DATA(44),
	C2S_TCP_BYTEARRAY_DATA(45),
	S2C_UDP_BYTEARRAY_DATA(46),
	C2S_UDP_BYTEARRAY_DATA(47),
	S2C_TCP_OBJECT_DATA(48),
	C2S_TCP_OBJECT_DATA(49),
	S2C_UDP_OBJECT_DATA(50),
	C2S_UDP_OBJECT_DATA(51);
	
	
	private byte theID;

	DataCommand( int pID )
	{
		theID = (byte)pID;
	}

	public byte getID()
	{
		return this.theID;
	}

	public static DataCommand get( final int pID )
	{
		DataCommand lTypes[] = DataCommand.values();
		for( DataCommand lType : lTypes )
		{
			if( lType.getID() == pID )
			{
				return lType;
			}
		}
		throw new IllegalArgumentException("Unknown type: " + pID);
	}
	
}
