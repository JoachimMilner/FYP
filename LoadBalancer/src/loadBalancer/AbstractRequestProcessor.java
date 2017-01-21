package loadBalancer;

import java.nio.channels.SocketChannel;

import connectionUtils.MessageType;

/**
 * @author Joachim
 *         <p>
 * 		Abstract base class for the active and passive runnable request
 *         processor implementations.
 *         </p>
 *
 */
public abstract class AbstractRequestProcessor implements Runnable {

	/**
	 * The socket channel that this request processor will read messages from
	 * and respond to.
	 */
	protected SocketChannel socketChannel;

	
	/**
	 * The load balancer object that created this request processor. Allows this
	 * object to relay information back to a centralised location.
	 */
	protected AbstractLoadBalancer loadBalancer;

	
	/**
	 * Abstract method to be implemented by concrete classes to handle the
	 * incoming request types that the class will process.
	 */
	protected abstract void processMessage(MessageType messageType);

}
