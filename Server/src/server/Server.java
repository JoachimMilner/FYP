package server;

public class Server {

	public static void main(String[] args) {
		Server instance = new Server();
		instance.launch(args);
	}
	
	private void launch(String[] args) {
		int threadPoolSize = 0;
		try {
			threadPoolSize = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Thread pool size must be an integer.");
		}
		int connectPort = 0;
		try {
			connectPort = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Connection port must be an integer.");
		}
		ThreadPooledServer server = new ThreadPooledServer(threadPoolSize, connectPort);
		new Thread(server).start();
	}

}
