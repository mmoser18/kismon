/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.flow.server.Command;
import lombok.extern.slf4j.Slf4j;

/**
 * utility class for the off-loading of activities that should not block the UI thread to a different thread
 */
@Slf4j
public class ThreadSupport
{
	static ExecutorService executorService = Executors.newFixedThreadPool(10);
	private ThreadSupport() {
		throw new RuntimeException("should never be called..."); //$NON-NLS-1$
	}

	/**
	 * @param delay
	 * @param activity
	 */
	public static void executeLater(long delay,  // in milliseconds
	                                Command activity) { // activity to be started (after delay)
		log.debug("executeLater({})", activity); //$NON-NLS-1$
		executorService.submit(new Runnable()
		{
			@Override
			public void run () {
				if (delay > 0) {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException ex) {
						log.warn("thread was interrupted"); //$NON-NLS-1$
					}
				}
				activity.execute();
			}
		});
	}

	/**
	 * @param <T>
	 * @param delay1 optional delay before the execution of activity1 [in milliseconds]
	 * @param activity1
	 * @param delay2 optional delay before the execution of activity2 [in milliseconds]
	 * @param activity2
	 */
	public static <T extends Object> void executeLater(long delay1,
	                                                   Supplier<T> activity1,
	                                                   long delay2,
	                                                   Consumer<T> activity2) {
		log.debug("executeLater({}, {})", activity1, activity2); //$NON-NLS-1$
		executeLater(delay1, () -> {
			final T result; // final to make the lambda happy...
			if (activity1 != null) {
				log.debug("activity1 start:"); //$NON-NLS-1$
				try {
					result = activity1.get();
					log.debug("activity1 done."); //$NON-NLS-1$
				} catch (Exception ex) {
					log.error("activity1 failed.", ex); //$NON-NLS-1$
					return;
				}
			} else {
				result = null;
			}
			if (activity2 != null) {
				executeLater(delay2, () -> {
					log.debug("activity2 start:"); //$NON-NLS-1$
					try {
						activity2.accept(result);
						log.debug("activity2 done."); //$NON-NLS-1$
					} catch (Exception ex) {
						log.error("activity2 failed.", ex); //$NON-NLS-1$
					}
				});
			}
		});
	}
}
