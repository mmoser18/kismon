
package net.mmo.utils.monitoring_kiss.entities;

import java.io.Serializable;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class Contact extends AbstractEntity implements Cloneable, Serializable
{
	private static final long serialVersionUID = -7262528730016175539L;

	public enum Status {
		ImportedLead,
		NotContacted,
		Contacted,
		Customer,
		ClosedLost 
	}

	@NotNull
	@NotEmpty
	private String firstName = "";
	
	@NotNull
	@NotEmpty
	private String lastName = "";

	@ManyToOne
	@JoinColumn(name = "company_id")
	private Company company;

	@Enumerated(EnumType.STRING)
	@NotNull
	private Contact.Status status;

	@Email
	@NotNull
	@NotEmpty
	private String email = "";
	
}