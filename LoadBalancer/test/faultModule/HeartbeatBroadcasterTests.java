package faultModule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import commsModel.RemoteLoadBalancer;
import testUtils.TestUtils;

/**
 * @author Joachim
 *         <p>
 *         Tests for the {@link HeartbeatBroadcaster} class and its methods.
 *         </p>
 *
 */
public class HeartbeatBroadcasterTests {

	/**
	 * Tests successful creation of a new {@link HeartbeatBroadcaster}.
	 */
	@Test
	public void testCreateHeartbeatBroadcaster_successful() {
		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(TestUtils.getRemoteLoadBalancerSet(1), 3);
		assertNotNull(heartbeatBroadcaster);
	}

	/**
	 * Tests creating a new {@link HeartbeatBroadcaster} instance with a null
	 * <code>remoteLoadBalancers</code> set passed in. Should throw
	 * IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateHeartbeatBroadcaster_nullNodeSet() {
		new HeartbeatBroadcaster(null, 3);
	}

	/**
	 * Tests creating a new {@link HeartbeatBroadcaster} instance with an empty
	 * <code>remoteLoadBalancers</code> set passed in. Should throw
	 * IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateHeartbeatBroadcaster_emptyNodeSet() {
		new HeartbeatBroadcaster(new HashSet<RemoteLoadBalancer>(), 3);
	}
	
	/**
	 * Tests creating a new {@link HeartbeatBroadcaster} instance with 
	 * <code>heartbeatIntervalSecs</code> set to less than 1. Should throw
	 * IllegalArgumentException
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateHeartbeatBroadcaster_invalidHBInterval() {
		new HeartbeatBroadcaster(TestUtils.getRemoteLoadBalancerSet(1), 0);
	}
	
	/**
	 * Tests that the <code>remoteLoadBalancers</code> property of the
	 * {@link HeartbeatBroadcaster} is set correctly in the class constructor. Use
	 * reflection to check the value after the constructor is called.
	 */
	@Test
	public void testHeartbeatBroadcasterConstructor_remoteLBsAreSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Set<RemoteLoadBalancer> expectedLBSet = TestUtils.getRemoteLoadBalancerSet(1);
		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(expectedLBSet, 3);

		Field remoteLBField = heartbeatBroadcaster.getClass().getDeclaredField("remoteLoadBalancers");
		remoteLBField.setAccessible(true);
		assertEquals(expectedLBSet, remoteLBField.get(heartbeatBroadcaster));
	}
	
	/**
	 * Tests that the <code>heartbeatIntervalSecs</code> property of the
	 * {@link HeartbeatBroadcaster} is set correctly in the class constructor. Use
	 * reflection to check the value after the constructor is called.
	 */
	@Test
	public void testHeartbeatBroadcasterConstructor_heartbeatIntervalIsSet()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		int expectedHeartbeatInterval = 5;
		HeartbeatBroadcaster heartbeatBroadcaster = new HeartbeatBroadcaster(TestUtils.getRemoteLoadBalancerSet(1), expectedHeartbeatInterval);

		Field heartbeatIntervalField = heartbeatBroadcaster.getClass().getDeclaredField("heartbeatIntervalSecs");
		heartbeatIntervalField.setAccessible(true);
		assertEquals(expectedHeartbeatInterval, heartbeatIntervalField.get(heartbeatBroadcaster));
	}
}
