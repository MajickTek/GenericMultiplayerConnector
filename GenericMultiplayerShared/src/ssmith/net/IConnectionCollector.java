package ssmith.net;

import java.io.IOException;
import java.net.Socket;

public interface IConnectionCollector {

	void newConnection(Socket sck) throws IOException;
}
