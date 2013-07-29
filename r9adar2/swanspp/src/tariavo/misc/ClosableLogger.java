package tariavo.misc;
/**
 * 
 * @author tariavo (tariavo@mail.ru)
 *
 */
public interface ClosableLogger extends Logger {
	/**
	 * close correctly
	 */
	void close();
	
	void flush();
}
