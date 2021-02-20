/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes;

import java.util.Hashtable;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.IntermediateNode;
import net.mmo.utils.kism.entities.nodes.Node;
import net.mmo.utils.kism.entities.nodes.RootNode;
import net.mmo.utils.kism.entities.nodes.connections.JDBCConnection;
import net.mmo.utils.kism.entities.nodes.connections.RESTConnection;
import net.mmo.utils.kism.entities.nodes.connections.SOAPConnection;
import net.mmo.utils.kism.ui.view.nodes.connection_forms.JDBCConnectionForm;
import net.mmo.utils.kism.ui.view.nodes.connection_forms.RESTConnectionForm;
import net.mmo.utils.kism.ui.view.nodes.connection_forms.SOAPConnectionForm;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FormFactory <N extends Node>
{
//	// Here we cache the forms - one per nodeType. Since we don't have that many form types there is no cleanup nor eviction
	Hashtable<Class<N>, NodeForm<N>> forms = new Hashtable<>();

	@SuppressWarnings("unchecked")
	// @Cacheable("forms") // tried using Spring cache here but didn't work. And as a sideeffect
	// it caused darn slow application startup, so back to a good ol' Hashtable...
	NodeForm<N> createForm(Class<N> clazz, NodeService nodeService) {
		NodeForm<N> form = this.forms.get(clazz);
		if (form == null) {
			log.info("creating new form for node-class {}:", clazz); //$NON-NLS-1$
			if (clazz == RootNode.class) {
				form = (NodeForm<N>)new RootNodeForm(nodeService);
			} else if (clazz == IntermediateNode.class) {
				form = (NodeForm<N>)new IntermediateNodeForm(nodeService);
			} else if (clazz == RESTConnection.class) {
				form = (NodeForm<N>)new RESTConnectionForm(nodeService);
			} else if (clazz == SOAPConnection.class) {
				form = (NodeForm<N>)new SOAPConnectionForm(nodeService);
			} else if (clazz == JDBCConnection.class) {
				form = (NodeForm<N>)new JDBCConnectionForm(nodeService);
			} else {
				throw new IllegalArgumentException("No form defined (yet) for node class " + clazz); //$NON-NLS-1$
			}
			this.forms.put(clazz, form); // cache it
			log.info("created new form for class {}: {}", clazz, form); //$NON-NLS-1$
		} else {
			log.info("found existing form for class {}: {}", clazz, form); //$NON-NLS-1$
		}
		return form;
	}
}
