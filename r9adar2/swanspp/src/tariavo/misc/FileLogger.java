package tariavo.misc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import jist.runtime.JistAPI;


/**
 * Simple logger class, using file. Creating records have following template:
 * time(long) \t who(String) \t event(String) \t notes(String) {new line}
 * 
 * @author tariavo (tariavo@mail.ru)
 * 
 */
public class FileLogger implements ClosableLogger {
	private static String dir = "stats";
	private static Hashtable /*FileLogger*/ name_logger = new Hashtable();
	
	
	private ClosableLogger self = (ClosableLogger) JistAPI.proxy(this,
			ClosableLogger.class);
	private PrintWriter printWriter;
	private String fileName;
	private boolean closed = false;
	
	public synchronized static ClosableLogger getProxyInstance
	(String fileName) throws IOException {
		if(!name_logger.containsKey(fileName)) {
			name_logger.put(fileName, new FileLogger(fileName));
		}
		
		return ((FileLogger)name_logger.get(fileName)).getProxy();
	}

	/**
	 * Close all existing FileLoggers.
	 *
	 */
	public synchronized static void closeAll() {
		Enumeration keys =  name_logger.keys();
		
		while(keys.hasMoreElements()) {
			FileLogger fl = (FileLogger) name_logger.get(keys.nextElement());
			fl.close();			
			
			/*synchronized (fl.fileWriter) {
				if(!fl.isClosed()) {
					try {
						fl.fileWriter.flush();
						fl.fileWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}*/
		}
	}
	
	private FileLogger(String fileName) throws IOException {
		this.fileName  = dir + File.separator + fileName;
		(new File(dir)).mkdir();
		printWriter = new PrintWriter(
				new FileWriter(this.fileName, false), true);
	}

	/** {@inheritDoc}*/
	public void close() {
		synchronized (printWriter) {
			if(isClosed()) return;
			printWriter.flush();
			printWriter.close();
			closed = true;
		}
	}

	/**{@inheritDoc}
	 * Nothing if FileLogger has been closed. 
	 */
	public synchronized void log(String who, String event, String notes) {
		synchronized (printWriter) {
			if(isClosed()) return;
			
			long time = JistAPI.getTime();
			printWriter.println(time + "\t" + who + "\t"
					+ event + "\t" + notes);
		}
	}

	private ClosableLogger getProxy() {
		return self;
	}
	
	/**
	 * Called from internal synchronized blocks => doesn't need synchronizing.
	 * @return
	 */
	private boolean isClosed() {
		return closed;
	}

	public void flush() {
		printWriter.flush();
	}
}