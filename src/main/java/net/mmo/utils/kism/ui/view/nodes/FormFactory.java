/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.ui.view.nodes;

import java.util.Hashtable;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.IntermediateNode;
import net.mmo.utils.kism.entities.Node;
import net.mmo.utils.kism.entities.RootNode;
import net.mmo.utils.kism.entities.connections.RESTConnection;
import net.mmo.utils.kism.ui.view.nodes.connection_forms.RESTConnectionForm;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FormFactory
{
	// we cache the forms - one per type:
	Hashtable<Class<?>, NodeForm> forms = new Hashtable<>();

	NodeForm createForm(Node node, NodeService nodeService) {
		log.info("creating new form for node {}:", node); //$NON-NLS-1$
		Class<?> clazz = node.getClass();
		NodeForm form = this.forms.get(clazz);
		if (form == null) {
			if (clazz == RootNode.class) {
				form = new RootNodeForm(nodeService);
			} else if (clazz == IntermediateNode.class) {
				form = new IntermediateNodeForm(nodeService);
			} else if (clazz == RESTConnection.class) {
				form = new RESTConnectionForm(nodeService);
//			} else if (clazz == SOAPConnection.class) {
//				form = new SOAPConnectionForm(nodeService);
//			} else if (clazz == JDBCConnection.class) {
//				form = new JDBCConnectionForm(nodeService);
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
