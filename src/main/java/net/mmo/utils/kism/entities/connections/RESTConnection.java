/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class RESTConnection extends Connection
{
	private static final long serialVersionUID = -2228821478937714703L;

	public RESTConnection(String name, String description, State state) {
		super(name, description, state);
	}

}
