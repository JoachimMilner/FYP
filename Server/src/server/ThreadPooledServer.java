package server;

import connectionUtils.ConnectableComponent;

/**
 * @author Joachim
 * This class is used to initialise and manage a thread pool of {@link RunnableRequestProcessor} instances.
 * Exactly one ThreadPooledServer should be instantiated and started in a thread when the application starts.
 */
public class ThreadPooledServer implements ConnectableComponent, Runnable {

	/**
	 * The size of the thread pool to be created for handling received client requests.
	 */
	private int threadPoolSize;
	
	
	/**
	 * Creates a new ThreadPooledServer instance that will create and start a pool of {@link RunnableRequestProcessor} threads
	 * once started. This object must be passed to the {@link Thread} constructor so that its <code>run</code> method can be called.
	 * @param threadPoolSize the size of the thread pool that this ThreadPooledServer will create.
	 */
	public ThreadPooledServer(int threadPoolSize) {
		if (threadPoolSize < 1) throw new IllegalArgumentException("Thread pool size must be at least 1.");
		
		this.threadPoolSize = threadPoolSize;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
