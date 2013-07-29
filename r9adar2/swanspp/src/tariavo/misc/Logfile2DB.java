package tariavo.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Properties;

/**
 * Convenience class: write file log into derby database.
 * Records like:<br>
 * time(bigint) who(varchar(255)) event(varchar(255)) notes(varchar(255)) ...
 * <br>
 * Ellipsis(add0, add1, ..) means the variable number of 'add_'.<br>
 * The number of columns is determined by the first row in the file.
 * @author tariavo (tariavo@mail.ru)
 *
 */
public class Logfile2DB {
	private String dbName = "derbyDB";
	private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	private String protocol = "jdbc:derby:";
	private String user = "user1";
	private String pass = "user1";
	private static int def_number_colmns = 4;
	private boolean spoke = true;
	
	private Connection conn = null;
	private Statement s;
	private int number_colmns;
	private boolean closed;
	
	public Logfile2DB(String dbName) {
		this.dbName = dbName;
	}
	
	public Logfile2DB() {}
	
	public void process(File file) {
		//time(bigint) who(varchar(255)) event(varchar(255)) notes(varchar(255))
		if(!file.exists()) {
			System.out.println("file doesnt exist: file = " + file);
			return;
		}
		String tableName = parseTableName(file.getName());
		number_colmns = getNumberColumns(file);
//		System.out.println("number_colmns = " + number_colmns);
//		System.out.println("def_number_colmns = " + def_number_colmns);
		if(number_colmns <= 0) {
			System.out.println("no columns(" + number_colmns
					+ ") were found, exit");
			return;
		}
		if(spoke) System.out.println("will be created table: " + tableName);
		
		initDB();
		initTable(tableName);
		
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(
					file));
			String str;
			String[] res;
			long time;
			String who, event, notes;
			String[] adds;
			if(number_colmns - def_number_colmns > 0)
				adds = new String[number_colmns - def_number_colmns];
			else adds = null;
			while((str = bufferedReader.readLine()) != null) {
				res = str.split("\t");
				if(res.length < 4) {
					throw new RuntimeException("res.length < 4: " +
							Arrays.toString(res));
				}
				time = Long.parseLong(res[0]);
				who = res[1];
				event = res[2];
				notes = res[3];
				if(adds != null) {
					for (int i = 0; i < adds.length; i++) {
						if(res.length >= i + def_number_colmns + 1) 
							adds[i] = res[i + def_number_colmns];
						else adds[i] = null;
					}
				}
				writeRecord(time, who, event, notes, adds, tableName);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		closeDB();
	}
	
	private int getNumberColumns(File file) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			reader.close();
//			default value, file is empty
			if(line == null) return def_number_colmns;
			return line.split("\t").length;
		} catch(IOException e ) {
			e.printStackTrace();
			return -1;
		}
	}

	private void writeRecord(long time, String who, String event,
			String notes, String[] adds, String tableName) {
//		System.out.println("Logfile2DB.writeRecord():adds = "
//				+ Arrays.toString(adds));
		try {
			String s_adds = "";
			for (int i = 0; i < number_colmns - def_number_colmns; i++) {
				if(adds == null || adds.length <= i || adds[i] == null) {
					s_adds = s_adds + ", " + null;
				} else {
					s_adds = s_adds + ", " + "'" + adds[i] + "'";
				}
			}
			s.execute("insert into " + tableName + " values (" + 
					time + ", '" + who + "', '" + event + "', '" + 
					notes + "'" + s_adds + ")");
			
		} catch (SQLException e) {
			printSQLError(e);
		}
	}

	private String parseTableName(String fileName) {
		String[] res;
		res = fileName.split("[.]");
		if(res.length == 0) return fileName;
		return res[0];
	}

	protected void initTable(String tableName) {
		try {			
			s = conn.createStatement();
			//check existence of the table
			boolean ex = true;
			try {
				s.execute("select * from " + tableName);
			} catch (SQLException e) {
				//doesnt exist
//				System.out.println("DBLogger.DBLogger(): table doesnt exist");
				ex = false;
			}
			
			if(ex) {
//				System.out.println("DBLogger.DBLogger(): drop table");
				s.execute("drop table " + tableName);
			}
			String s_string = "";
			for(int i = 0; i < number_colmns - def_number_colmns; i++) {
				s_string = s_string + ", add" + i + " varchar(255)";
			}
			s.execute("create table " + tableName
					+ "(time bigint, who varchar(255), event varchar(255)" + 
					", notes varchar(255)" + s_string + ")");
			conn.commit();
			//return
			
		}  catch (Throwable e) {
			if (e instanceof SQLException) {
				printSQLError((SQLException)e);
			} else {
				e.printStackTrace();
			}
		}
	}
	
	protected void closeDB() {
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
			DriverManager.getConnection("jdbc:derby:" + dbName
					+ ";shutdown=true");
		} catch (SQLException se) {
			gotSQLExc = true;
		}

		if (!gotSQLExc) {
			System.out.println("Database did not shut down normally");
		} else {
			if(spoke) System.out.println("Database shut down normally");
		}
		closed = true;
	}
	
	protected void printSQLError(SQLException e) {
//		e.printStackTrace();
		while (e != null) {
			System.out.println(e.toString());
			e = e.getNextException();
		}
	}
	
	
	
	public static void main(String[] args) {
	}
	
	public int getEventsNumber(String event, String tableName) {
		initDB();
		int number = 0;
		try {
			s = conn.createStatement();
			String sql = "select * from " + tableName + " where event='" + 
					event + "'";
			ResultSet rs = s.executeQuery(sql);

			while(rs.next()) {
				number++;
			}

		} catch (SQLException e) {
			printSQLError(e);
		}
		closeDB();
		return number;
	}
	
	private void initDB() {
		try {
			/*
			 * The driver is installed by loading its class. In an embedded
			 * environment, this will start up Derby, since it is not already
			 * running.
			 */
			if(conn == null || closed) {
				closed = false;
				Class.forName(driver).newInstance();
				
				if(spoke) System.out.println("Loaded the appropriate driver.");
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

				conn.setAutoCommit(false);
			}
		} catch (Throwable e) {
			if (e instanceof SQLException) {
				printSQLError((SQLException)e);
			} else {
				e.printStackTrace();
			}
		}
	}
	/**
	 * whether print intermidiate process.
	 * @param spoke
	 */
	public void setSpoken(boolean spoke) {
		this.spoke = spoke;
	}
}
