/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.net.http.HttpRequest.Builder;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("javadoc")
@Setter
@Getter
public class SOAPConnection extends HTTPConnection
{
	private static final long serialVersionUID = -7524304663532085424L;

	protected String soapAction;


	public SOAPConnection() { // required for deserialization & node-factory
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
		String resolvedSoapAction = resolveProperties(getSoapAction());
		if (resolvedSoapAction != null && !resolvedSoapAction.isEmpty()) {
			builder.setHeader("SOAPAction", resolvedSoapAction); //$NON-NLS-1$
		}
	}


}
