/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;

@Entity
@Setter
@Getter
@Slf4j
public class SSHConnection extends IPConnection
{
	private static final long serialVersionUID = 643208328101928988L;

	public final static String PROPERTYNAME_COMMAND      = "command"; //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE     = "response"; //$NON-NLS-1$

	protected int hostPort = 22; // default SSH port is 22

	protected String username;
	protected String password;

	protected String command;

	@JsonIgnore
	transient protected String response;

	@JsonIgnore
	transient private SSHClient client;
	@JsonIgnore
	transient private Session session;


	public SSHConnection() { // required for deserialization & node-factory
		super();
	}
	public SSHConnection(String name, String description) {
		super(name, description);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		if (this.session != null) {
			this.session.close();
			this.session = null;
		}
		if (this.client != null) {
			this.client.disconnect();
			this.client = null;
		}
		super.finalize();
	}

	@Override
	public void setResultingHostAddress(String resultingHostAddress) {
		if (!Objects.equal(this.getResultingHostAddress(), resultingHostAddress)) {
			super.setResultingHostAddress(resultingHostAddress);
			resetClient();
		}
	}

	public void setHostPort(int hostPort) {
		if (this.hostPort != hostPort) {
			this.hostPort = hostPort;
			resetClient();
		}
	}

	public void setUsername(String username) {
		if (!Objects.equal(this.username, username)) {
			this.username = username;
			resetClient();
		}
	}
	public void setPassword(String password) {
		if (!Objects.equal(this.password, password)) {
			this.password = password;
			resetClient();
		}
	}

	// trigger a recreation of the client and the session
	private void resetClient() {
		if (this.session != null) {
			try {
				this.session.close();
			} catch (TransportException | ConnectionException ex) {
				log.trace("Exception closing session", ex); //$NON-NLS-1$
			}
			this.session = null;
		}
		if (this.client != null) {
			try {
				this.client.disconnect();
			} catch (IOException ex) {
				log.trace("Exception closing client", ex); //$NON-NLS-1$
			}
			this.client = null;
		}
	}

	@Override
	public void sendRequest() throws Exception {
		log.info("ssh to '{}':", getResultingHostAddress()); //$NON-NLS-1$
		try {
			if (this.client == null) {
				this.client = new SSHClient();
				this.client.loadKnownHosts();
				this.client.connect(getResultingHostAddress(), getHostPort());
				this.client.authPassword(resolveProperties(getUsername()), resolveProperties(getPassword()));
			}
			if (this.session == null) {
				this.session = this.client.startSession();
			}
			final String resolvedCommand = resolveProperties(getCommand());
			// ... before executing the actual request:
			setTimestamp(LocalDateTime.now());
			long startTime = System.nanoTime();
			try (final Command sshCommand = this.session.exec(resolvedCommand)) {
				sshCommand.join(getTimeout(), TimeUnit.SECONDS);
				long callDuration = System.nanoTime() - startTime;
				setDuration(callDuration);
				log.info("responseReceived for '{}' after {} microsecs.", getName(), callDuration/1000); //$NON-NLS-1$
				String result = sshCommand.getExitErrorMessage();
				if (result != null) { // violent exit!
					setResponse(result);
					setState(State.FAILED);
					sshCommand.close();
					log.info("exit-status for '{}' (command:'{}') was: {}", getName(), resolvedCommand, sshCommand.getExitStatus()); //$NON-NLS-1$
				} else { // cmd went OK:
					try (InputStream is = sshCommand.getErrorStream()) {
						result = readStream(is);
					}
					if (result != null && !result.isEmpty()) { // we had some error:
						log.debug("error executing '{}' (command:'{}'): {}", getName(), resolvedCommand, result); //$NON-NLS-1$
						setState(State.DEGRADED);
					} else { // nothing on error stream:
						try (InputStream is = sshCommand.getInputStream()) {
							result = readStream(is);
						}
						log.debug("result executing '{}' (command:'{}'): {}", getName(), resolvedCommand, result); //$NON-NLS-1$
						setState(State.OK);
					}
				}
				setResponse(result);
				deriveState();
			} // cmd gets closed here.
		} catch (Exception ex) {
			log.error("Exception executing '{}' (creating client/session): {}", getName(), ex); //$NON-NLS-1$
			setDuration(-1);
			setResponse(ex.getMessage());
			setState(State.FAILED);
			resetClient(); // trigger a new client and session setup for the next attempt:
		} finally {
			// create history entry:
			final String resp = getResponse();
			setRequestResult(getState() + "/" + (resp.length() > 50 ? resp.substring(0, 50) + "..." : resp)); //$NON-NLS-1$ //$NON-NLS-2$
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, getResponse());
		}
	}

	String readStream(InputStream str) {
		return new BufferedReader(new InputStreamReader(str, StandardCharsets.UTF_8))
					.lines()
					.collect(Collectors.joining("\n")); //$NON-NLS-1$
	}

	private void deriveState() throws Exception {
		State res = State.OK;
		if (isCheckResults()) {
			if (getResultChecker() != null) {
				if (!getResultChecker().checkResult(this, getResponse())) {
					res = State.FAILED;
				}
			} else {
				throw new IllegalArgumentException("resultChecker == null for checkResults == true"); //$NON-NLS-1$
			}
		}
		log.debug("deriveState '{}': {}", getName(), res); //$NON-NLS-1$
		setState(res);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", hostPort:").append(this.hostPort) //$NON-NLS-1$
			.append(", username:").append(this.username) //$NON-NLS-1$
			.append(", password:").append(this.password) //$NON-NLS-1$
			.append(", command:").append(this.command) //$NON-NLS-1$
			.append(", response:").append(this.response) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}
}
