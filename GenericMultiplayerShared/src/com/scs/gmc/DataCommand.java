package com.scs.gmc;


public enum DataCommand {

	C2S_VERSION(1), 
	S2C_OK(2), 
	S2C_ERROR(3),
	C2S_CHECK_ALIVE(4),
	S2C_PLAYER_ID(5),
	C2S_UDP_CONN(6),
	S2C_UDP_CONN_OK(7),
	S2C_RAW_DATA(8),
	C2S_RAW_DATA(9),
	S2C_I_AM_ALIVE(10),
	S2C_PING_ME(12),
	C2S_PING_RESPONSE(14),
	S2C_ALL_DATA_SENT(15),
	C2S_EXIT(16),
	S2C_MESSAGE(18),
	C2S_PLAYER_NAME(24), 
	S2C_SIDE_CONFIRMED(26),
	S2C_STAT_UPDATE(29),
	C2S_SEND_ERROR(39);
	
	
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
