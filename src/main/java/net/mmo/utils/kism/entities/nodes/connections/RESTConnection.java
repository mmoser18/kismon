/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class RESTConnection extends HTTPConnection
{
	private static final long serialVersionUID = -2228821478937714703L;

	@SuppressWarnings("unused")
	private RESTConnection() { // required for deserialization
		super();
	}
	public RESTConnection(String name, String description) {
		super(name, description);
	}


}
