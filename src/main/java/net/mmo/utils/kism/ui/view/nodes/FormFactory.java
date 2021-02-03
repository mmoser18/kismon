/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.IntermediateNode;
import net.mmo.utils.kism.entities.Node;
import net.mmo.utils.kism.entities.RootNode;
import net.mmo.utils.kism.entities.connections.JDBCConnection;
import net.mmo.utils.kism.entities.connections.RESTConnection;
import net.mmo.utils.kism.entities.connections.SOAPConnection;
import net.mmo.utils.kism.ui.view.nodes.connection_forms.JDBCConnectionForm;
import net.mmo.utils.kism.ui.view.nodes.connection_forms.RESTConnectionForm;
import net.mmo.utils.kism.ui.view.nodes.connection_forms.SOAPConnectionForm;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FormFactory <N extends Node>
{
//	// Here we cache the forms - one per type. Since we don't have that many form types there is no cleanup nor eviction
//	Hashtable<Class<N>, NodeForm<N>> forms = new Hashtable<>();

	// @Cacheable("forms") // tried using Spring cache here but didn't work. And as a sideeffect it caused darn slow application startup, so back to a good ol' Hashtable...
	@SuppressWarnings("unchecked")
	NodeForm<N> createForm(Class<N> clazz) {
//		NodeForm<N> form = this.forms.get(clazz);
//		if (form == null) {
		NodeForm<N> form;
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
			} else {
				throw new IllegalArgumentException("No form defined for unexpected class " + clazz); //$NON-NLS-1$
			}
//			this.forms.put(clazz, form); // cache it
			log.info("created new form for class {}: {}", clazz, form); //$NON-NLS-1$
//		} else {
//			log.info("found existing form for class {}: {}", clazz, form); //$NON-NLS-1$
//		}
		return form;
	}
}
