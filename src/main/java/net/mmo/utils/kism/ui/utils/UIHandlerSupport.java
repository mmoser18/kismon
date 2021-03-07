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
	private static ExecutorService executorService = Executors.newFixedThreadPool(10);

	private final static boolean DO_SYNC = true;
	private final static boolean RESERVE_UI_BEFORE_UPDATE = true;

	private UIHandlerSupport() {
		// should bever be called...
	}

	public static <T extends Object> void executeLater(Command longRunningActivity) {
		log.info("executeLater({})", longRunningActivity); //$NON-NLS-1$
		executeLater(() -> longRunningActivity, null);
	}

	public static <T extends Object> void executeLater(Supplier<T> longRunningActivity,
	                                                   Consumer<T> uiActivity) {
		log.info("executeLater({}, {})", longRunningActivity, uiActivity); //$NON-NLS-1$
		if (DO_SYNC) {
			executeLaterInternal(longRunningActivity, uiActivity);
		} else { // offload the heavy lifting to a different thread:
			executorService.submit(new Runnable()
				{
					@Override
					public void run () {
						executeLaterInternal(longRunningActivity, uiActivity);
					}
				});
		}
	}

	public static <T extends Object> void executeLaterInternal(Supplier<T> longRunningActivity,
	                                                           Consumer<T> uiActivity) {
		final T result; // final to make the lambda happy...
		if (longRunningActivity != null) {
			log.info("longRunningActivity:"); //$NON-NLS-1$
			result = longRunningActivity.get();
		} else {
			result = null;
		}
		if (uiActivity != null) {
			log.info("uiActivity:"); //$NON-NLS-1$
			if (RESERVE_UI_BEFORE_UPDATE) {
				UI.getCurrent().access(() -> uiActivity.accept(result));
			} else {
				uiActivity.accept(result);
			}
		}
	}
}
