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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.KeyValuesConverter;

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

	public final static String PROPERTYNAME_REQUEST_HEADERS      = "requestHeaders"; //$NON-NLS-1$
	public final static String PROPERTYNAME_REQUEST_BODY         = "requestBody"; //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE_STATUS_CODE = "responseStatusCode"; //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE_HEADERS     = "responseHeaders"; //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE_BODY        = "responseBody"; //$NON-NLS-1$
	public final static String PROPERTYNAME_RESPONSE_COMPLETE    = "responseComplete"; //$NON-NLS-1$

	private final static String KEY_VALUES_SEPARATOR = ":"; //$NON-NLS-1$
	private final static String VALUES_SEPARATOR     = ","; //$NON-NLS-1$
	private final static String VALUES_FRAGMENT_SEPARATOR = ";"; //$NON-NLS-1$

	private final static String PROPERTY_SEPARATOR = "\n"; //$NON-NLS-1$

	private final static Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1; // the default HTTP 1.1 charset
	private final static String CONTENT_TYPE_HEADER = "Content-Type"; //$NON-NLS-1$
	private final static String LEGAL_CHARSET_NAME_CHARS = "[A-Za-z0-9\\+\\-\\.:_]"; // according to java.nio.charset.Charset //$NON-NLS-1$
	private final static String CHARSET_REGEXP = VALUES_FRAGMENT_SEPARATOR + "\\s*(?i:charset)\\s*=\\s*(\\\"?)(" + LEGAL_CHARSET_NAME_CHARS + LEGAL_CHARSET_NAME_CHARS + "*)\\1"; //$NON-NLS-1$ //$NON-NLS-2$
	private final static int    CHARSET_GROUP  = 2; // the group name containing the character set
	private final static Pattern CHARSET_PATTERN = Pattern.compile(CHARSET_REGEXP);

	protected HTTP_Method method = HTTP_Method.POST;

	protected Version httpVersion = Version.HTTP_1_1;

	protected Redirect redirectBehavior = Redirect.NORMAL;

	protected String targetUid = "user"; //$NON-NLS-1$
	protected String targetPwd = "password"; //$NON-NLS-1$

	protected String proxyHost;
	protected int    proxyPort;
//	protected String proxyUid;
//	protected String proxyPwd;

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


	protected static boolean syncRequests = Boolean.getBoolean(AppProperties.getProperties().getProperty("HTTPConnection.SyncRequests", "false")); //$NON-NLS-1$ //$NON-NLS-2$

	// response:
	@JsonIgnore
	transient protected int responseStatusCode = -1;
	@JsonIgnore
	transient protected HttpHeaders responseHeaders;
	@JsonIgnore
	transient protected byte[] responseBody;


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

	public void setResponseStatusCode(int responseStatusCode ) {
		if (!Objects.equal(this.responseStatusCode, responseStatusCode)) {
			int oldResponseStatusCode = this.responseStatusCode;
			this.responseStatusCode = responseStatusCode;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_STATUS_CODE, oldResponseStatusCode, responseStatusCode);
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
			Map<String, Object> map = KeyValuesConverter.convertStringToMap(resolveProperties(this.headers));
			map.forEach((key, value) -> builder.setHeader(key, value.toString()));
		}
		switch (this.method) {
		case GET:
			setRequestBody((byte[])null);
			builder.GET();
			break;
		case POST:
			setRequestBody(resolveProperties(this.payload));
			builder.POST(BodyPublishers.ofByteArray(getRequestBody()));
			break;
		case PUT:
			setRequestBody(resolveProperties(this.payload));
			builder.POST(BodyPublishers.ofByteArray(getRequestBody()));
			break;
		case DELETE:
			setRequestBody((byte[])null);
			builder.DELETE();
			break;
		default:
			throw new IllegalArgumentException("Unsupported HTTP method: " + this.method); //$NON-NLS-1$
		}
		this.request = builder.build();
		setRequestHeaders(this.request.headers());
		logRequestValues();
		return this.request;
	}

	public void sendRequest() {
		log.info("sendRequest:"); //$NON-NLS-1$
		if (this.client == null) {
			createClient();
		}
		try {
			if (this.request == null) {
				this.request = createRequest();
			}
			if (HTTPConnection.syncRequests) {
				long startTime = System.nanoTime();
				HttpResponse<byte[]> response = this.client.send(this.request, BodyHandlers.ofByteArray());
				responseReceived(System.nanoTime() - startTime, response);
			} else { // async:
				long startTime = System.nanoTime();
				CompletableFuture<HttpResponse<byte[]>> futureResponse = this.client.sendAsync(this.request, BodyHandlers.ofByteArray());
				futureResponse.thenAccept(response ->
				{
					responseReceived(System.nanoTime() - startTime, response);
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

	public void responseReceived(long callDuration, HttpResponse<byte[]> response) {
		log.info("responseReceived after {} nanosecs.", callDuration); //$NON-NLS-1$
		setDuration(callDuration);
		setResponseStatusCode(response.statusCode());
		setResponseHeaders(response.headers());
		setResponseBody(response.body());
		logResponseValues();
		deriveState();
		informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, System.nanoTime());
	}

	private void deriveState() {
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
		setState(res);
	}

	public void logRequestValues() {
		log.info("{}-request to '{}':\nheaders: {}", this.request.method(), this.request.uri(), this.requestHeaders); //$NON-NLS-1$
		Optional<BodyPublisher> optional = this.request.bodyPublisher();
		if (!optional.isEmpty()) {
			log.info("body: \"{}\"", this.getRequestBodyAsString()); //$NON-NLS-1$
		}
	}

	public void logResponseValues() {
		log.info("response: status={} / payload-length={}\nheaders:{}\nbody (assuming UTF-8): \"{}\"", //$NON-NLS-1$
		         this.responseStatusCode, this.responseBody.length, this.responseHeaders, this.getResponseBodyAsString());
	}

	// utility methods:

	public Integer getResponseStatusCode() {
		return (this.responseStatusCode != -1 ? Integer.valueOf(this.responseStatusCode) : null);
	}
	@JsonIgnore
	public String getRequestHeadersAsString() {
		return convertHeadersToString(this.requestHeaders);
	}
	@JsonIgnore
	public String getResponseHeadersAsString() {
		return convertHeadersToString(this.responseHeaders);
	}

	private static String convertHeadersToString(HttpHeaders headers) {
		return (headers != null ? convertMapToString(headers.map()) : ""); //$NON-NLS-1$
	}
	private static String convertMapToString(Map<String, List<String>> headers) {
		return headers.entrySet().stream()
			.map(entry -> entry.getKey() + KEY_VALUES_SEPARATOR + convertHeaderValues(entry.getValue()))
			.collect(Collectors.joining(PROPERTY_SEPARATOR));
	}
	private static String convertHeaderValues(List<String> values) {
		return String.join(VALUES_SEPARATOR, values);
	}

	/** extract character set from request headers - if there is any...
	 * @throws Exception */
	protected Charset extractRequestCharset() {
		HttpHeaders reqHdrs = this.getRequestHeaders();
		return extractRequestCharset(reqHdrs != null ? reqHdrs.map() : null);
	}
	/** extract character set from request headers - if there is any...
	 * @throws Exception */
	protected Charset extractRequestCharset(Map<String, List<String>> map) {
		if (map != null) {
			List<String> mapValues = map.get(CONTENT_TYPE_HEADER);
			if (mapValues != null) {
				String contentType = mapValues.get(0);
				int pos = contentType.indexOf(VALUES_FRAGMENT_SEPARATOR);
				if (pos > 1) {
					String charsetFragment = contentType.substring(pos);
					log.trace("extractRequestCharset: charsetFragment = '{}'", charsetFragment); //$NON-NLS-1$
					Matcher m = CHARSET_PATTERN.matcher(charsetFragment);
					if (m.matches()) {
						String match = m.group(CHARSET_GROUP);
						log.trace("extractRequestCharset: match = '{}'", match); //$NON-NLS-1$
						if (match != null && Charset.isSupported(match = match.toUpperCase())) {
							Charset cs = Charset.forName(match);
							log.info("extractRequestCharset: found character set: '{}'", cs); //$NON-NLS-1$
							return cs;
						} else {
							log.warn("extractRequestCharset: charset '{}' not supported", match); //$NON-NLS-1$
							// throw new Exception("charset '" + match + "' not supported");
						}
					} else {
						log.warn("extractRequestCharset: '{}' not matching regexp '{}'.", contentType, CHARSET_REGEXP); //$NON-NLS-1$
						// throw new Exception("charset '" + contentType + "' not matching expected format");
					}
				} else {
					log.trace("extractRequestCharset: no charset fragment found in ", contentType); //$NON-NLS-1$
				}
			} else {
				log.trace("extractRequestCharset: '{}' not found in headers.", CONTENT_TYPE_HEADER); //$NON-NLS-1$
			}
		}
		log.info("extractRequestCharset: using default charset: '{}'", DEFAULT_CHARSET); //$NON-NLS-1$
		return DEFAULT_CHARSET;
	}

	/** extract character set from response headers - if there is any... */
	protected Charset extractResponseCharset() {
		return DEFAULT_CHARSET;
	}

	@JsonIgnore
	public String getRequestBodyAsString() {
		return convertBodyToString(this.getRequestBody(), extractRequestCharset());
	}
	@JsonIgnore
	public String getResponseBodyAsString() {
		return convertBodyToString(this.getResponseBody(), extractResponseCharset());
	}
	private static String convertBodyToString(byte[] body, Charset charset) {
		return (body != null ? new String(body, charset) : ""); //$NON-NLS-1$
	}

	public void setRequestBody(String value) {
		setRequestBody(convertBodyFromString(value, extractRequestCharset()));
	}
	private static byte[] convertBodyFromString(String value, Charset charset) {
		return (value != null ? value.getBytes(charset) : null);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", method:").append(this.method) //$NON-NLS-1$
			.append(", httpVersion:").append(this.httpVersion) //$NON-NLS-1$
			.append(", redirectBehavior:").append(this.redirectBehavior) //$NON-NLS-1$
			.append(", targetUid:").append(this.targetUid) //$NON-NLS-1$
			.append(", targetPwd:").append(this.targetPwd) //$NON-NLS-1$
//			.append(", proxyHost:").append(this.proxyHost) //$NON-NLS-1$
//			.append(", proxyPort:").append(this.proxyPort) //$NON-NLS-1$
//			.append(", proxyUid:").append(this.proxyUid) //$NON-NLS-1$
//			.append(", proxyPwd:").append(this.proxyPwd) //$NON-NLS-1$

			.append(", checkResult:").append(this.checkResult) //$NON-NLS-1$
			.append(", acceptableReturnCodes:").append(this.acceptableReturnCodes) //$NON-NLS-1$
			.append(", headers:").append(this.headers) //$NON-NLS-1$
			.append(", payload:").append(this.payload) //$NON-NLS-1$
			.append(" transient: ") //$NON-NLS-1$
			.append("client:").append(this.client) //$NON-NLS-1$
			.append("request:").append(this.request) //$NON-NLS-1$
			.append("client:").append(this.client) //$NON-NLS-1$
			.append("requestHeaders:").append(this.requestHeaders) //$NON-NLS-1$
			.append("requestBody:").append(this.requestBody) //$NON-NLS-1$
			.append("responseStatusCode:").append(this.responseStatusCode) //$NON-NLS-1$
			.append("responseHeaders:").append(this.responseHeaders) //$NON-NLS-1$
			.append("responseBody:").append(this.responseBody) //$NON-NLS-1$
			.append("]") //$NON-NLS-1$
			.toString();
	}

}
