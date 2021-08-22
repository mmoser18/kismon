/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 3 Mar 2021
 */

package net.mmo.utils.kism.ui.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.Command;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UIHandlerSupport
{
	private static ExecutorService executorService = Executors.newFixedThreadPool(10);

	private final static boolean DO_ASYNC = true;
	private final static boolean RESERVE_UI_BEFORE_UPDATE = true;

	private UIHandlerSupport() {
		// should never be called...
	}

	public static <T extends Object> void executeLater(Component comp,
	                                                   Command longRunningActivity) {
		log.debug("executeLater({})", longRunningActivity); //$NON-NLS-1$
		executeLater(comp, () -> longRunningActivity, null);
	}

	public static <T extends Object> void executeLater(Component comp,
	                                                   long delay,  // in milliseconds
	                                                   Command uiActivity) {
		executeLater(comp, () -> {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException ex) {
					log.warn("thread was interrupted"); //$NON-NLS-1$
				}
				return null;
			},
			(dummy) -> uiActivity.execute());
	}

	public static <T extends Object> void executeLater(Component comp,
	                                                   Supplier<T> longRunningActivity,
	                                                   Consumer<T> uiActivity) {
		log.debug("executeLater({}, {})", longRunningActivity, uiActivity); //$NON-NLS-1$
		if (DO_ASYNC) { // offload the heavy lifting to a different thread:
			executorService.submit(new Runnable()
				{
					@Override
					public void run () {
						executeLaterInternal(comp, longRunningActivity, uiActivity);
					}
				});
		} else {
			executeLaterInternal(comp, longRunningActivity, uiActivity);
		}
	}

	protected static <T extends Object> void executeLaterInternal(Component comp,
	                                                              Supplier<T> longRunningActivity,
	                                                              Consumer<T> uiActivity) {
		final T result; // final to make the lambda happy...
		if (longRunningActivity != null) {
			log.debug("longRunningActivity start:"); //$NON-NLS-1$
			try {
				result = longRunningActivity.get();
				log.debug("longRunningActivity done."); //$NON-NLS-1$
			} catch (Exception ex) {
				log.error("longRunningActivity failed.", ex); //$NON-NLS-1$
				return;
			}
		} else {
			result = null;
		}
		if (uiActivity != null) {
			if (RESERVE_UI_BEFORE_UPDATE) {
				log.debug("obtaining GUI access:"); //$NON-NLS-1$
				comp.getUI().ifPresentOrElse((ui) -> ui.access(() -> {
				                            	log.debug("uiActivity start:"); //$NON-NLS-1$
				                            	uiActivity.accept(result);
				                            	log.debug("uiActivity done."); //$NON-NLS-1$
				                             }),
				                             () -> log.warn("unable to obtain UI-handle - output dropped")); //$NON-NLS-1$
			} else {
				log.debug("uiActivity start:"); //$NON-NLS-1$
				try {
					uiActivity.accept(result);
					log.debug("uiActivity done."); //$NON-NLS-1$
				} catch (Exception ex) {
					log.error("uiActivity failed.", ex); //$NON-NLS-1$
				}
			}
		}
	}
}
