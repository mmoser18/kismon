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

@Entity
@Setter
@Getter
public class PingConnection extends IPConnection
{
	private static final long serialVersionUID = 643208328101928988L;

	boolean isWindows;
	{
		this.isWindows = System.getProperty("os.name").toLowerCase().contains("win"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@SuppressWarnings("unused")
	private PingConnection() { // required for deserialization
		super();
	}
	public PingConnection(String name, String description) {
		super(name, description);
	}

	@Override
	public void sendRequest() throws Exception {
		ProcessBuilder processBuilder = new ProcessBuilder("ping", this.isWindows ? "-n" : "-c", "1", getHostAddress()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		setTimestamp(LocalDateTime.now());
		// ... before executing the actual request:
		long startTime = System.nanoTime();
		Process proc = processBuilder.start();
		if (proc.waitFor(1000, TimeUnit.MILLISECONDS)) {
			setState(State.OK);
			long callDuration = System.nanoTime() - startTime;
			setDuration(callDuration);
			setRequestResult(getState().name() + '/' + callDuration);
			} else {
			setDuration(-1);
			setState(State.FAILED);
			setRequestResult(getState().name() + "/---"); //$NON-NLS-1$
		}
	}
}
