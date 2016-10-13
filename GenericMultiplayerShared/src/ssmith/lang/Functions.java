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

package ssmith.lang;


public final class Functions {


	public static void delay(int milliseconds) {
		if (milliseconds > 0) {
			try {
				Thread.sleep(milliseconds);
			}
			catch (InterruptedException e) {
			}
		}
	}


	public static void delay(long milliseconds) {
		if (milliseconds > 0) {
			try {
				Thread.sleep(milliseconds);
			}
			catch (InterruptedException e) {
			}
		}
	}


	public static String Throwable2String( Throwable pThrowable )
	{
		StringBuilder lStackTrace = new StringBuilder();
		while( pThrowable != null )
		{
			lStackTrace.append( pThrowable + "\n" );
			for( int i = 0; i < pThrowable.getStackTrace().length; i++ )
			{
				lStackTrace.append( " " ).append( pThrowable.getStackTrace()[i].getClassName() );
				lStackTrace.append( ":" ).append( pThrowable.getStackTrace()[i].getLineNumber() ).append( " - " );
				lStackTrace.append( pThrowable.getStackTrace()[i].getMethodName() );
				lStackTrace.append( "\n" );
			}
			pThrowable = pThrowable.getCause();
			if( pThrowable != null )
			{
				lStackTrace.append( "Caused by:\n" );
			}
		}
		return lStackTrace.toString();
	}

}


