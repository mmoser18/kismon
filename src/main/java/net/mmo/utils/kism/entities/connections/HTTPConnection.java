/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Entity
@Setter
@Getter
@Slf4j
abstract public class HTTPConnection extends Connection
{
	private static final long serialVersionUID = -1288268291213672552L;

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

	protected HTTP_Method method = HTTP_Method.POST;

	protected Version httpVersion = Version.HTTP_1_1;
	protected int connectTimeout = 30;

	protected Redirect redirectBehavior = Redirect.NORMAL;

	protected String targetUid = "user"; //$NON-NLS-1$
	protected String targetPwd = "password"; //$NON-NLS-1$

	protected String proxyHost;
	protected int    proxyPort;
//	protected String proxyUid;
//	protected String proxyPwd;

	protected boolean sync = false;

	protected boolean checkResult = false;
	protected String acceptableReturnCodes = "200"; //$NON-NLS-1$

	transient protected HttpClient client;
	transient protected HttpRequest request;

	// request:
	transient protected HttpHeaders requestHeaders;
	transient protected byte[] requestBody;

	// response:
	transient protected HttpHeaders responseHeaders;
	transient protected byte[] responseBody;

	transient protected int responseStatusCode;


	public HTTPConnection() { // required for deserialization
		super();
	}
	public HTTPConnection(String name, String description) {
		super(name, description);
	}

	public void createClient() {
		HttpClient.Builder builder = HttpClient
			.newBuilder()
			.version(this.httpVersion)
			.followRedirects(this.redirectBehavior)
			.connectTimeout(Duration.ofSeconds(this.connectTimeout))
			;
		if (this.targetUid != null && this.targetPwd != null) {
			builder.authenticator(new Authenticator()
				{
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(HTTPConnection.this.targetUid, HTTPConnection.this.targetPwd.toCharArray());
					}
				});
		}
		if (this.proxyHost != null) {
			builder.proxy(ProxySelector.of(new InetSocketAddress(this.proxyHost, this.proxyPort)));
		}
		this.client = builder.build();
	}

	public HttpRequest createRequest() throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
			.uri(URI.create(getResultingURL(this.url)))
			.timeout(Duration.ofSeconds(this.timeout))
			.header("Content-Type", "application/json");  //$NON-NLS-1$ //$NON-NLS-2$
		switch (this.method) {
		case GET:
			builder.GET();
			break;
		case POST:
			builder.POST(BodyPublishers.ofByteArray(this.requestBody));
			break;
		case PUT:
			builder.PUT(BodyPublishers.ofByteArray(this.requestBody));
			break;
		case DELETE:
			builder.DELETE();
			break;
		default:
			throw new IllegalArgumentException("Unsupported HTTP method: " + this.method); //$NON-NLS-1$
		}
		return builder.build();
	}

	public void sendRequest() {
		log.info("sendRequest:"); //$NON-NLS-1$
		if (this.client == null) {
			createClient();
		}
		try {
//			if (this.request == null) {
				this.request = createRequest();
//			}
			reportRequest();
			if (this.sync) {
				HttpResponse<byte[]> response = this.client.send(this.request, BodyHandlers.ofByteArray());
				reportResponse(response);
			} else { // async:
				CompletableFuture<HttpResponse<byte[]>> futureResponse = this.client.sendAsync(this.request, BodyHandlers.ofByteArray());
				futureResponse.thenAccept(response -> reportResponse(response));
			}
		} catch (Throwable t) {
			if (this.request != null) {
				log.error(String.format("error sending request to {}: ", this.request.uri()), t); //$NON-NLS-1$
			} else {
				log.error("Error creating request: ", t); //$NON-NLS-1$
			}
		}
	}

	protected void reportRequest() {
		this.requestHeaders = this.request.headers();
		log.info("{}-request to '{}':\nheaders: {}", this.request.method(), this.request.uri(), this.requestHeaders); //$NON-NLS-1$
		Optional<BodyPublisher> optional = this.request.bodyPublisher();
		if (!optional.isEmpty()) {
			log.info("body (assuming UTF-8): \"{}\"", new String(this.requestBody, StandardCharsets.UTF_8)); //$NON-NLS-1$
		}
	}

	protected void reportResponse(HttpResponse<byte[]> response) {
		this.responseStatusCode = response.statusCode();
		this.responseHeaders = response.headers();
		this.responseBody = response.body();
		log.info("response: status={}\nheaders:{}\nbody (assuming UTF-8): \"{}\"", this.responseStatusCode, this.responseHeaders, new String(this.responseBody, StandardCharsets.UTF_8)); //$NON-NLS-1$
		if (this.checkResult) {
			String acceptableCodes = (this.acceptableReturnCodes != null ? this.acceptableReturnCodes : "200"); //$NON-NLS-1$
			String responseStatusCodeStr = Integer.toString(this.responseStatusCode);
			for (String acceptable: acceptableCodes.split("[,\s]")) { //$NON-NLS-1$
				if (acceptable.equals(responseStatusCodeStr)) {
					this.setState(State.OK);
					return;
				}
			}
			this.setState(State.NOT_OK);
		} else {
			this.setState(State.OK);
		}
	}
}
