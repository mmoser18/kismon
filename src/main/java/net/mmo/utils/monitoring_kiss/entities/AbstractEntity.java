
package net.mmo.utils.monitoring_kiss.entities;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;
	public Long getId() {
		return id;
	}

	public boolean isPersisted() {
		return id != null;
	}

	@Override
	public int hashCode() {
		return (getId() != null ? getId().hashCode() : super.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (obj == null) { return false; }
		if (getClass() != obj.getClass()) { return false; }
		final AbstractEntity other = (AbstractEntity)obj;
		if (getId() == null || other.getId() == null) { return false; }
		return getId().equals(other.getId());
	}
}