package commsModel;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.Test;

import commsModel.AbstractRemote;
import commsModel.LoadBalancerState;
import commsModel.RemoteLoadBalancer;
import commsModel.Server;

public class RemoteLoadBalancerTests {

	/**
	 * Test successful creation of a new {@link RemoteLoadBalancer} object.
	 */
	@Test
	public void testCreateRemoteLoadBalancer_successful() {
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000));
		assertNotNull(remoteLoadBalancer);
	}

	/**
	 * Test creation of a new {@link RemoteLoadBalancer} object with a null
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateRemoteLoadBalancer_nullAddress() {
		new RemoteLoadBalancer(null);
	}

	/**
	 * Test the {@link AbstractRemote}'s <code>getAddress</code> method.
	 * Although this method is already covered in the {@link Server} tests, we
	 * need to make sure that this implementation of the abstract class
	 * successfully sets the address property.
	 */
	@Test
	public void testRemoteLoadBalancer_getAddress() {
		InetSocketAddress expectedAddress = new InetSocketAddress("localhost", 8000);
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(expectedAddress);
		InetSocketAddress returnedAddress = remoteLoadBalancer.getAddress();
		assertEquals(expectedAddress, returnedAddress);
	}

	/**
	 * Test the {@link RemoteLoadBalancer}'s <code>getState</code> method. The
	 * method should return the enum value that we set using reflection.
	 */
	@Test
	public void testRemoteLoadBalancer_getState()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000));
		LoadBalancerState expectedState = LoadBalancerState.ACTIVE;

		Field stateField = remoteLoadBalancer.getClass().getDeclaredField("state");
		stateField.setAccessible(true);
		stateField.set(remoteLoadBalancer, expectedState);

		LoadBalancerState state = remoteLoadBalancer.getState();
		assertEquals(expectedState, state);
	}

	/**
	 * Test the {@link RemoteLoadBalancer}'s <code>setState</code> method. The
	 * method should set the <code>state</code> property of the object to the
	 * value passed in.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@Test
	public void testRemoteLoadBalancer_setState()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000));
		LoadBalancerState expectedState = LoadBalancerState.ACTIVE;
		remoteLoadBalancer.setState(expectedState);

		Field stateField = remoteLoadBalancer.getClass().getDeclaredField("state");
		stateField.setAccessible(true);

		assertEquals(expectedState, stateField.get(remoteLoadBalancer));
	}

	/**
	 * Test the {@link RemoteLoadBalancer}'s <code>isElectedBackup</code>
	 * method. The method should return the boolean value that we set using
	 * reflection.
	 */
	@Test
	public void testRemoteLoadBalancer_isElectedBackup()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000));
		boolean expectedIsElectedBackup = true;

		Field isElectedBackupField = remoteLoadBalancer.getClass().getDeclaredField("isElectedBackup");
		isElectedBackupField.setAccessible(true);
		isElectedBackupField.set(remoteLoadBalancer, expectedIsElectedBackup);

		boolean isElectedBackup = remoteLoadBalancer.isElectedBackup();
		assertEquals(expectedIsElectedBackup, isElectedBackup);
	}

	/**
	 * Test the {@link RemoteLoadBalancer}'s <code>setElectionOrdinality</code>
	 * method. The method should set the <code>electionOrdinality</code>
	 * property of the object to the value passed in.
	 */
	@Test
	public void testRemoteLoadBalancer_setIsElectedBackup()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000));
		boolean expectedIsElectedBackup = true;
		remoteLoadBalancer.setIsElectedBackup(expectedIsElectedBackup);

		Field isElectedBackupField = remoteLoadBalancer.getClass().getDeclaredField("isElectedBackup");
		isElectedBackupField.setAccessible(true);

		assertEquals(expectedIsElectedBackup, isElectedBackupField.get(remoteLoadBalancer));
	}
	
	/**
	 * Test the {@link RemoteLoadBalancer}'s <code>getSocketChannel</code>
	 * method. The method should return the object that we set using
	 * reflection.
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IOException 
	 */
	@Test
	public void testRemoteLoadBalancer_getSocketChannel() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, IOException {
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000));
		SocketChannel expectedSocketChannel = SocketChannel.open();

		Field socketChannelField = remoteLoadBalancer.getClass().getSuperclass().getDeclaredField("socketChannel");
		socketChannelField.setAccessible(true);
		socketChannelField.set(remoteLoadBalancer, expectedSocketChannel);

		SocketChannel socketChannel = remoteLoadBalancer.getSocketChannel();
		assertEquals(expectedSocketChannel, socketChannel);
	}
	
	/**
	 * Test the {@link RemoteLoadBalancer}'s <code>setSocketChannel</code>
	 * method. The method should set the <code>socketChannel</code>
	 * property of the object to the value passed in.
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws IOException 
	 */
	@Test
	public void testRemoteLoadBalancer_setSocketChannel() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000));
		SocketChannel expectedSocketChannel = SocketChannel.open();
		remoteLoadBalancer.setSocketChannel(expectedSocketChannel);

		Field socketChannelField = remoteLoadBalancer.getClass().getSuperclass().getDeclaredField("socketChannel");
		socketChannelField.setAccessible(true);

		assertEquals(expectedSocketChannel, socketChannelField.get(remoteLoadBalancer));
	}
	
	/**
	 * Test the {@link RemoteLoadBalancer}'s <code>connect</code>
	 * method. This method should ensure that only one TCP connection is ever active
	 * between two load balancer nodes. 
	 * @throws IOException 
	 */
	@Test
	public void testRemoteLoadBalancer_connect() throws IOException {
		ServerSocketChannel mockServerSocketChannel = ServerSocketChannel.open();
		mockServerSocketChannel.bind(new InetSocketAddress(8000));
		
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000));
		new Thread(new Runnable() {
		
			@Override
			public void run() {
				remoteLoadBalancer.connect(0);
			}
			
		}).start();
		
		mockServerSocketChannel.accept();
		
		assertTrue(remoteLoadBalancer.getSocketChannel().isConnected());
	}

}
