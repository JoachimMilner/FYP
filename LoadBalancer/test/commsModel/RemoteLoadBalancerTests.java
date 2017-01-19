package commsModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;

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
	 * Test the {@link RemoteLoadBalancer}'s <code>getElectionOrdinality</code>
	 * method. The method should return the int value that we set using
	 * reflection.
	 */
	@Test
	public void testRemoteLoadBalancer_getElectionOrdinality()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000));
		int expectedElectionOrdinality = 1;

		Field electionOrdinalityField = remoteLoadBalancer.getClass().getDeclaredField("electionOrdinality");
		electionOrdinalityField.setAccessible(true);
		electionOrdinalityField.set(remoteLoadBalancer, expectedElectionOrdinality);

		int electionOrdinality = remoteLoadBalancer.getElectionOrdinality();
		assertEquals(expectedElectionOrdinality, electionOrdinality);
	}

	/**
	 * Test the {@link RemoteLoadBalancer}'s <code>setElectionOrdinality</code>
	 * method. The method should set the <code>electionOrdinality</code>
	 * property of the object to the value passed in.
	 */
	@Test
	public void testRemoteLoadBalancer_setElectionOrdinality()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		RemoteLoadBalancer remoteLoadBalancer = new RemoteLoadBalancer(new InetSocketAddress("localhost", 8000));
		int expectedElectionOrdinality = 1;
		remoteLoadBalancer.setElectionOrdinality(expectedElectionOrdinality);

		Field electionOrdinalityField = remoteLoadBalancer.getClass().getDeclaredField("electionOrdinality");
		electionOrdinalityField.setAccessible(true);

		assertEquals(expectedElectionOrdinality, electionOrdinalityField.get(remoteLoadBalancer));
	}

}