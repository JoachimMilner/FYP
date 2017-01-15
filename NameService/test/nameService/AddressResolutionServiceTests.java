package nameService;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.junit.Test;

import connectionUtils.MessageType;

public class AddressResolutionServiceTests {

	/**
	 * Test successful creation of an {@link AddressResolutionService} instance.
	 * Should not be null
	 */
	@Test
	public void testCreateAddressResolutionService() {
		AddressResolutionService addressResolutionService = new AddressResolutionService(8000);
		assertNotNull(addressResolutionService);
	}

	/**
	 * Test that the {@link AddressResolutionService}'s
	 * <code>getHostAddress</code> method functions correctly. Should be null as
	 * we have not set it from a remote server.
	 */
	@Test
	public void testAddressResolutionService_getHostAddress() {
		AddressResolutionService addressResolutionService = new AddressResolutionService(8000);
		assertNull(addressResolutionService.getHostAddress());
	}

	/**
	 * Test that the {@link AddressResolutionService}'s
	 * <code>setHostAddress</code> method functions correctly.
	 */
	@Test
	public void testAddressResolutionService_setHostAddress() {
		AddressResolutionService addressResolutionService = new AddressResolutionService(8000);
		addressResolutionService.setHostAddress("192.163.1.1");
		assertEquals("192.163.1.1", addressResolutionService.getHostAddress());
	}

	/**
	 * Test that the {@link AddressResolutionService} correctly updates the host
	 * address when sent a notify message from a server
	 */
	@Test
	public void testAddressResolutionService_notifyHostAddress() throws IOException {
		AddressResolutionService addressResolutionService = new AddressResolutionService(8000);
		addressResolutionService.setHostAddress("localhost");
		Thread serviceThread = new Thread(new Runnable() {

			@Override
			public void run() {
				addressResolutionService.startService();
			}

		});
		serviceThread.start();
		SocketChannel mockServer = SocketChannel.open();
		mockServer.connect(new InetSocketAddress("localhost", 8000));

		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.clear();
		buffer.put((byte) MessageType.HOST_ADDR_NOTIFY.getValue());
		buffer.flip();
		while (buffer.hasRemaining()) {
			mockServer.write(buffer);
		}

		assertEquals("localhost", addressResolutionService.getHostAddress());
		serviceThread.interrupt();
		mockServer.close();
	}

	/**
	 * Test that a mocked remote process can successfully request the host's
	 * address from the {@link AddressResolutionService}.
	 */
	@Test
	public void testAddressResolutionService_requestHostAddress() throws IOException {
		AddressResolutionService addressResolutionService = new AddressResolutionService(8000);
		addressResolutionService.setHostAddress("localhost");
		Thread serviceThread = new Thread(new Runnable() {

			@Override
			public void run() {
				addressResolutionService.startService();
			}

		});
		serviceThread.start();
		
		SocketChannel mockClient = SocketChannel.open();
		mockClient.connect(new InetSocketAddress("localhost", 8000));

		ByteBuffer buffer = ByteBuffer.allocate(16);
		buffer.clear();
		buffer.put((byte) MessageType.HOST_ADDR_REQUEST.getValue());
		buffer.flip();
		while (buffer.hasRemaining()) {
			mockClient.write(buffer);
		}

		Selector selector = Selector.open();
		mockClient.configureBlocking(false);
		mockClient.register(selector, SelectionKey.OP_READ);
		if (selector.select(1000) == 0) {
			throw new SocketTimeoutException();
		}
		buffer.clear();
		int bytesRead = mockClient.read(buffer);
		//byte[] byteArray = new byte[bytesRead];
		assertTrue(bytesRead != -1);
		buffer.flip();
		CharBuffer charBuffer = Charset.forName("UTF-8").decode(buffer);
		
		//buffer.get(byteArray);
		String hostAddress = charBuffer.toString();
		assertEquals("localhost", hostAddress);
		serviceThread.interrupt();
		selector.close();
		mockClient.close();
	}
}
