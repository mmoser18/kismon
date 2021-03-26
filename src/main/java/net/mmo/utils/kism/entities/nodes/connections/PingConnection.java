/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Setter
@Getter
@Slf4j
public class PingConnection extends IPConnection
{
	private static final long serialVersionUID = 643208328101928988L;

	boolean isWindows;
	{
		this.isWindows = System.getProperty("os.name").toLowerCase().contains("win"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	transient ProcessBuilder processBuilder;


	public PingConnection() { // required for deserialization and NodeFactory
		super();
	}
	public PingConnection(String name, String description) {
		super(name, description);
	}

	@Override
	public void setResultingHostAddress(String resultingHostAddress) {
		super.setResultingHostAddress(resultingHostAddress);
		setProcessBuilder(null); // trigger a recreation of the processBuilder
	}

	@Override
	public void setTimeout(int timeout) {
		super.setTimeout(timeout);
		setProcessBuilder(null); // trigger a recreation of the processBuilder
	}

	@Override
	public void sendRequest() throws Exception {
		if (this.processBuilder == null) {
			this.processBuilder =
				new ProcessBuilder("ping", //$NON-NLS-1$
				                   (this.isWindows ? "-n" : "-c"), "1", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				                   "-w", Integer.toString(getTimeout() * 1000), // timeout in milliseconds //$NON-NLS-1$
				                   resultingHostAddressResolved());
		}
		// take timestamp:
		setTimestamp(LocalDateTime.now());
		// ... right before executing the actual request:
		long startTime = System.nanoTime();
		Process proc = this.processBuilder.start();
		if (proc.waitFor(getTimeout()+3, TimeUnit.SECONDS)) {
			long callDuration = System.nanoTime() - startTime;
			log.info("ping to '{}' responded in {} msecs", getResultingHostAddress(), callDuration / 1e6); //$NON-NLS-1$
			setState(State.OK);
			setDuration(callDuration);
			setRequestResult(getState().name() + '/' + (callDuration / 1e6)/*=nano-secs to milli-secs*/);
		} else {
			log.info("ping to '{}' timed out.", getResultingHostAddress()); //$NON-NLS-1$
			setState(State.FAILED);
			setDuration(-1);
			setRequestResult(getState().name() + "/timed out"); //$NON-NLS-1$
		}
	}
}
