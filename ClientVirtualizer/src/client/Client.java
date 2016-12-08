package client;

public class Client {
	
	public static void main(String[] args) {
		Client instance = new Client();
		instance.launch(args);

	}
	
	private void launch(String[] args) {
		VirtualClientManager clientManager = new VirtualClientManager(5, 500, 1000);
		clientManager.initialiseClientPool();
	}
}
