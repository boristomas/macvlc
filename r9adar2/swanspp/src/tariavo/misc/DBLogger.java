package tariavo.misc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Properties;

import jist.runtime.JistAPI;

/**
 * Backup of prev logger(that doesnt work because of "rewriter" problem:
 * jist.runtime.Rewriter cant find some class(it is really not in build path), 
 * but default system ClassLoader doesnt throw any exception, then when i
 * put this class in build path jist.runtime.Rewriter cant find class that is 
 * presented in build path(accurate: cant find unknown class Lorg...., when 
 * class org.... exists))
 * @author tariavo (tariavo@mail.ru)
 *
 */
public class DBLogger implements ClosableLogger {
	private static String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	private static String protocol = "jdbc:derby:";
	private static String dbName = "derbyDB";
	private static String user = "user1";
	private static String pass = "user1";
	private static Connection conn = null;
	
	private static Hashtable/*DBLogger*/ instance_table = new Hashtable();
	
	private ClosableLogger self = (ClosableLogger) JistAPI.proxy(this,
			ClosableLogger.class);
	private String tableName;

	/**
	 * @param tableName
	 * @return
	 */
	public synchronized static ClosableLogger getProxyInstance
	(String tableName) {
		if(!instance_table.containsKey(tableName)) {
			instance_table.put(tableName, new DBLogger(tableName));
		}
		
		return ((DBLogger)instance_table.get(tableName)).getProxy();
	}
	
	private ClosableLogger getProxy() {
		return self;
	}

	private DBLogger(String tableName) {
		this.tableName = tableName;
		try {
			/*
			 * The driver is installed by loading its class. In an embedded
			 * environment, this will start up Derby, since it is not already
			 * running.
			 */
			if(conn == null) {
				/*
				 * The use of standart system ClassLoader beacause the
				 * jist.runtime.Rewriter has bootstrap problem
				 * (uncomment and see)
				 * ("JiST Java bootstrap - Class not found:
				 * org.apache.xml.utils.PrefixResolverDefault")
				 */
				Class.forName(driver, true,	ClassLoader.getSystemClassLoader())
				.newInstance();
				
//				EmbeddedDriver driver_  = new EmbeddedDriver();
				
//				System.out.println("Loaded the appropriate driver.");
				Properties props = new Properties();
				props.put("user", user);
				props.put("password", pass);

				/*
				 * The connection specifies create=true to cause the database to
				 * be created. To remove the database, remove the directory
				 * derbyDB and its contents. The directory derbyDB will be
				 * created under the directory that the system property
				 * derby.system.home points to, or the current directory if
				 * derby.system.home is not set.
				 */
				conn = DriverManager.getConnection(protocol + dbName
						+ ";create=true", props);

				// System.out.println("Connected to and created database
				// derbyDB");
				//T-ODO change if there will be overload
				conn.setAutoCommit(true);
			}
			
			Statement s = conn.createStatement();
			//check existence of the table
			boolean ex = true;
			try {
				s.execute("select * from " + tableName);
			} catch (SQLException e) {
				//doesnt exist
				System.out.println("DBLogger.DBLogger(): table doesnt exist");
				ex = false;
			}
			
			if(ex) {
				System.out.println("DBLogger.DBLogger(): drop table");
				s.execute("drop table " + tableName);
			}
			
			s.execute("create table " + tableName
					+ "(time bigint, who varchar(255), event varchar(255)" + 
					", notes varchar(255))");
			conn.commit();
			s.close();
			//return
			
		}  catch (Throwable e) {
			if (e instanceof SQLException) {
				printSQLError((SQLException) e);
			} else {
				e.printStackTrace();
			}
		}
	}
	
	protected void printSQLError(SQLException e) {
		e.printStackTrace();
		/*while (e != null) {
			System.out.println(e.toString());
			e = e.getNextException();
		}*/
	}
	
	public void log(String who, String event, String notes) {
		System.out.println("DBLogger.log(): " + tableName);
		try {
			Statement s = conn.createStatement();
			long time = JistAPI.getTime();
	//time(bigint) who(varchar(255)) event(varchar(255)) notes(varchar(255))
			s.execute("insert into " + tableName + " values (" + 
					time + ", '" + who + "', '" + event + "', '" + 
					notes + "')");
			s.close();
			
		} catch (Throwable e) {
			if (e instanceof SQLException) {
				printSQLError((SQLException) e);
			} else {
				e.printStackTrace();
			}
		}
	}

	/**{@inheritDoc}*/
	public void close() {
		try {
			conn.commit();
			conn.close();
		} catch(Throwable e) {
			if (e instanceof SQLException) {
				printSQLError((SQLException) e);
			} else {
				e.printStackTrace();
			}
		}
		
		boolean gotSQLExc = false;

		try {
			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		} catch (SQLException se) {
			gotSQLExc = true;
		}

		if (!gotSQLExc) {
			System.out.println("Database did not shut down normally");
		} else {
			System.out.println("Database shut down normally");
		}
	}

	public void flush() {
		try {
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
