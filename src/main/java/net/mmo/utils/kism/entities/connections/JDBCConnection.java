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

	public static final String PROPERTYNAME_DB_RESPONSE = "dbResponse"; //$NON-NLS-1$
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

	protected boolean checkResult = false;
	transient protected String dbResponse;


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

	public void setDbResponse(String dbResponse) {
		if (!Objects.equal(this.dbResponse, dbResponse)) {
			String oldDbResponse = this.dbResponse;
			this.dbResponse = dbResponse;
			informOnPropertyChange(PROPERTYNAME_DB_RESPONSE, oldDbResponse, dbResponse);
		}
	}

	@SuppressWarnings("deprecation")
	private void registerDriver() throws Exception {
		log.info("registerDriver:"); //$NON-NLS-1$
		this.driver = (Driver)Class.forName(this.driverName).newInstance();
		DriverManager.registerDriver(this.driver);
	}

	private void createConnection() throws Exception {
		log.info("createConnection:"); //$NON-NLS-1$
		if (this.connection == null || !this.connection.isValid(10)) {
			this.connection = DriverManager.getConnection(this.url, this.uid, this.pwd);
		}
	}

	@SuppressWarnings("resource")
	@Override
	public void sendRequest() throws Exception {
		log.info("sendRequest:"); //$NON-NLS-1$
		Statement statement = null;
		try {
			if (this.driver == null) {
				registerDriver();
			}
			createConnection();
			log.info("createStatement:"); //$NON-NLS-1$
			statement = getConnection().createStatement();
//			if (IPConnection.syncRequests) {
				log.info("statement.execute():", getQuery()); //$NON-NLS-1$
				long startTime = System.nanoTime();
				if (statement.execute(getQuery())) {
					responseReceived(System.nanoTime() - startTime, statement);
				}
//			} else { // async:
//				long startTime = System.nanoTime();
//				CompletableFuture<HttpResponse<byte[]>> futureResponse = statement.execute(this.query);
//								this.client.sendAsync(this.request, BodyHandlers.ofByteArray());
//				futureResponse.thenAccept(response ->
//				{
//					responseReceived(System.nanoTime() - startTime, response);
//				});
//			}
		} catch (Throwable t) {
			if (this.connection != null) {
				throw new Exception(String.format("Error sending request to {}: ", this.connection), t); //$NON-NLS-1$
			} else {
				throw new Exception("Error creating request", t); //$NON-NLS-1$
			}
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException ex) {
					log.error("Error closing statement: ", ex); //$NON-NLS-1$
				}
			}
		}
	}

	private void responseReceived(long callDuration, Statement statement) {
		log.info("responseReceived after {} nanosecs.", callDuration); //$NON-NLS-1$
		setDuration(callDuration);
		setDbResponse(convertResultSet(statement));

		logResponseValues();
		deriveState();
		informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());
	}

	private static String convertResultSet(Statement statement) {
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
			return buf.toString();
		} catch (SQLException ex) {
			log.error("Exception extracting results:", ex); //$NON-NLS-1$
			return String.format(Messages.getString("JDBCConnectionForm.Error.ConvertingResultSet"), ex); //$NON-NLS-1$
		}
	}

	public void logResponseValues() {
//		log.info("response: status={} / payload-length={}\nheaders:{}\nbody (assuming UTF-8): \"{}\"", //$NON-NLS-1$
//		         this.responseStatusCode, this.responseBody.length, this.responseHeaders, this.getResponseBodyAsString());
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
			.append(", dbResponse:").append(this.dbResponse) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}
}
