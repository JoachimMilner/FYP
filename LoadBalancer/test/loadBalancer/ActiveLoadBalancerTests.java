package loadBalancer;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;

import org.junit.Test;

/**
 * @author Joachim
 *         <p>
 *         Tests for the {@link ActiveLoadBalancer} class and its instance
 *         methods.
 *         </p>
 */
public class ActiveLoadBalancerTests {

	/**
	 * Test successful creation of a new {@link ActiveLoadBalancer} instance.
	 */
	@Test
	public void testCreateActiveLoadBalancer_successful() {
		AbstractLoadBalancer loadBalancer = new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1),
				TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8000));
		assertNotNull(loadBalancer);
	}

	/**
	 * Test creating a new {@link ActiveLoadBalancer} instance with null passed for the set of 
	 * remote load balancers. Should throw {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveLoadBalancer_nullRemoteLBSet() {
		new ActiveLoadBalancer(8000, null, TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8000));
	}
	
	/**
	 * Test creating a new {@link ActiveLoadBalancer} instance with null passed for the set of 
	 * remote servers. Should throw {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveLoadBalancer_nullServerSet() {
		new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1), null, new InetSocketAddress("localhost", 8000));
	}
	
	/**
	 * Test creating a new {@link ActiveLoadBalancer} instance with an empty set of 
	 * remote load balancers. Should throw {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveLoadBalancer_emptyRemoteLBSet() {
		new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(0), TestUtils.getServerSet(1), new InetSocketAddress("localhost", 8000));
	}
	
	/**
	 * Test creating a new {@link ActiveLoadBalancer} instance with an empty set of 
	 * remote servers. Should throw {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveLoadBalancer_emptyServerSet() {
		new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1), TestUtils.getServerSet(0), new InetSocketAddress("localhost", 8000));
	}

	/**
	 * Test creating a new {@link ActiveLoadBalancer} instance with the name service address
	 * passed as null. Should throw {@link IllegalArgumentException}.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCreateActiveLoadBalancer_nullNameServiceAddress() {
		new ActiveLoadBalancer(8000, TestUtils.getRemoteLoadBalancerSet(1), TestUtils.getServerSet(1), null);
	}

}
