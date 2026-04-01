/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.aura.Aura;

@SuppressWarnings("javadoc")
@PWA(
 	name = CommonConstants.ApplicationFullName,
 	shortName = CommonConstants.ApplicationShortName,
 	offlineResources = {
 		"./styles/offline.css",
 		"./images/offline.png"
 	}
 )
@StyleSheet(Aura.STYLESHEET)
@Push
public class AppShellConfig implements AppShellConfigurator
{
	private static final long serialVersionUID = -1154876404764354514L;
}
