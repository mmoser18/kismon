/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.ui.views.nodes;

import java.util.Hashtable;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.IntermediateNode;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.RootNode;
import net.mmo.utils.kism.entities.nodes.connections.JDBCConnection;
import net.mmo.utils.kism.entities.nodes.connections.PingConnection;
import net.mmo.utils.kism.entities.nodes.connections.RESTConnection;
import net.mmo.utils.kism.entities.nodes.connections.SOAPConnection;
import net.mmo.utils.kism.entities.nodes.connections.SSHConnection;
import net.mmo.utils.kism.ui.views.nodes.connection_forms.JDBCConnectionForm;
import net.mmo.utils.kism.ui.views.nodes.connection_forms.PingConnectionForm;
import net.mmo.utils.kism.ui.views.nodes.connection_forms.RESTConnectionForm;
import net.mmo.utils.kism.ui.views.nodes.connection_forms.SOAPConnectionForm;
import net.mmo.utils.kism.ui.views.nodes.connection_forms.SSHConnectionForm;
import org.springframework.stereotype.Component;

@SuppressWarnings("javadoc")
@Component
@Slf4j
/**
 * Note that this class is not really a UI component but was made one so that its fields are
 * session-local, i.e. each session has its own factory (and specifically its own form cache).
 */
public class FormFactory <N extends Node> extends VerticalLayout
{
	private static final long serialVersionUID = 7234214483184623150L;

	/**
	 * Here we cache the forms - one per nodeType. Since we don't have that many
	 * form types there is no cleanup nor eviction
	 */
	Hashtable<Class<N>, NodeForm<N>> forms = new Hashtable<>();

	@SuppressWarnings("unchecked")
	/**
	 * @Cacheable("forms") // tried using Spring cache here but didn't work. And as a sideeffect
	 * it caused dead slow application startup, so back to a good ol' Hashtable...
	 */
	NodeForm<N> createForm(Class<N> clazz, NodeService nodeService) {
		NodeForm<N> form = this.forms.get(clazz);
		if (form == null) {
			log.info("creating new form for node-class {}:", clazz); //$NON-NLS-1$
			if (clazz == RootNode.class) {
				form = (NodeForm<N>)new RootNodeForm();
			} else if (clazz == IntermediateNode.class) {
				form = (NodeForm<N>)new IntermediateNodeForm();
			} else if (clazz == RESTConnection.class) {
				form = (NodeForm<N>)new RESTConnectionForm();
			} else if (clazz == SOAPConnection.class) {
				form = (NodeForm<N>)new SOAPConnectionForm();
			} else if (clazz == JDBCConnection.class) {
				form = (NodeForm<N>)new JDBCConnectionForm();
			} else if (clazz == PingConnection.class) {
				form = (NodeForm<N>)new PingConnectionForm();
			} else if (clazz == SSHConnection.class) {
				form = (NodeForm<N>)new SSHConnectionForm();
			} else {
				throw new IllegalArgumentException("No form defined (yet) for node " + clazz); //$NON-NLS-1$
			}
			form.init(nodeService);
			this.forms.put(clazz, form); // cache it
			log.info("created new form for class {}: {}", clazz, form); //$NON-NLS-1$
		} else {
			log.info("found existing form for {}: {}", clazz, form); //$NON-NLS-1$
			// form.getElement().removeFromTree(); // make sure this is not attached to any old tree anymore.
		}
		form.initFlags(); // these may have changed (e.g. by a logout/login with a different uid)
		return form;
	}
}
