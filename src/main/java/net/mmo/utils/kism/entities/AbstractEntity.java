/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@SuppressWarnings("javadoc")
@MappedSuperclass
public abstract class AbstractEntity implements Cloneable, Serializable
{
	private static final long serialVersionUID = 452368103142431981L;

	@JsonIgnore
	public static boolean initializing = true; // signals that we are still initializing the application
	                                           // necessary to avoid calling updateAllResolvableValues
	                                           // while still initializing/deserializing a node.

	@JsonIgnore
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id; // = idCtr++;
	public Long getId() {
		return this.id;
	}

	@JsonIgnore
	public boolean isPersisted() {
		return this.id != null;
	}

	@Override
	public int hashCode() {
		return (getId() != null ? getId().hashCode() : super.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final AbstractEntity other = (AbstractEntity)obj;
		if (getId() == null || other.getId() == null) return false;
		return getId().equals(other.getId());
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
		return buf
			.append("{id:").append(getId()) //$NON-NLS-1$
			.append('}')
			.toString();
	}
}