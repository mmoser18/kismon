/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 3 Mar 2021
 */

package net.mmo.utils.kism.ui.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UIHandlerSupport
{
	ExecutorService executorService;

	@SuppressWarnings("deprecation")
	@Override
	public void finalize() {
		if (this.executorService != null) {
			this.executorService.shutdown();
		}
	}

	public <T extends Object> void executeLater(Supplier<T> longRunningActivity,
	                                            Consumer<T> uiActivity) {
		log.info("executeLater"); //$NON-NLS-1$
		if (this.executorService == null) {
			this.executorService = Executors.newFixedThreadPool(5);
		}
		this.executorService.submit(new Runnable()
			{
				@Override
				public void run () {
					T result = (longRunningActivity != null ? longRunningActivity.get() : null);
					if (uiActivity != null) doUIOperation(() -> uiActivity.accept(result));
				}
			});
	}

	public static void doUIOperation(Command cmd) {
		UI.getCurrent().access(cmd);
	}
}
