/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 16.12.2020
 */

package net.mmo.utils.kism.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;

@SuppressWarnings("javadoc")
@PWA(
 	name = CommonConstants.ApplicationFullName,
 	shortName = CommonConstants.ApplicationShortName,
 	offlineResources = {
 		"./styles/offline.css",
 		"./images/offline.png"
 	}
 )
@Push
public class AppShellConfig implements AppShellConfigurator
{
	private static final long serialVersionUID = -1154876404764354514L;
}
