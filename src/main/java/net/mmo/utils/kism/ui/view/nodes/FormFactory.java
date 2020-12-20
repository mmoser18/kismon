/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes;

import java.util.Hashtable;

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
	// we cache the forms - one per type:
	Hashtable<Class<N>, NodeForm<N>> forms = new Hashtable<>();

	@SuppressWarnings("unchecked")
	NodeForm<N> createForm(N node) {
		log.info("creating new form for node {}:", node); //$NON-NLS-1$
		Class<N> clazz = (Class<N>)node.getClass();
		NodeForm<N> form = this.forms.get(clazz);
		if (form == null) {
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
			this.forms.put(clazz, form);
			log.info("created new form for class {}: {}", clazz, form); //$NON-NLS-1$
		} else {
			log.info("found existing form for class {}: {}", clazz, form); //$NON-NLS-1$
		}
		return form;
	}

}
