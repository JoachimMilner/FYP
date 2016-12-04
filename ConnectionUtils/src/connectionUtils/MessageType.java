package connectionUtils;

public enum MessageType {
	CLIENT_REQUEST(0), SERVER_RESPONSE(1);
	
	private int value;
	
	private MessageType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}
}
