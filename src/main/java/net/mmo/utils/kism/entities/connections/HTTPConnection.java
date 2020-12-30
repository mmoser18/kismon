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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.KeyValues;

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

	public final static String PROPERTYNAME_REQUEST_HEADERS  = "requestHeaders"; //$NON-NLS-1$
	public final static String PROPERTYNAME_REQUEST_BODY     = "requestBody"; //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE_HEADERS = "responseHeaders"; //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE_BODY    = "responseBody"; //$NON-NLS-1$

	private final static String KEY_VALUES_SEPARATOR = "="; //$NON-NLS-1$
	private final static String VALUES_SEPARATOR = ","; //$NON-NLS-1$
	private final static String PROPERTY_SEPARATOR = "\n"; //$NON-NLS-1$

	protected HTTP_Method method = HTTP_Method.POST;

	protected Version httpVersion = Version.HTTP_1_1;

	protected Redirect redirectBehavior = Redirect.NORMAL;

	protected String targetUid = "user"; //$NON-NLS-1$
	protected String targetPwd = "password"; //$NON-NLS-1$

	protected String proxyHost;
	protected int    proxyPort;
//	protected String proxyUid;
//	protected String proxyPwd;

	protected boolean syncRequests = false;

	protected boolean checkResult = false;
	protected String acceptableReturnCodes = "200"; //$NON-NLS-1$

	protected String headers;
	protected String payload;

// TODO: the below should be kept as history list:
//	transient protected history = new ArrayList();

	@JsonIgnore
	transient protected HttpClient client;
	@JsonIgnore
	transient protected HttpRequest request;

	// request:
	@JsonIgnore // TODO just for now!
	transient protected HttpHeaders requestHeaders;
	@JsonIgnore
	transient protected byte[] requestBody;

	// response:
	@JsonIgnore
	transient protected HttpHeaders responseHeaders;
	@JsonIgnore
	transient protected byte[] responseBody;
	@JsonIgnore
	transient protected int responseStatusCode = -1;


	public HTTPConnection() { // required for deserialization
		super();
	}
	public HTTPConnection(String name, String description) {
		super(name, description);
	}

	public void setRequestHeaders(HttpHeaders requestHeaders) {
		if (!Objects.equal(this.requestHeaders, requestHeaders)) {
			HttpHeaders oldRequestHeaders = this.requestHeaders;
			this.requestHeaders = requestHeaders;
			informOnPropertyChange(PROPERTYNAME_REQUEST_HEADERS, oldRequestHeaders, requestHeaders);
		}
	}
	public void setRequestBody(byte[] requestBody) {
		if (!Objects.equal(this.requestBody, requestBody)) {
			byte[] oldRequestBody = this.requestBody;
			this.requestBody = requestBody;
			informOnPropertyChange(PROPERTYNAME_REQUEST_BODY, oldRequestBody, requestBody);
		}
	}

	public void setResponseHeaders(HttpHeaders responseHeaders) {
		if (!Objects.equal(this.responseHeaders, responseHeaders)) {
			HttpHeaders oldResponseHeaders = this.responseHeaders;
			this.responseHeaders = responseHeaders;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_HEADERS, oldResponseHeaders, responseHeaders);
		}
	}
	public void setResponseBody(byte[] responseBody) {
		if (!Objects.equal(this.responseBody, responseBody)) {
			byte[] oldResponseBody = this.responseBody;
			this.responseBody = responseBody;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_BODY, oldResponseBody, responseBody);
		}
	}

	// the functional part:

	public void createClient() {
		HttpClient.Builder builder = HttpClient
			.newBuilder()
			.version(this.httpVersion)
			.followRedirects(this.redirectBehavior)
			.connectTimeout(Duration.ofSeconds(this.timeout))
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
			.timeout(Duration.ofSeconds(this.timeout));
		if (this.headers != null) {
			Map<String, Object> map = KeyValues.getEntriesFromString(replaceProperties(this.headers));
			map.forEach((key, value) -> builder.setHeader(key, value.toString()));
		}
		switch (this.method) {
		case GET:
			builder.GET();
			break;
		case POST:
			setRequestBody(replaceProperties(this.payload));
			builder.POST(BodyPublishers.ofByteArray(this.requestBody));
			break;
		case PUT:
			setRequestBody(replaceProperties(this.payload));
			builder.POST(BodyPublishers.ofByteArray(this.requestBody));
			break;
		case DELETE:
			builder.DELETE();
			break;
		default:
			throw new IllegalArgumentException("Unsupported HTTP method: " + this.method); //$NON-NLS-1$
		}
		this.request = builder.build();
		updateRequestValues();
		return this.request;
	}

	public void sendRequest() {
		log.info("sendRequest:"); //$NON-NLS-1$
		if (this.client == null) {
			createClient();
		}
		try {
			this.request = createRequest();
			if (this.syncRequests) {
				long startTime = System.nanoTime();
				HttpResponse<byte[]> response = this.client.send(this.request, BodyHandlers.ofByteArray());
				this.duration = System.nanoTime() - startTime;
				updateResponseValues(response);
			} else { // async:
				long startTime = System.nanoTime();
				CompletableFuture<HttpResponse<byte[]>> futureResponse = this.client.sendAsync(this.request, BodyHandlers.ofByteArray());
				futureResponse.thenAccept(response ->
				{
					this.duration = System.nanoTime() - startTime;
					updateResponseValues(response);
				});
			}
		} catch (Throwable t) {
			if (this.request != null) {
				log.error(String.format("error sending request to {}: ", this.request.uri()), t); //$NON-NLS-1$
			} else {
				log.error("Error creating request: ", t); //$NON-NLS-1$
			}
		}
	}

	public void updateRequestValues() {
		if (this.request != null) {
			this.requestHeaders = this.request.headers();
			log.info("{}-request to '{}':\nheaders: {}", this.request.method(), this.request.uri(), this.requestHeaders); //$NON-NLS-1$
			Optional<BodyPublisher> optional = this.request.bodyPublisher();
			if (!optional.isEmpty()) {
				log.info("body (assuming UTF-8): \"{}\"", this.responseBodyAsString()); //$NON-NLS-1$
			}
		} else {
			this.requestHeaders = null;
		}
	}

	public void updateResponseValues(HttpResponse<byte[]> response) {
		this.responseStatusCode = response.statusCode();
		this.responseHeaders = response.headers();
		this.responseBody = response.body();
		log.info("response: status={} / payload-length={}\nheaders:{}\nbody (assuming UTF-8): \"{}\"", //$NON-NLS-1$
		         this.responseStatusCode, this.responseBody.length, this.responseHeaders, this.responseBodyAsString());
		State res;
		if (this.checkResult) {
			String acceptableCodes = (this.acceptableReturnCodes != null ? this.acceptableReturnCodes : "200"); //$NON-NLS-1$
			String responseStatusCodeStr = Integer.toString(this.responseStatusCode);
			res = State.FAILED;
			for (String acceptable: acceptableCodes.split("[,\\s]")) { //$NON-NLS-1$
				if (acceptable.equals(responseStatusCodeStr)) {
					res = State.OK;
					break;
				}
			}
		} else {
			res = State.OK;
		}
		this.setState(res);
	}

	// utility methods:

	public Integer responseStatusCode() {
		return (this.responseStatusCode != -1 ? Integer.valueOf(this.responseStatusCode) : null);
	}
	public String requestHeadersAsString() {
		return convertHeaders(this.requestHeaders);
	}
	public String responseHeadersAsString() {
		return convertHeaders(this.responseHeaders);
	}

	private static String convertHeaders(HttpHeaders headers) {
		return (headers != null ? convertHeadersMap(headers.map()) : null);
	}
	private static String convertHeadersMap(Map<String, List<String>> headers) {
		return headers.entrySet().stream()
			.map(entry -> entry.getKey() + KEY_VALUES_SEPARATOR + convertHeaderValues(entry.getValue()))
			.collect(Collectors.joining(PROPERTY_SEPARATOR));
	}
	private static String convertHeaderValues(List<String> values) {
		return String.join(VALUES_SEPARATOR, values);
	}

	public String requestBodyAsString() {
		return convertBody(this.getRequestBody());
	}
	public String responseBodyAsString() {
		return convertBody(this.getResponseBody());
	}

	private static String convertBody(byte[] body) {
		return (body != null ? new String(body, StandardCharsets.UTF_8) : ""); //$NON-NLS-1$
	}

	public byte[] setRequestBody(String value) {
		return this.requestBody = convertBody(value);
	}
	private static byte[] convertBody(String value) {
		return (value != null ? value.getBytes(StandardCharsets.UTF_8) : null);
	}
}
