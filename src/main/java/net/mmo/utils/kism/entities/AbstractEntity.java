
package net.mmo.utils.kism.entities;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonIgnore;

@MappedSuperclass
public abstract class AbstractEntity
{
	//	private static long idCtr;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@JsonIgnore
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
		if (this == obj) { return true; }
		if (obj == null) { return false; }
		if (getClass() != obj.getClass()) { return false; }
		final AbstractEntity other = (AbstractEntity)obj;
		if (getId() == null || other.getId() == null) { return false; }
		return getId().equals(other.getId());
	}
}