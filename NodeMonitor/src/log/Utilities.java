package log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utilities {
	
	private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.S");
	
	private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.S");
	
	public static String getFormattedTimestamp() {
		Date date = new Date();
		return timeFormat.format(date);
	}
	
	public static String getFormattedDateTimestamp() {
		Date date = new Date();
		return dateTimeFormat.format(date);
	}

}
