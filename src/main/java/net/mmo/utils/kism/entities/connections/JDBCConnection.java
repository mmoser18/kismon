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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.Messages;

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

	protected String uid;
	protected String pwd;

	protected String query;

	protected String resultingQuery;

	protected String driverName = "oracle.jdbc.driver.OracleDriver"; //$NON-NLS-1$

	@JsonIgnore
	transient protected Driver driver;
	@JsonIgnore
	transient protected Connection connection;

	@JsonIgnore
	transient protected String responseStatus;
	@JsonIgnore
	transient protected String responsePayload;

	@JsonIgnore
	transient protected ExecutorService interactiveExecutor;
	@JsonIgnore
	transient protected ExecutorService periodicExecutor;

	@SuppressWarnings("unused")
	private JDBCConnection() { // required for deserialization
		super();

		if (!syncRequests) {
			this.interactiveExecutor = Executors.newSingleThreadExecutor();
		}
		this.periodicExecutor = Executors.newSingleThreadScheduledExecutor();
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
		log.info("registerDriver:"); //$NON-NLS-1$
		this.driver = (Driver)Class.forName(this.driverName).newInstance();
		DriverManager.registerDriver(this.driver);
	}

	private Connection createConnection() throws Exception {
		log.info("createConnection:"); //$NON-NLS-1$
		if (this.driver == null) {
			registerDriver();
		}
		if (this.connection == null || this.connection.isClosed() || !this.connection.isValid(15)) {
			this.connection = DriverManager.getConnection(this.url, this.uid, this.pwd);
		}
		return this.connection;
	}

	@SuppressWarnings("resource")
	@Override
	public void sendRequest() throws Exception {
		log.info("sendRequest:"); //$NON-NLS-1$
		if (IPConnection.syncRequests) {
			sendRequestInternal();
		} else {
			this.interactiveExecutor.execute(() -> {
				try {
					sendRequestInternal();
				} catch (Exception ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				}
			});
		}
	}
	public void sendRequestInternal() throws Exception {
		String resQuery = getResultingQuery();
		log.info("createStatement:"); //$NON-NLS-1$
		try (Connection conn = createConnection();
			 Statement statement = conn.createStatement()) {
			long startTime = System.nanoTime();
			this.timestamp = LocalDateTime.now();
			log.info("statement.execute():", resQuery); //$NON-NLS-1$
			boolean isRS = statement.execute(resQuery);
			long callDuration = System.nanoTime() - startTime;
			responseReceived(callDuration, statement, isRS);
		} catch (SQLException t) {
			setResponseStatus(extractSqlError(t));
			setResponsePayload(String.format("Exception executing query '%s': %s", resQuery, t.getMessage())); //$NON-NLS-1$
			deriveState();
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());
			if (this.connection != null) {
				throw new Exception(String.format("Error sending request '%s' to %s: ", resQuery, this.connection), t); //$NON-NLS-1$
			} else {
				throw new Exception("Error creating request", t); //$NON-NLS-1$
			}
		}
	}

	private void responseReceived(long callDuration, Statement statement, boolean isRS) throws SQLException {
		log.info("responseReceived after {} nanosecs.", callDuration); //$NON-NLS-1$
		setDuration(callDuration);
		if (isRS) { // result is a ResultSet
			convertResultSet(statement);
//			log.info("response: status={} / payload-length={}\nheaders:{}\nbody (assuming UTF-8): \"{}\"", //$NON-NLS-1$
//	         this.responseStatusCode, this.responseBody.length, this.responseHeaders, this.getResponseBodyAsString());
			deriveState();
		} else { // result is not a ResultSet but an update count or there are no results
			int updates = statement.getUpdateCount();
			log.info("Result: #updates:{}", updates); //$NON-NLS-1$
			State res = updates >= 0 ? State.OK : State.FAILED;
			setResponseStatus(res.name());
			setResponsePayload(res == State.OK
							   ? String.format(Messages.getString("JDBCConnection.Result.Updates"), updates) //$NON-NLS-1$
							   : String.format(Messages.getString("JDBCConnection.Result.NoResult"))); //$NON-NLS-1$
			this.setState(res);
		}
		informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());
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

	private void deriveState() {
		State res = State.OK;
		if (this.checkResult) {
//			String acceptableCodes = (this.acceptableReturnCodes != null ? this.acceptableReturnCodes : "200"); //$NON-NLS-1$
//			String responseStatusCodeStr = Integer.toString(this.responseStatusCode);
//			res = State.FAILED;
//			for (String acceptable: acceptableCodes.split("[,\\s]")) { //$NON-NLS-1$
//				if (acceptable.equals(responseStatusCodeStr)) {
//					res = State.OK;
//					break;
//				}
//			}
		}
		setState(res);
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
