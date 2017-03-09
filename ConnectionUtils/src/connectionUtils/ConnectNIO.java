package connectionUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author Joachim Utility class for all network communication functionality.
 *
 */
public class ConnectNIO {

	/**
	 * Creates a non-blocking {@link SocketChannel} using the given IP address
	 * and connection port. Contains default timeout value of 5 seconds to avoid
	 * an accidental infinite loop.
	 * 
	 * @param connectAddress
	 *            the remote address to connect to
	 * @return a non-blocking {@link SocketChannel} that is connected to the
	 *         remote address, or null if a connection error occurs.
	 */
	public static SocketChannel getNonBlockingSocketChannel(InetSocketAddress connectAddress) {
		SocketChannel socketChannel = null;
		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.connect(connectAddress);
			long connectAttemptStart = System.currentTimeMillis();
			long timeoutEpoch = connectAttemptStart + 5000;
			while (!socketChannel.finishConnect()) {
				if (System.currentTimeMillis() > timeoutEpoch) {
					break;
				}
			}
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return socketChannel;
	}
	
	/**
	 * Creates a non-blocking {@link SocketChannel} using the given IP address
	 * and connection port. Overloaded method with timeout value.
	 * 
	 * @param connectAddress
	 *            the remote address to connect to
	 * @param timeoutMillis
	 *            the duration, in milliseconds, to timeout after attempting to connect
	 * @return a non-blocking {@link SocketChannel} that is connected to the
	 *         remote address, or null if a connection error occurs.
	 */
	public static SocketChannel getNonBlockingSocketChannel(InetSocketAddress connectAddress, int timeoutMillis) {
		SocketChannel socketChannel = null;
		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.connect(connectAddress);
			long connectAttemptStart = System.currentTimeMillis();
			long timeoutEpoch = connectAttemptStart + timeoutMillis;
			while (!socketChannel.finishConnect()) {
				if (System.currentTimeMillis() > timeoutEpoch) {
					break;
				}
			}
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return socketChannel;
	}

	/**
	 * Creates a blocking {@link SocketChannel} using the given IP address and
	 * connection port.
	 * 
	 * @param connectAddress
	 *            the remote address to connect to
	 * @param connectPort
	 *            the remote port to connect to
	 * @return a blocking {@link SocketChannel} that is connected to the remote
	 *         address, or null if a connection error occurs.
	 */
	public static SocketChannel getBlockingSocketChannel(InetSocketAddress connectAddress) {
		SocketChannel socketChannel = null;
		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(true);
			socketChannel.connect(connectAddress);
		} catch (IOException e) {
			// e.printStackTrace();
		}
		if (socketChannel.isConnected()) {
			return socketChannel;
		} else {
			try {
				socketChannel.close();
			} catch (IOException e) {
				// e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * Creates a new, open {@link ServerSocketChannel} that will be bound to the
	 * given <code>acceptPort</code>.
	 * 
	 * @param acceptPort
	 *            the port to listen for incoming connection requests on
	 * @return an open {@link ServerSocketChannel} that is bound to the
	 *         specified port, or null if there is a problem binding to the port
	 *         (e.g. port already bound).
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

	/**
	 * Convenience method for checking if a port is available by using a
	 * {@link Socket}. Method taken from: http://stackoverflow.com/a/13826145
	 * 
	 * @param port
	 *            the port to check availability on
	 * @return true if the port is available, otherwise false.
	 */
	public static boolean portIsAvailable(int port) {
		System.out.println("--------------Testing port " + port);
		Socket s = null;
		try {
			s = new Socket("localhost", port);

			// If the code makes it this far without an exception it means
			// something is using the port and has responded.
			System.out.println("--------------Port " + port + " is not available");
			return false;
		} catch (IOException e) {
			System.out.println("--------------Port " + port + " is available");
			return true;
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {
					throw new RuntimeException("You should handle this error.", e);
				}
			}
		}
	}
}
