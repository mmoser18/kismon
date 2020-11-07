/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 07.08.2022
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

/**
 * class description here...
 */
@Slf4j
public class JDBCHandling implements Closeable
{
	protected Driver driver;
	protected Executor timeoutExecutor;

	private Connection connection;

	Executor getExecutor() {
		if (this.timeoutExecutor == null) {
			this.timeoutExecutor = Executors.newFixedThreadPool(3);
		}
		return this.timeoutExecutor;
	}

	private void registerDriver(String driverClassName) throws Exception {
		this.driver = (Driver)Class.forName(driverClassName).getDeclaredConstructor().newInstance();
		log.debug("registerDriver '{}': {}", driverClassName, this.driver); //$NON-NLS-1$
		DriverManager.registerDriver(this.driver);
	}

	Connection createConnection(String driverName, String url, String uid, String pwd, int timeout) throws Exception {
		log.debug("createConnection:"); //$NON-NLS-1$
		if (this.driver == null) {
			registerDriver(driverName);
		}
		if (this.connection == null || this.connection.isClosed() || !this.connection.isValid(10)) {
			this.connection = DriverManager.getConnection(url, uid, pwd);
			try {
				this.connection.setNetworkTimeout(getExecutor(), timeout * 1000); // sec --> msecs
			} catch (java.lang.AbstractMethodError ex) { // Oracle driver doesn't implement that method ||-(
				log.trace("setNetworkTimeout() not supported by {}", driverName); //$NON-NLS-1$
			}
		}
		return this.connection;
	}

	@Override
	public void close() {
		if (this.connection != null) {
			try {
				this.connection.close();
			} catch (SQLException ex) {
				log.error("Closing", ex); //$NON-NLS-1$
			}
		}
		this.timeoutExecutor = null;
	}

	void resetConnection() {
		synchronized(this) {
			if (this.connection != null) {
				try {
					this.connection.abort(getExecutor()); // the Oracle-Driver doesn't implement that...
				} catch (Throwable ex) { // ... and then throws an java.lang.reflect.InvocationTargetException
					log.trace("Exception aborting connection: " + ex.getMessage()); //$NON-NLS-1$
				}
				this.connection = null;
			}
			if (this.driver != null) {
				try {
					log.debug("deregisterDriver '{}'", this.driver); //$NON-NLS-1$
					DriverManager.deregisterDriver(this.driver);
				} catch (SQLException ex) {
					log.trace("Error deregistering driver {}", this.driver); //$NON-NLS-1$
				}
				this.driver = null;
			}
		}
	}

	/**
	 * @return a ResultSet or an updateCount (Integer)
	 * Note: it is important that the user later closes the statement by calling close(...) with the result of the query!
	 */
	@SuppressWarnings("resource")
	Object executeQuery(int statementTimeout, String resQuery) throws SQLException {
		try {
			Statement statement = this.connection.createStatement();
			statement.setQueryTimeout(statementTimeout);
			log.trace("statement.execute():", resQuery); //$NON-NLS-1$
			return (statement.execute(resQuery)
			       ? statement.getResultSet() // true: result is a ResultSet
			       : statement.getUpdateCount()); // false: result is not a ResultSet but an update count or there are no results
		} catch (SQLException ex) {
			this.connection.close(); // delete old connection so that a new connection is attempted. Otherwise, if a connection had been terminated no new one will ever be attempted
			throw ex;
		}
	}

	/**
	 * Extract the column names from the result set
	 * @param rs
	 * @return an array of column headers
	 * @throws SQLException
	 */
	public static String[] getColumnHeaders(ResultSet rs) throws SQLException {
		ResultSetMetaData metadata = rs.getMetaData();
		String[] headers = new String[metadata.getColumnCount()];
		for (int i = 0; i < metadata.getColumnCount(); i++) {
			headers[i] = metadata.getColumnLabel(i+1);
		}
		return headers;
	}


	/**
	 * Extract a list of string arrays, each contains a row's values
	 * @param rs
	 * @return list of string arrays, each string array contains a row's values (as strings)
	 * @throws SQLException
	 */
	public static List<String[]> getColumnValues(ResultSet rs) throws SQLException {
		ResultSetMetaData metadata = rs.getMetaData();
		int nrColumns = metadata.getColumnCount();
		List<String[]> values = new ArrayList<>();
		// for each row: add all the values:
		while (rs.next()) {
			String[] line = new String[nrColumns];
			for (int i = 0; i < nrColumns; i++) {
				Object value = rs.getObject(i+1);
				line[i] = (value != null ? value.toString().trim() : null);
			}
			values.add(line);
		}
		return values;
	}

	static void closeResultSet(Object res) {
		if (res instanceof ResultSet) {
			try {
				((ResultSet)res).getStatement().close(); // note: this also closes res!
			} catch (Exception ex) {
				log.error("Closing ResultSet", ex); //$NON-NLS-1$
			}
		}
	}
}