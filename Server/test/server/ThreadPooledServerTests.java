package server;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

/**
 * @author Joachim
 * <p>Tests for the {@link ThreadPooledServer} class and its instance methods.</p>
 */
public class ThreadPooledServerTests {

	/**
	 * Tests creating an instance of the {@link ThreadPooledServer} class. 
	 */
	@Test
	public void testCreateThreadPooledServer_successful() {
		ThreadPooledServer server = new ThreadPooledServer(1);
		assertNotNull(server);
	}
	
	/**
	 * Tests creating an instance of the {@link ThreadPooledServer} class. 
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testCreateThreadPooledServer_threadPoolTooSmall() {
		ThreadPooledServer server = new ThreadPooledServer(0);
	}
}
