package connectionUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author Joachim</br> Utility class for all network communication functionality.
 *
 */
public class ConnectNIO {

	/**
	 * Creates a non-blocking {@link SocketChannel} using the given IP address and connection port.
	 * @param connectAddress the remote address to connect to
	 * @param connectPort the remote port to connect to
	 * @return a non-blocking {@link SocketChannel} that is connected to the remote address, or null if
	 * a connection error occurs.
	 */
	public static SocketChannel getNonBlockingSocketChannel(InetSocketAddress connectAddress) {
		SocketChannel socketChannel = null;
		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.connect(connectAddress);
			while (!socketChannel.finishConnect()) {}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return socketChannel;
	}
	
	/**
	 * Creates a blocking {@link SocketChannel} using the given IP address and connection port.
	 * @param connectAddress the remote address to connect to
	 * @param connectPort the remote port to connect to
	 * @return a blocking {@link SocketChannel} that is connected to the remote address, or null if
	 * a connection error occurs.
	 */
	public static SocketChannel getBlockingSocketChannel(InetSocketAddress connectAddress) {
		SocketChannel socketChannel = null;
		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(true);
			socketChannel.connect(connectAddress);
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return socketChannel.isConnected() ? socketChannel : null;
	}
	
	/**
	 * Creates a new, open {@link ServerSocketChannel} that will be bound to the given <code>acceptPort</code>.
	 * @param acceptPort the port to listen for incoming connection requests on
	 * @return an open {@link ServerSocketChannel} that is bound to the specified port, or null
	 * if there is a problem binding to the port (e.g. port already bound). 
	 */
	public static ServerSocketChannel getServerSocketChannel(int acceptPort) {
		ServerSocketChannel serverSocketChannel = null;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(new InetSocketAddress(acceptPort));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return serverSocketChannel;
	}
}
