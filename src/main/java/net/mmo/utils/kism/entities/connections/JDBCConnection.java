/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ExceptionUtils;
import net.mmo.utils.kism.utils.NodeProperties;

@Setter
@Getter
@Slf4j
public class JDBCConnection extends IPConnection
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

	protected String uid;
	protected String pwd;

	protected String query;

	protected String driverName =
		AppProperties.getProperties().getProperty("JDBC.DriverName", //$NON-NLS-1$
		                                          "oracle.jdbc.driver.OracleDriver"); //$NON-NLS-1$

	// transient stuff:

	@JsonIgnore
	transient protected Driver driver;
	@JsonIgnore
	transient protected Connection connection;

	// result stuff - later to be collected into some "history"-object:

	@JsonIgnore
	transient protected String resultingQuery;
	@JsonIgnore
	transient protected String responseStatus;
	@JsonIgnore
	transient protected String responsePayload;


	@SuppressWarnings("unused")
	private JDBCConnection() { // required for deserialization
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
	public void setProperties(NodeProperties properties) {
		super.setProperties(properties);
		updateResultingQuery();
	}

	protected void setQuery(String query) {
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
			informOnPropertyChange(PROPERTYNAME_RESPONSE_PAYLOAD, oldResponseStatus, responseStatus);
		}
	}
	public void setResponsePayload(String responsePayload) {
		if (!Objects.equal(this.responsePayload, responsePayload)) {
			String oldResponsePayload = this.responsePayload;
			this.responsePayload = responsePayload;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_PAYLOAD, oldResponsePayload, responsePayload);
		}
	}

	@SuppressWarnings("deprecation")
	private void registerDriver() throws Exception {
		log.debug("registerDriver:"); //$NON-NLS-1$
		this.driver = (Driver)Class.forName(this.driverName).newInstance();
		DriverManager.registerDriver(this.driver);
	}

	private void createConnection() throws Exception {
		log.debug("createConnection:"); //$NON-NLS-1$
		if (this.driver == null) {
			registerDriver();
		}
		if (this.connection == null || this.connection.isClosed() || !this.connection.isValid(15)) {
			this.connection = DriverManager.getConnection(resultingUrlResolved(), getUid(), getPwd());
		}
	}

	@Override
	public void sendRequest() throws Exception {
		log.info("sendRequest {}:", getName()); //$NON-NLS-1$
		Statement statement = null;
		setTimestamp(LocalDateTime.now()); // must be BEFORE createConnection so that we get a timestamp in the case this fails
		String resQuery = resultingQueryResolved();
		try {
			createConnection();
			statement = this.connection.createStatement();
			log.debug("statement.execute():", resQuery); //$NON-NLS-1$
			long startTime = System.nanoTime();
			boolean isRS = statement.execute(resQuery);
			long callDuration = System.nanoTime() - startTime;
			setDuration(callDuration);
			log.info("responseReceived for '{}' after {} µsecs.", getName(), callDuration/1000); //$NON-NLS-1$
			responseReceived(statement, isRS);
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());
		} catch (Exception ex) {
			setResponseStatus(ex instanceof SQLException ? extractSqlError((SQLException)ex) : ex.getMessage());
			setResponsePayload(String.format("Exception executing '{}' (query '%s'): %s", //$NON-NLS-1$
			                                 getName(), resQuery, ExceptionUtils.exceptionCauseSummary(ex)));
			setState(State.FAILED);
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());
			if (this.connection != null) {
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
			log.debug("response: status={} / payload=\"{}\"", //$NON-NLS-1$
			         getResponseStatus(), getResponsePayload());
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
		if (this.checkResults) {
			if (this.resultChecker != null) {
				setState(this.resultChecker.checkResult(this,getResponsePayload()) ? State.OK : State.FAILED);
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
			.append(", uid:").append(this.uid) //$NON-NLS-1$
			.append(", pwd:").append(this.pwd) //$NON-NLS-1$
			.append(", query:").append(this.query) //$NON-NLS-1$
			.append(", resultingQuery:").append(this.resultingQuery) //$NON-NLS-1$
			.append(", driverName:").append(this.driverName) //$NON-NLS-1$
			.append(", responseStatus:").append(this.responseStatus) //$NON-NLS-1$
			.append(", responsePayload:").append(this.responsePayload) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}
}
