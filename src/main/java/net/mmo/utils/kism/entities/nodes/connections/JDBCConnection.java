/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ExceptionUtils;

@Setter
@Getter
@Slf4j
public class JDBCConnection extends TCPConnection
{
	private static final long serialVersionUID = -2228821478937714703L;

	public static final String PROPERTYNAME_RESPONSE_STATUS  = "responseStatus"; //$NON-NLS-1$
	public static final String PROPERTYNAME_RESPONSE_PAYLOAD = "responsePayload"; //$NON-NLS-1$
	public static final String RESPONSE_STATUS_OK = "OK"; //$NON-NLS-1$
	public static final String RESPONSE_STATUS_FAILED = "FAILED"; //$NON-NLS-1$

	public static final String RESULTSET_ROW_SEPARATOR = AppProperties.getProperties().getProperty("JDBC.ResultSet.Row.Separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final String RESULTSET_COL_SEPARATOR = AppProperties.getProperties().getProperty("JDBC.ResultSet.Col.Separator", "\t"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final String RESULTSET_NR_RESULTS = AppProperties.getProperties().getProperty("JDBC.ResultSet.NrRresults", "#results: {}"); //$NON-NLS-1$ //$NON-NLS-2$
	public static final String RESULTSET_NO_RESULTS = AppProperties.getProperties().getProperty("JDBC.ResultSet.NoResults", "no results"); //$NON-NLS-1$ //$NON-NLS-2$

	protected String dbUid;
	protected String dbPwd;

	protected String query;

	protected String driverName =
		AppProperties.getProperties().getProperty("JDBC.DriverName.Default", //$NON-NLS-1$
		                                          "oracle.jdbc.driver.OracleDriver"); //$NON-NLS-1$

	// transient stuff:

	@JsonIgnore
	transient protected Driver driver;
	@JsonIgnore
	transient protected Connection connection;

	@JsonIgnore
	transient protected Executor timeoutExecutor = Executors.newFixedThreadPool(3);

	// result stuff - later to be collected into some "history"-object:

	@JsonIgnore
	transient protected String resultingQuery;
	@JsonIgnore
	transient protected String responseStatus;
	@JsonIgnore
	transient protected String responsePayload;


	public JDBCConnection() { // required for deserialization & node-factory
		super();
	}

	public JDBCConnection(String name, String description) {
		super(name, description);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		if (this.connection != null) {
			this.connection.close();
		}
		super.finalize();
	}

	@Override
	public void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingQuery();
	}

	public void setDbUid(String uid) {
		this.dbUid = uid;
		resetConnection(); // trigger a recreation of the connection if the dbUid changes
	}
	public void setDbPwd(String pwd) {
		this.dbPwd = pwd;
		resetConnection(); // trigger a recreation of the connection if the dbPwd changes
	}

	public void setQuery(String query) {
		this.query = query;
		updateResultingQuery();
	}

	private void updateResultingQuery() {
		updateResultingValue(getQuery(), (str) -> setResultingQuery(str));
	}

	public String resultingQueryResolved() throws Exception {
		return resultingFieldResolved(getQuery(), () -> getResultingQuery(), (str) -> setResultingQuery(str));
	}

	public void setResponseStatus(String responseStatus) {
		if (!Objects.equal(this.responseStatus, responseStatus)) {
			String oldResponseStatus = this.responseStatus;
			this.responseStatus = responseStatus;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_STATUS, oldResponseStatus, responseStatus);
		}
	}
	public void setResponsePayload(String responsePayload) {
		if (!Objects.equal(this.responsePayload, responsePayload)) {
			String oldResponsePayload = this.responsePayload;
			this.responsePayload = responsePayload;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_PAYLOAD, oldResponsePayload, responsePayload);
		}
	}

	private void resetConnection() {
		synchronized(this) {
			if (this.connection != null) {
				try {
					this.connection.abort(this.timeoutExecutor); // the Oracle-Driver doesn't implement that...
				} catch (Throwable ex) { // ... and then throws an java.lang.reflect.InvocationTargetException
					log.trace("Exception aborting connection: " + ex.getMessage()); //$NON-NLS-1$
				}
			}
			setConnection(null);
		}
	}

	private void registerDriver() throws Exception {
		log.debug("registerDriver:"); //$NON-NLS-1$
		setDriver((Driver)Class.forName(resolveProperties(this.driverName)).getDeclaredConstructor().newInstance());
		DriverManager.registerDriver(this.driver);
	}

	private void createConnection() throws Exception {
		log.debug("createConnection:"); //$NON-NLS-1$
		if (getDriver() == null) {
			registerDriver();
		}
		if (this.connection == null || this.connection.isClosed() || !this.connection.isValid(15)) {
			this.connection = DriverManager.getConnection(resultingUrlResolved(), resolveProperties(getDbUid()), resolveProperties(getDbPwd()));
			try {
				this.connection.setNetworkTimeout(this.timeoutExecutor, this.timeout * 1000); // sec --> msecs
			} catch (java.lang.AbstractMethodError ex) { // Oracle driver doesn't implement that method ||-(
				log.trace("setNetworkTimeout() not supported by {}", this.getDriverName()); //$NON-NLS-1$
			}
		}
	}

	@SuppressWarnings("resource")
	@Override
	public void sendRequest() throws Exception {
		log.debug("sendRequest {}:", getName()); //$NON-NLS-1$
		Statement statement = null;
		setTimestamp(LocalDateTime.now()); // must be BEFORE createConnection so that we get a timestamp in the case this fails
		String resQuery = resultingQueryResolved();
		Connection reqConnection = null;
		try {
			synchronized(this) {
				if (getConnection() == null) createConnection();
				reqConnection = getConnection();
			}
			statement = reqConnection.createStatement();
			statement.setQueryTimeout(this.timeout);
			setResponseStatus("<waiting>"); //$NON-NLS-1$
			setResponsePayload("<no response received (yet>"); //$NON-NLS-1$
			log.trace("statement.execute():", resQuery); //$NON-NLS-1$
			long startTime = System.nanoTime();
			boolean isRS = statement.execute(resQuery);
			long callDuration = System.nanoTime() - startTime;
			setDuration(callDuration);
			log.trace("responseReceived for '{}' after {} microsecs.", getName(), callDuration/1000); //$NON-NLS-1$
			responseReceived(statement, isRS);
			setRequestResult(getState().name() + '/' + getResponseStatus());
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());
		} catch (Exception ex) {
			log.debug("exception executing '{}': {}", getName(), ex.getMessage()); //$NON-NLS-1$
			setResponseStatus(ex instanceof SQLException ? extractSqlError((SQLException)ex) : ex.getMessage());
			setResponsePayload(String.format("Exception executing '%s' (query '%s'): %s", //$NON-NLS-1$
			                                 getName(), resQuery, ExceptionUtils.exceptionCauseSummary(ex)));
			setDuration(NO_RESPONSE_DURATION); // signals an exception
			setState(State.FAILED);
			setRequestResult(getState().name() + '/' + getResponseStatus());
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());
			if (reqConnection != null) {
				if (ex instanceof SQLException) setConnection(null); // delete old connection so that a new connection is attempted. Otherwise, if a connection had been terminate no new one will ever be attempted
				throw new Exception(String.format("sending query '%s' to %s: ", resQuery, getResultingUrl()), ex); //$NON-NLS-1$
			} else {
				throw new Exception(String.format("creating query to '%s'", getResultingUrl()), ex); //$NON-NLS-1$
			}
		} finally {
			if (statement != null) {
				statement.close();
			}
		}
	}

	private void responseReceived(Statement statement, boolean isRS) throws Exception {
		if (isRS) { // result is a ResultSet
			convertResultSet(statement);
			log.debug("response: status={} / payload={} bytes", //$NON-NLS-1$
			         getResponseStatus(), getResponsePayload().length());
			log.trace("payload=\"{}\"", //$NON-NLS-1$
				      getResponsePayload());
			deriveState();
		} else { // result is not a ResultSet but an update count or there are no results
			int updates = statement.getUpdateCount();
			log.debug("Result: #updates:{}", updates); //$NON-NLS-1$
			State res = updates >= 0 ? State.OK : State.FAILED;
			setResponseStatus(res.name());
			setResponsePayload(res == State.OK
							   ? String.format(RESULTSET_NR_RESULTS, updates)
							   : String.format(RESULTSET_NO_RESULTS));
			setState(res);
		}
	}

	private void convertResultSet(Statement statement) {
		try (ResultSet rs = statement.getResultSet()) {
			ResultSetMetaData metadata = rs.getMetaData();
			StringBuilder buf = new StringBuilder();
			// add all column names:
			for (int i = 0; i < metadata.getColumnCount(); i++) {
				if (i > 0) buf.append(RESULTSET_COL_SEPARATOR);
				buf.append(metadata.getColumnLabel(i+1));
			}
			buf.append(RESULTSET_ROW_SEPARATOR);
			// add a separator line
			for (int i = 0; i < metadata.getColumnCount(); i++) {
				if (i > 0) buf.append(RESULTSET_COL_SEPARATOR);
				for (int j = 0; j < metadata.getColumnLabel(i+1).length(); j++) {
					buf.append('-');
				}
			}
			// for each row: add all the values:
			while (rs.next()) {
				buf.append(RESULTSET_ROW_SEPARATOR);
				for (int i = 0; i < metadata.getColumnCount(); i++) {
					if (i > 0) buf.append(RESULTSET_COL_SEPARATOR);
					Object value = rs.getObject(i+1);
					if (value == null) {
						buf.append("null"); //$NON-NLS-1$
					} else {
						buf.append(value.toString().trim());
					}
				}
			}
			setResponseStatus(RESPONSE_STATUS_OK);
			setResponsePayload(buf.toString());
		} catch (SQLException ex) {
			log.error("Exception extracting results:", ex); //$NON-NLS-1$
			setResponseStatus(extractSqlError(ex));
			setResponsePayload(String.format("Exception converting result set: %s", ex)); //$NON-NLS-1$
		}
	}

	// expects strings like: "ORA-00942: table or view does not exist"
	// and tries to extract the "ORA-00942" part:
	private static String extractSqlError(SQLException t) {
		String errorMsg = t. getMessage();
		int pos1 = errorMsg.indexOf(": "); //$NON-NLS-1$
		if (pos1 > 0) {
			return errorMsg.substring(0, pos1);
		}
		return errorMsg;
	}

	private void deriveState() throws Exception {
		if (isCheckResults()) {
			if (getResultChecker() != null) {
				boolean resultCheck = getResultChecker().checkResult(this, getResponsePayload());
				log.trace("deriveState: {}", resultCheck); //$NON-NLS-1$
				setState(resultCheck ? State.OK : State.FAILED);
				return;
			} else {
				throw new IllegalArgumentException("resultChecker == null for checkResults == true"); //$NON-NLS-1$
			}
		}
		setState(State.OK); // if there is no check defined we consider any successful query-response as OK
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", dbUid:").append(this.dbUid) //$NON-NLS-1$
			.append(", dbPwd:").append(this.dbPwd) //$NON-NLS-1$
			.append(", query:").append(this.query) //$NON-NLS-1$
			.append(", resultingQuery:").append(this.resultingQuery) //$NON-NLS-1$
			.append(", driverName:").append(this.driverName) //$NON-NLS-1$
			.append(", responseStatus:").append(this.responseStatus) //$NON-NLS-1$
			.append(", responsePayload:(").append((this.responsePayload != null ? this.responsePayload.length() : 0)).append(" bytes)") // echoing the entire payload was flooding the logs //$NON-NLS-1$ //$NON-NLS-2$
			.append("}") //$NON-NLS-1$
			.toString();
	}
}
