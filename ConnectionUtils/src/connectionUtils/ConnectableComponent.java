package connectionUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public interface ConnectableComponent {

	default SocketChannel getNonBlockingSocketChannel(String connectAddress, int connectPort) {
		SocketChannel socketChannel = null;
		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.connect(new InetSocketAddress(connectAddress, connectPort));
			while (!socketChannel.finishConnect()) {}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return socketChannel;
	}
	
	default ServerSocketChannel getServerSocketChannel(int acceptPort) {
		ServerSocketChannel serverSocketChannel = null;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(new InetSocketAddress(8000));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return serverSocketChannel;
	}
}
