package ssmith.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public abstract class TCPConnectionWrapper {

	public Socket sck;
	private DataOutputStream dos;
	private DataInputStream dis;
	public long started_time;

	
	public TCPConnectionWrapper(Socket sck) throws IOException {
		super();
		
		//this.setDaemon(false); //No, as we don't want to stop it abruptly
		this.sck = sck;
		
		dos = new DataOutputStream(sck.getOutputStream());
		dis = new DataInputStream(sck.getInputStream());
		this.started_time = System.currentTimeMillis();
		
		sck.setTcpNoDelay(true);
	}

	
	public void close() {
		try {
			dis.close();
			dos.close();
			sck.close();
		} catch (IOException e) {
			// Nothing
		}
	}

	
	public DataOutputStream getDataOutputStream() {
		return dos;
	}

	
	public DataInputStream getDataInputStream() {
		return dis;
	}


	public InetAddress getINetAddress() {
		return sck.getInetAddress();
	}

}
