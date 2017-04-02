package log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Joachim
 * <p>Utility methods for the NodeMonitor's logging functionality</p>
 *
 */
public class LoggerUtility {
	
	static {
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy_HHmmss");
		Date date = new Date();
		programStartTimestamp = format.format(date);
		
		File logDirectory = new File("logs");
	    try{
	    	logDirectory.mkdir();
	    } 
	    catch(SecurityException se){
	        System.out.println("Could not create log directory.");
	    }  
	}
	
	/**
	 * Formatted string to be used in the names of any log files.
	 */
	private static String programStartTimestamp;
	
	/**
	 * PrintWriter for this session's log
	 */
	private static BufferedWriter logWriter;
	
	/**
	 * PrintWriter for this session's server load csv log.
	 */
	private static BufferedWriter csvWriter;
	
	/**
	 * Time format including hours, minutes, seconds and milliseconds
	 */
	private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.S");
	
	/**
	 * Full date/time format from year to milliseconds
	 */
	private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.S");
	
	/**
	 * @return a string representing the current time to millisecond accuracy
	 */
	public static String getFormattedTimestamp() {
		Date date = new Date();
		return timeFormat.format(date);
	}
	
	/**
	 * @return a string representing the current date and time to millisecond accuracy
	 */
	public static String getFormattedDateTimestamp() {
		Date date = new Date();
		return dateTimeFormat.format(date);
	}
	
	/**
	 * Writes the specified message to the log file for this session. 
	 * @param info the message to be logged
	 */
	public static synchronized void logInfo(String info) {
		if (logWriter == null) {
			try {
				logWriter = new BufferedWriter(new OutputStreamWriter(
				          new FileOutputStream("logs/logNodeMonitorLog" + programStartTimestamp + ".log"), "utf-8"));
			} catch (UnsupportedEncodingException | FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		try {
			logWriter.write(getFormattedTimestamp() + " " + info);
			logWriter.newLine();
			logWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Logs the given server CPU load reading to the .csv file for this session.
	 * @param timestamp the time that the CPU load reading was taken (seconds since program start)
	 * @param serverID the ID of the server
	 * @param cpuLoadReading the CPU load reading
	 */
	public static synchronized void logServerLoadReading(double timestamp, int serverID, double cpuLoadReading) {
		if (csvWriter == null) {
			try {
				csvWriter = new BufferedWriter(new OutputStreamWriter(
				          new FileOutputStream("logs/serverCPULoadData" + programStartTimestamp + ".csv"), "utf-8"));
			} catch (UnsupportedEncodingException | FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		try {
			csvWriter.write(serverID + "," + timestamp + "," + cpuLoadReading);
			csvWriter.newLine();
			csvWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Closes resources - to be called on program shutdown
	 */
	public static void cleanup() {
		try {
			if (logWriter != null) {
				logWriter.close();
			}
			if (csvWriter != null) {
				csvWriter.close();
			}
		} catch (IOException e) {
		}
	}
}
