/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.net.http.HttpRequest.Builder;

import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class SOAPConnection extends HTTPConnection
{
	private static final long serialVersionUID = -7524304663532085424L;

	@NotNull
	@NotEmpty
	@NotBlank
	protected String soapAction;

	@SuppressWarnings("unused")
	private SOAPConnection() { // required for deserialization
		super();
	}

	public SOAPConnection(String name, String description) {
		super(name, description);
		setMethod(HTTP_Method.POST); // SOAP-requests are *always* POSTs!
	}

	@Override
	public void setMethod(HTTP_Method method) {
		if (method != HTTP_Method.POST) throw new IllegalArgumentException("SOAP-requests can only be POSTs!"); //$NON-NLS-1$
		this.method = method;
	}

	public void setSoapAction(String soapAction) {
		this.soapAction = soapAction;
		updateRequestHeaders();
	}

	@Override
	protected void aditionalRequestPreparations(Builder builder) throws Exception {
		builder.setHeader("SOAPAction", '"' + resolveProperties(getSoapAction()) + '"'); //$NON-NLS-1$
	}


}
