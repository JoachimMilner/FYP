package logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.atomic.AtomicInteger;

import connectionUtils.ConnectNIO;

/**
 * @author Joachim
 *         <p>
 *         Singleton class used by all system components for logging at the
 *         centralised NodeMonitor. On component initialisation, the component
 *         registers with the NodeMonitor using this class and in turn receives
 *         a unique ID which is used in subsequent logging.
 *         </p>
 *
 */
public class ComponentLogger {

	/**
	 * The instance used to make this class singleton.
	 */
	private static ComponentLogger instance;

	/**
	 * The address held for the node monitor.
	 */
	private static InetSocketAddress nodeMonitorAddress;

	/**
	 * The SocketChannel that this component is connected to the node monitor
	 * through.
	 */
	private SocketChannel socketChannel;

	/**
	 * The unique ID of this component, assigned by the node monitor.
	 */
	private int componentID = -1;

	/**
	 * Private constructor for singleton purposes.
	 */
	private ComponentLogger() {
	}

	/**
	 * Singleton method for this class. Important that the component using this
	 * class sets the address of the node monitor through the
	 * <code>setNodeMonitorAddress</code> method.
	 * 
	 * @return a new instance of this class if one has not already been created,
	 *         otherwise returns the singleton instance.
	 */
	public static ComponentLogger getInstance() {
		if (nodeMonitorAddress == null) {
			throw new IllegalStateException(
					"The address of the node monitor must be set before this class can be initialised.");
		}
		if (instance == null) {
			instance = new ComponentLogger();
		}
		return instance;
	}

	/**
	 * @param nodeMonitorAddress
	 *            the address of the node monitor.
	 */
	public static void setMonitorAddress(InetSocketAddress address) {
		nodeMonitorAddress = address;
	}

	/**
	 * Attempts to register this component with the node monitor, expecting to
	 * receive a unique ID for this node in response. Should only be called
	 * once. If the registration is successful, this method returns the
	 * connected SocketChannel.
	 * 
	 * @param registrationType
	 *            the type of this component (i.e. Client, Server, NameService
	 *            or LoadBalancer)
	 * @param additionalClientParams
	 *            a little messy but allows the client to send its default
	 *            configuration values on registration.
	 * @return the SocketChannel that has been connected to the NodeMonitor if
	 *         successful, otherwise null.
	 */
	public SocketChannel registerWithNodeMonitor(LogMessageType registrationType, int... additionalClientParams) {
		System.out.println("Attempting to register with NodeMonitor...");
		if (socketChannel == null) {
			socketChannel = ConnectNIO.getNonBlockingSocketChannel(nodeMonitorAddress);
		}

		ByteBuffer buffer = ByteBuffer.allocate(21);
		buffer.put((byte) registrationType.getValue());
		if (additionalClientParams.length > 0) {
			buffer.putInt(additionalClientParams[0]);
			buffer.putInt(additionalClientParams[1]);
			buffer.putInt(additionalClientParams[2]);
			buffer.putInt(additionalClientParams[3]);
			buffer.putInt(additionalClientParams[4]);
		}
		buffer.flip();

		try {
			while (buffer.hasRemaining()) {
				socketChannel.write(buffer);
			}
			Selector readSelector = Selector.open();
			socketChannel.register(readSelector, SelectionKey.OP_READ);
			if (readSelector.select(5000) == 0) {
				throw new SocketTimeoutException("Failed to register with NodeMonitor.");
			}
			buffer.clear();
			socketChannel.read(buffer);
			buffer.flip();
			LogMessageType response = LogMessageType.values()[buffer.get()];
			if (response.equals(LogMessageType.REGISTRATION_CONFIRM)) {
				System.out.println("Successfully registered with NodeMonitor.");
				componentID = buffer.getInt();
			} else {
				System.out.println("Failed to register with NodeMonitor.");
				return null;
			}
		} catch (IOException e) {
			System.out.println("Failed to register with NodeMonitor.");
		}
		return socketChannel;
	}

	/**
	 * Sends a log message to the NodeMonitor of the specified type. Attaches
	 * the ID of this component and any parameters that are provided.
	 * 
	 * @param logMessageType
	 *            the type of log message to be sent
	 * @param params
	 *            any additional parameters that are used for this log message
	 */
	public void log(LogMessageType logMessageType, Object... params) {
		ByteBuffer buffer = ByteBuffer.allocate(50);
		buffer.put((byte) logMessageType.getValue());
		buffer.putInt(componentID);
		for (int i = 0; i < params.length; i++) {
			Object param = params[i];
			@SuppressWarnings("rawtypes")
			Class paramClass = params[i].getClass();
			if (paramClass.equals(String.class)) {
				CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
				try {
					buffer.put(encoder.encode(CharBuffer.wrap((String) param)));
				} catch (CharacterCodingException e) {
				}
			} else if (paramClass.equals(Integer.class)) {
				buffer.putInt((int) param);
			} else if (paramClass.equals(AtomicInteger.class)) {
				AtomicInteger atomicInt = (AtomicInteger) param;
				buffer.putInt(atomicInt.get());
			} else if (paramClass.equals(Double.class)) {
				buffer.putDouble((double) param);
			}
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			try {
				socketChannel.write(buffer);
			} catch (IOException | NullPointerException e) {
				// e.printStackTrace();
			}
		}
	}
}
