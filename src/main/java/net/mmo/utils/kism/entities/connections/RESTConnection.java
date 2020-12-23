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

	@Getter
	public enum HTTP_Method {
		GET    (false),	// GET method requests a representation of the specified resource. Requests using GET should only retrieve data.
		POST   (true),	// POST method is used to submit an entity to the specified resource, often causing a change in state or side effects on the server.
		PUT    (true),	// PUT method replaces all current representations of the target resource with the request payload.
		DELETE (false),	// DELETE method deletes the specified resource.
		HEAD   (false),	// HEAD method asks for a response identical to that of a GET request, but without the response body.
		CONNECT(false), // CONNECT method establishes a tunnel to the server identified by the target resource.
		OPTIONS(false), // OPTIONS method is used to describe the communication options for the target resource.
		TRACE  (false),	// TRACE method performs a message loop-back test along the path to the target resource.
		PATCH  (false);	// PATCH method is used to apply partial modifications to a resource.

		private final boolean withPayload;

		HTTP_Method(boolean withPayload) {
			this.withPayload = withPayload;
		}
	}

	private HTTP_Method method = HTTP_Method.GET;
	// TODO: should be byte[]
	private String payload;

	public RESTConnection() { // required for deserialization
		super();
	}
	public RESTConnection(String name, String description) {
		super(name, description);
	}
}
