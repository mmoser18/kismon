/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.StringUtils;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@SuppressWarnings("javadoc")
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

	public enum Auth_Method {
		PWD, // user & pwd: authArg is a password
		KEY; // user & key: authArg is private key
	}
	protected Auth_Method authMethod = Auth_Method.PWD;
	protected String authArg; // for PWD: password, for KEY: private key

	protected String fingerprint; // if none is provided "PromiscuousVerifier" is used.

	protected String command;

	@JsonIgnore
	transient private SSHClient sshClient;

	@JsonIgnore
	protected String response;


	public SSHConnection() { // required for deserialization & node-factory
		super();
	}
	public SSHConnection(String name, String description) {
		super(name, description);
	}

	@SuppressWarnings("removal")
	@Override
	protected void finalize() throws Throwable {
		if (this.sshClient != null) {
			try {
				this.sshClient.disconnect();
			} catch (Exception ex) {
				// ignore
			}
			this.sshClient = null;
		}
		super.finalize();
	}

	@Override
	public void setResultingHostAddress(String resultingHostAddress) {
		if (!Objects.equals(this.getResultingHostAddress(), resultingHostAddress)) {
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
		if (!Objects.equals(this.username, username)) {
			this.username = username;
			resetClient();
		}
	}

	public void setPassword(String password) {
		if (this.authMethod != Auth_Method.PWD) {
			this.authMethod = Auth_Method.PWD;
			setAuthArg(password);
			resetClient();
		} else if (!Objects.equals(this.getAuthArg(), password)) {
			this.setAuthArg(password);
			resetClient();
		}
	}
	public void setPrivateKey(String privateKey) {
		if (this.authMethod != Auth_Method.KEY) {
			this.authMethod = Auth_Method.KEY;
			setAuthArg(privateKey);
			resetClient();
		} else if (!Objects.equals(this.getAuthArg(), privateKey)) {
			this.setAuthArg(privateKey);
			resetClient();
		}
	}

	// trigger a recreation of the sshClient and the session
	private void resetClient() {
		if (this.sshClient != null) {
			try {
				this.sshClient.disconnect();
			} catch (IOException ex) {
				log.trace("Exception closing sshClient", ex); //$NON-NLS-1$
			}
			this.sshClient = null;
		}
	}

	@Override
	public void sendRequest() throws Exception {
		log.debug("ssh to '{}':", getResultingHostAddress()); //$NON-NLS-1$
		try {
			if (this.sshClient == null) {
				log.trace("Creating SSHClient:"); //$NON-NLS-1$
				this.sshClient = new SSHClient();
				String resolvedFingerprint = resolveProperties(getFingerprint());
				log.debug("accaptable fingerprint is '{}'", resolvedFingerprint); //$NON-NLS-1$
				if (StringUtils.isEmpty(resolvedFingerprint)) {
					log.trace("using PromiscuousVerifier"); //$NON-NLS-1$
					this.sshClient.addHostKeyVerifier(new PromiscuousVerifier());
				} else {
					this.sshClient.addHostKeyVerifier(resolvedFingerprint);
				}
				this.sshClient.loadKnownHosts();
				this.sshClient.connect(getResultingHostAddress(), getHostPort());
				switch (this.authMethod) {
				case PWD:
					this.sshClient.authPassword(resolveProperties(getUsername()), resolveProperties(getAuthArg()));
					break;
				case KEY:
					this.sshClient.authPublickey(resolveProperties(getUsername()), resolveProperties(getAuthArg()));
					break;
				default:
					throw new IllegalArgumentException("Illegal Auth_Method: " + this.authMethod); //$NON-NLS-1$
				}
			}
			final String resolvedCommand = resolveProperties(getCommand());
			log.debug("{}: Executing command '{}':", getName(), resolvedCommand); //$NON-NLS-1$
			// ... before executing the actual request:
			setTimestamp(LocalDateTime.now());
			long startTime = System.nanoTime();
			try (Session session = this.sshClient.startSession();
				 Command sshCommand = session.exec(resolvedCommand)) {
				sshCommand.join(getTimeout(), TimeUnit.SECONDS);
				long callDuration = System.nanoTime() - startTime;
				setDuration(callDuration);
				log.debug("responseReceived for '{}' after {} microsecs.", getName(), callDuration/1000); //$NON-NLS-1$
				String result = sshCommand.getExitErrorMessage();
				if (result != null) { // violent exit!
					setResponse(result);
					setState(State.FAILED);
					sshCommand.close();
					log.debug("Exit-status for '{}' (command:'{}') was: {} / {}", //$NON-NLS-1$
					         getName(), resolvedCommand, sshCommand.getExitStatus(), result);
				} else { // cmd went OK:
					try (InputStream is = sshCommand.getErrorStream()) {
						result = readStream(is);
					}
					if (result != null && !result.isEmpty()) { // we had some error:
						log.debug("Error executing '{}' (command:'{}'): {}", //$NON-NLS-1$
						          getName(), resolvedCommand, result);
						setState(State.DEGRADED);
					} else { // nothing on error stream:
						setState(State.OK);
					}
					try (InputStream is = sshCommand.getInputStream()) {
						result = readStream(is);
					}
					log.debug("Result executing '{}' (command:'{}'): {}", //$NON-NLS-1$
					          getName(), resolvedCommand, result);
				}
				setResponse(result);
				deriveState();
			} // cmd gets closed here.
		} catch (Exception ex) {
			log.debug("Exception executing '{}' (creating ssh-client/-session/-command): {}", getName(), ex); //$NON-NLS-1$
			setDuration(-1);
			setResponse(ex.getMessage());
			setState(State.FAILED);
			resetClient(); // trigger a new sshClient and session setup for the next attempt:
		} finally {
			// create history entry:
			final String resp = getResponse();
			setRequestResult(getState() + "/" + (resp.length() > 50 ? resp.substring(0, 50) + "..." : resp)); //$NON-NLS-1$ //$NON-NLS-2$
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, getResponse());
		}
	}

	String readStream(InputStream str) throws IOException {
		return new String(str.readAllBytes(), StandardCharsets.UTF_8);
	}

	private void deriveState() throws Exception {
		State res = State.OK;
		if (isCheckResults()) {
			if (getResultChecker() != null) {
				res = getResultChecker().checkResult(this, getResponse());
			} else {
				throw new IllegalArgumentException("resultChecker == null for checkResults == true"); //$NON-NLS-1$
			}
		}
		log.trace("deriveState '{}': {}", getName(), res); //$NON-NLS-1$
		setState(res);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		buf.append(", hostPort:").append(this.hostPort) //$NON-NLS-1$
			.append(", username:").append(this.username) //$NON-NLS-1$
			.append(", authMethod:").append(this.authMethod) //$NON-NLS-1$
			;
			switch (this.authMethod) {
			case PWD:
				buf.append(", key:"); //$NON-NLS-1$
				break;
			case KEY:
				buf.append(", pubkey:"); //$NON-NLS-1$
				break;
			default:
				buf.append("<Illegal Auth_Method: " + this.authMethod + ">"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		buf.append(getAuthArg())
			.append(", command:").append(this.command) //$NON-NLS-1$
			.append(", response:").append(this.response) //$NON-NLS-1$
			.append("}"); //$NON-NLS-1$
		return buf.toString();
	}
}
