/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ResultChecker;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.ExceptionUtils;

@SuppressWarnings("javadoc")
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
	transient protected JDBCHandling handling;

	// result stuff - later to be collected into some "history"-object:

	@JsonIgnore
	transient protected String resultingQuery;
	@JsonIgnore
	protected String responseStatus;
	@JsonIgnore
	protected String responsePayload;


	public JDBCConnection() { // required for deserialization & node-factory
		super();
	}

	public JDBCConnection(String name, String description) {
		super(name, description);
	}

	@SuppressWarnings("removal")
	@Override
	protected void finalize() throws Throwable {
		if (this.handling != null) {
			this.handling.close();
			this.handling = null;
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

	public void setDriverName(String driverName) {
		this.driverName = driverName;
		resetConnection(); // trigger a recreation of the connection if the driverName changes
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
		if (!Objects.equals(this.responseStatus, responseStatus)) {
			String oldResponseStatus = this.responseStatus;
			this.responseStatus = responseStatus;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_STATUS, oldResponseStatus, responseStatus);
		}
	}
	public void setResponsePayload(String responsePayload) {
		if (!Objects.equals(this.responsePayload, responsePayload)) {
			String oldResponsePayload = this.responsePayload;
			this.responsePayload = responsePayload;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_PAYLOAD, oldResponsePayload, responsePayload);
		}
	}

	@SuppressWarnings("resource")
	private JDBCHandling createConnection() throws Exception {
		log.debug("createConnection:"); //$NON-NLS-1$
		if (this.handling == null) {
			this.handling = new JDBCHandling();
		}
		this.handling.createConnection(resolveProperties(this.driverName), resultingUrlResolved(),
		                               resolveProperties(this.dbUid), resolveProperties(this.dbPwd),
		                               this.timeout);
		return this.handling;
	}

	private void resetConnection() {
		if (this.handling != null) {
			this.handling.resetConnection(); // trigger a recreation of the connection if the dbUid changes
			this.handling = null;
		}
	}

	@Override
	public void sendRequest() throws Exception {
		log.debug("sendRequest {}:", getName()); //$NON-NLS-1$
		String resQuery = resultingQueryResolved();
		JDBCHandling connection = null;
		Object res = null;
		setTimestamp(LocalDateTime.now()); // must be BEFORE createConnection so that we get a timestamp in the case this fails
		try {
			connection = createConnection();
			setResponseStatus("<waiting>"); //$NON-NLS-1$
			setResponsePayload("<no response received (yet>"); //$NON-NLS-1$
			long startTime = System.nanoTime();
			res = connection.executeQuery(this.timeout, resQuery);
			long callDuration = System.nanoTime() - startTime;
			setDuration(callDuration);
			log.trace("responseReceived for '{}' after {} microsecs.", getName(), callDuration/1000); //$NON-NLS-1$
			responseReceived(res);
			setRequestResult(getState().name() + '/' + getResponseStatus());
		} catch (Exception ex) {
			log.debug("exception executing '{}': {}", getName(), ex.getMessage()); //$NON-NLS-1$
			setResponseStatus(ex instanceof SQLException ? extractSqlError((SQLException)ex) : ex.getMessage());
			setResponsePayload(String.format("Exception executing '%s' (query '%s'): %s", //$NON-NLS-1$
			                                 getName(), resQuery, ExceptionUtils.exceptionCauseSummary(ex)));
			setDuration(NO_RESPONSE_DURATION); // signals an exception
			setState(State.FAILED);
			setRequestResult(getState().name() + '/' + getResponseStatus());
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());
			if (connection != null) {
				throw new Exception(String.format("sending query '%s' to %s: ", resQuery, getResultingUrl()), ex); //$NON-NLS-1$
			} else {
				throw new Exception(String.format("creating query to '%s'", getResultingUrl()), ex); //$NON-NLS-1$
			}
		} finally {
			// As I had to learn keeping our DB connection open between checks can keep that resource busy
			// and interfere with other usages of the DB (e.g. when the DB is to be dropped before a
			// DB-dump to be imported).
			// Since monitoring should never adversely interfere with the normal usage of a monitored
			// system we need to close our connection again right after each check. Less efficient but
			// necessary...
			if (res != null) {
				JDBCHandling.closeResultSet(res);
			}
			if (connection != null) {
				connection.close();
			}
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());
		}
	}

	private void responseReceived(Object res) throws Exception {
		if (res instanceof ResultSet) {
			convertResultSet((ResultSet)res);
			log.debug("response: status={} / payload={} bytes", //$NON-NLS-1$
			         getResponseStatus(), getResponsePayload().length());
			log.trace("payload=\"{}\"", //$NON-NLS-1$
				      getResponsePayload());
			deriveState();
		} else { // result is not a ResultSet but an update count or there are no results
			int updates = (Integer)res;
			log.debug("Result: #updates:{}", updates); //$NON-NLS-1$
			State state = updates >= 0 ? State.OK : State.FAILED;
			setResponseStatus(state.name());
			setResponsePayload(state == State.OK
							   ? String.format(RESULTSET_NR_RESULTS, updates)
							   : String.format(RESULTSET_NO_RESULTS));
			setState(state);
		}
	}

	private void convertResultSet(ResultSet rs) {
		try {
			String[] headers = JDBCHandling.getColumnHeaders(rs);
			StringBuilder buf = new StringBuilder();
			buf.append(String.join(RESULTSET_COL_SEPARATOR,headers));
			buf.append(RESULTSET_ROW_SEPARATOR);
			// add a separator line
			for (int i = 0; i < headers.length; i++) {
				if (i > 0) buf.append(RESULTSET_COL_SEPARATOR);
				for (int j = 0; j < headers[i].length(); j++) buf.append('-');
			}
			// for each row: add all the values:
			for (String[] line: JDBCHandling.getColumnValues(rs)) {
				buf.append(RESULTSET_ROW_SEPARATOR);
				buf.append(String.join(RESULTSET_COL_SEPARATOR,line));
			}
			setResponsePayload(buf.toString());

			setResponseStatus(RESPONSE_STATUS_OK);
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
		State checkResult = State.OK; // if there is no check defined we consider any successful query-response as OK
		if (isCheckResults()) {
			ResultChecker checker = getResultChecker();
			if (checker != null && checker.getCondition() != null) {
				checkResult = checker.checkResult(this, getResponsePayload());
			} else {
				log.debug("no result check defined."); //$NON-NLS-1$
			}
		}
		setState(checkResult);
		log.trace("deriveState: {}", checkResult); //$NON-NLS-1$
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
