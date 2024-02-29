
package net.mmo.utils.kism.entities;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonIgnore;

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