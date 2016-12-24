package connectionUtils;

public enum MessageType {
	CLIENT_REQUEST(0), SERVER_RESPONSE(1), HOST_ADDR_NOTIFY(2), HOST_ADDR_REQUEST(3), SERVER_CPU_REQUEST(4), SERVER_CPU_LOAD_NOTIFY(5);
	
	private int value;
	
	private MessageType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}
}
