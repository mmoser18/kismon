/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility class to log the creation of new sessions and UIs
 */
@Component
public class ServiceListener implements VaadinServiceInitListener
{
	private static final long serialVersionUID = 4733705815409120789L;
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void serviceInit(ServiceInitEvent event) {

		VaadinService src = event.getSource();

		src.addSessionInitListener(initEvent -> this.log.info("A new Session has been initialized: {}", initEvent.getSession())); //$NON-NLS-1$
		src.addSessionDestroyListener(destroyEvent -> this.log.info("A Session has been destroyed: {}", destroyEvent.getSession()));  //$NON-NLS-1$

		src.addUIInitListener(initEvent -> this.log.info("A new UI has been initialized: {}", initEvent.getUI())); //$NON-NLS-1$

		src.addServiceDestroyListener(destroyEvent -> this.log.info("A Service has been destroyed: {}", destroyEvent.getSource())); //$NON-NLS-1$
	}
}