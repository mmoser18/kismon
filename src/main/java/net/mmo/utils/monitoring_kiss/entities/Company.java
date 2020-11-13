
package net.mmo.utils.monitoring_kiss.entities;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Company extends AbstractEntity implements Serializable
{
	private static final long serialVersionUID = -5253062839935140497L;

	private String name;

	@OneToMany(mappedBy = "company", fetch = FetchType.EAGER)
	private List<Contact> employees = new LinkedList<>();

	public Company(String name) {
		setName(name);
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean listEmployees) {
		return this.getClass().getSimpleName()
			+ "(name:" + this.name //$NON-NLS-1$
			+ ", employees:" + (listEmployees //$NON-NLS-1$
			                   ? this.employees
			                   : this.employees.size() + "<details omitted to avoid recursion>") //$NON-NLS-1$
			+ ")"; //$NON-NLS-1$
	}
}
