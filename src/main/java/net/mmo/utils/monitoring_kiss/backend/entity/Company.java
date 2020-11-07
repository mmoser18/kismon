
package net.mmo.utils.monitoring_kiss.backend.entity;

import javax.persistence.*;

import lombok.*;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

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
	
	public String toString() {
		return toString(false);
	}

	public String toString(boolean listEmployees) {
		return this.getClass().getSimpleName() 
			+ "(name:" + this.name
			+ ", employees:" + (listEmployees 
			                   ? this.employees 
			                   : this.employees.size() + "<details omitted to avoid recursion>") 
			+ ")";
	}
}
