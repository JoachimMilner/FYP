package server;

/**
 * @author Joachim</br>
 *         Class based on code from:
 *         http://stackoverflow.com/questions/2596493/junit-assert-in-thread-throws-exception/2596530</br>
 *         This class allows indirect checking of exceptions that are thrown in
 *         sub-threads during unit tests.
 */
public class AsyncTester {
	
	private Thread thread;
	private volatile AssertionError exc;

	public AsyncTester(final Runnable runnable){
        thread = new Thread(new Runnable(){
            public void run(){
                try{            
                    runnable.run();
                }catch(AssertionError e){
                    exc = e;
                }
            }
        });
    }

	public void start() {
		thread.start();
	}

	public void test() throws InterruptedException {
		thread.join();
		if (exc != null)
			throw exc;
	}
}
