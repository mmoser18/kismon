/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 3 Mar 2021
 */

package net.mmo.utils.kism.ui.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
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
		throw new RuntimeException("should never be called..."); //$NON-NLS-1$
	}

	public static void executeLater(Component comp,
	                                long delay,  // in milliseconds
	                                Command uiActivity) { // ui-activity to be started after delay
		executeLater(comp,
		             () -> { // create dummy Supplier
		            	 if (delay > 0) {
		            		 try {
		            			 Thread.sleep(delay);
		            		 } catch (InterruptedException ex) {
		            			 log.warn("thread was interrupted"); //$NON-NLS-1$
		            		 }
		            	 }
		            	 return null;
		             },
		             (__) -> uiActivity.execute()); // map Command to Consumer
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

	// Same as above but with an argument to pass:

	public static <O extends Object> void executeLater(Component comp,
	                                                   long delay,  // in milliseconds
	                                                   O obj, // Object to be passed to uiActivity
	                                                   Consumer<O> uiActivity) { // ui-activity to be started after delay
		executeLater(comp,
		             obj,
		             (__) -> { // create dummy Function
		            	 if (delay > 0) {
		            		 try {
		            			 Thread.sleep(delay);
		            		 } catch (InterruptedException ex) {
		            			 log.warn("thread was interrupted"); //$NON-NLS-1$
		            		 }
		            	 }
		            	 return null;
		             },
		             (_1_, _2_) -> uiActivity.accept(obj)); // map Consumer to BiConsumer
	}

	public static <O extends Object, T extends Object> void executeLater(Component comp,
	                                                   O obj, // Object to be passed to longRunningActivity and uiActivity
	                                                   Function<O, T> longRunningActivity,
	                                                   BiConsumer<O, T> uiActivity) {
		log.debug("executeLater({}, {})", longRunningActivity, uiActivity); //$NON-NLS-1$
		if (DO_ASYNC) { // offload the heavy lifting to a different thread:
			executorService.submit(new Runnable()
				{
					@Override
					public void run () {
						executeLaterInternal(comp, obj, longRunningActivity, uiActivity);
					}
				});
		} else {
			executeLaterInternal(comp, obj, longRunningActivity, uiActivity);
		}
	}

	protected static <O extends Object, T extends Object> void executeLaterInternal(Component comp,
	                                                                                O obj, // Object to be passed to longRunningActivity and uiActivity
	                                                                                Function<O, T> longRunningActivity,
	                                                                                BiConsumer<O, T> uiActivity) {
		final T result; // final to make the lambda happy...
		if (longRunningActivity != null) {
			log.debug("longRunningActivity start:"); //$NON-NLS-1$
			try {
				result = longRunningActivity.apply(obj);
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
				                            	uiActivity.accept(obj, result);
				                            	log.debug("uiActivity done."); //$NON-NLS-1$
				                             }),
				                             () -> log.warn("unable to obtain UI-handle - output dropped")); //$NON-NLS-1$
			} else {
				log.debug("uiActivity start:"); //$NON-NLS-1$
				try {
					uiActivity.accept(obj, result);
					log.debug("uiActivity done."); //$NON-NLS-1$
				} catch (Exception ex) {
					log.error("uiActivity failed.", ex); //$NON-NLS-1$
				}
			}
		}
	}
}
