package connectionUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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
