/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.ResultChecker;
import net.mmo.utils.kism.utils.ExceptionUtils;
import net.mmo.utils.kism.utils.KeyValuesConverter;

@SuppressWarnings("javadoc")
@Setter
@Getter
@Slf4j
abstract public class HTTPConnection extends TCPConnection
{
	private static final long serialVersionUID = -1288268291213672552L;

	/**
	 * the supported HTTP Methods
	 */
	@Getter
	public enum HTTP_Method {
		GET    (false),	// GET method requests a representation of the specified resource. Requests using GET should only retrieve data.
		POST   (true),	// POST method is used to submit an entity to the specified resource, often causing a change in state or side effects on the server.
		PUT    (true),	// PUT method replaces all current representations of the target resource with the httpRequest payload.
		DELETE (false),	// DELETE method deletes the specified resource.
		HEAD   (false),	// HEAD method asks for a response identical to that of a GET httpRequest, but without the response body.
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

	private final static byte[] EMPTY_BODY = new byte[0];
	private final static String VALUE_UNDEFINED = ""; //$NON-NLS-1$

	private final static String VALUES_FRAGMENT_SEPARATOR = ";"; //$NON-NLS-1$

	private final static Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1; // the default HTTP 1.1 charset
	private final static String  CONTENT_TYPE_HEADER = "Content-Type"; //$NON-NLS-1$
	private final static String  LEGAL_CHARSET_NAME_CHARS = "[A-Za-z0-9\\+\\-\\.:_]"; // according to java.nio.charset.Charset //$NON-NLS-1$
	private final static String  CHARSET_REGEXP = VALUES_FRAGMENT_SEPARATOR + "\\s*(?i:charset)\\s*=\\s*(\\\"?)(" + LEGAL_CHARSET_NAME_CHARS + LEGAL_CHARSET_NAME_CHARS + "*)\\1"; //$NON-NLS-1$ //$NON-NLS-2$
	private final static int     CHARSET_GROUP_NR  = 2; // the group name containing the character set
	private final static Pattern CHARSET_PATTERN = Pattern.compile(CHARSET_REGEXP);

	protected HTTP_Method method = HTTP_Method.POST;

	protected String targetUid = "<user>"; //$NON-NLS-1$
	protected String targetPwd = "<password>"; //$NON-NLS-1$
	protected boolean includeBasicAuthHeader = false;

	// these two we don't support via the UI (yet):
	protected Version httpVersion = Version.HTTP_1_1;
	protected Redirect redirectBehavior = Redirect.NORMAL; // NORMAL: allow redirection except from https to http-URLs

	protected String headers;
	protected String payload;

	// for response checking:
	protected String acceptableReturnCodes = "200"; //$NON-NLS-1$

	// transient stuff:
	@JsonIgnore
	transient protected HttpClient httpClient;
	@JsonIgnore
	transient protected HttpRequest httpRequest;

	// result stuff:
	// TODO: the below should be kept into some kind of "history"-list:
	//	transient protected history = new ArrayList();

	@JsonIgnore // TODO just for now!
	protected HttpHeaders requestHeaders;
	@JsonIgnore
	protected byte[] requestBody;

	// response:
	@JsonIgnore
	protected int responseStatusCode = -1;
	@JsonIgnore
	protected HttpHeaders responseHeaders;
	@JsonIgnore
	protected byte[] responseBody;


	// If set then we use a TrustManager that trusts ALL (i.e. also self signed) certificates:
	protected final static TrustManager[] trustAllCerts =
		new TrustManager[] {
			new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					// empty
				}
				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
					// empty
				}
			}
		};

	protected final static CookieHandler cookieHandler =
		new CookieManager(null, // CookieStore to be used by cookie manager. If null, cookie manager
		                        // will use a default one, which is an in-memory CookieStore implementation.
		                  CookiePolicy.ACCEPT_ALL); // CookiePolicy instance to be used by cookie
		                                            // manager as policy callback.
		                                            // If null, ACCEPT_ORIGINAL_SERVER will be used.


	public HTTPConnection() { // required for deserialization
		super();
	}
	public HTTPConnection(String name, String description) {
		super(name, description);
	}

	@Override
	public void updateResolvableValues() {
		super.updateResolvableValues();
		updateRequestHeaders();
		updateRequestBody();
	}

//	@Override
//	public void setProxyHost(String proxyHost) {
//		super.setProxyHost(proxyHost);
//		setHttpClient(null); // trigger a recreation of the connection if the proxyHost changed
//	}
//	@Override
//	public void setProxyPort(int proxyPort) {
//		super.setProxyHost(proxyPort);
//		setHttpClient(null); // trigger a recreation of the connection if the proxyPort changed
//	}
//	@Override
//	public void setProxyUid(String proxyUid) {
//		super.setProxyHost(proxyUid);
//		setHttpClient(null); // trigger a recreation of the connection if the proxyUid changed
//	}
//	@Override
//	public void setProxyPwd(String proxyPwd) {
//		super.setProxyPwd(proxyPwd);
//		setHttpClient(null); // trigger a recreation of the connection if the proxyPwd changed
//	}

	@Override
	public void setResultingUrl(String resultingUrl) {
		super.setResultingUrl(resultingUrl);
		setHttpRequest(null); // trigger a recreation of the request if the URL changed
	}

	@Override
	public void setTimeout(int timeout) {
		super.setTimeout(timeout);
		setHttpClient(null); // trigger a recreation of the connection if the timeout changes
	}

	public void setMethod(HTTP_Method method) {
		this.method = method;
		setHttpRequest(null); // trigger a recreation of the request if the method changes
		setRequestHeaders(null);
		setRequestBody((byte[])null);
	}

	public void setHttpVersion(Version httpVersion) {
		this.httpVersion = httpVersion;
		setHttpClient(null); // trigger a recreation of the connection if the http version ever changes
	}

	public void setTargetUid(String targetUid) {
		this.targetUid = targetUid;
		setHttpClient(null); // trigger a recreation of the connection if the dbUid changes
	}
	public void setTargetPwd(String targetPwd) {
		this.targetPwd = targetPwd;
		setHttpClient(null); // trigger a recreation of the connection if the dbPwd changes
	}

	public void setHeaders(String headers) {
		this.headers = headers;
		updateRequestHeaders();
	}

	public void setPayload(String payload) {
		this.payload = payload;
		updateRequestBody();
	}

	public void setAcceptableReturnCodes(String acceptableReturnCodes) {
		this.acceptableReturnCodes = acceptableReturnCodes;
	}

	private synchronized void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}
	private synchronized void setHttpRequest(HttpRequest httpRequest) {
		this.httpRequest = httpRequest;
	}

	protected void updateRequestHeaders() {
		try {
			if (!initializing) createRequest(); // this sets the requestHeaders as side-effect
		} catch (Exception ex) {
			log.error("Error resolving '{}':", ex); //$NON-NLS-1$
			this.requestHeaders = null;
		}
	}

	protected void updateRequestBody() {
		if (!initializing) updateResultingValue(getPayload(), (str) -> setRequestBody(str));
	}

	public void setRequestHeaders(HttpHeaders requestHeaders) {
		if (!Objects.equals(this.requestHeaders, requestHeaders)) {
			HttpHeaders oldRequestHeaders = this.requestHeaders;
			this.requestHeaders = requestHeaders;
			informOnPropertyChange(PROPERTYNAME_REQUEST_HEADERS, oldRequestHeaders, requestHeaders);
		}
	}

	public void setRequestBody(byte[] requestBody) {
		if (!Objects.equals(this.requestBody, requestBody)) {
			byte[] oldRequestBody = this.requestBody;
			this.requestBody = requestBody;
			informOnPropertyChange(PROPERTYNAME_REQUEST_BODY, oldRequestBody, requestBody);
		}
	}

	@JsonIgnore
	public void setRequestBody(String value) {
		setRequestBody(convertBodyFromString(value, extractRequestCharset()));
	}
	private static byte[] convertBodyFromString(String value, Charset charset) {
		return (value != null ? value.getBytes(charset) : EMPTY_BODY);
	}

	// update received data:

	public void setResponseStatusCode(int responseStatusCode ) {
		if (!Objects.equals(this.responseStatusCode, responseStatusCode)) {
			int oldResponseStatusCode = this.responseStatusCode;
			this.responseStatusCode = responseStatusCode;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_STATUS_CODE, oldResponseStatusCode, responseStatusCode);
		}
	}
	public void setResponseHeaders(HttpHeaders responseHeaders) {
		if (!Objects.equals(this.responseHeaders, responseHeaders)) {
			HttpHeaders oldResponseHeaders = this.responseHeaders;
			this.responseHeaders = responseHeaders;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_HEADERS, oldResponseHeaders, responseHeaders);
		}
	}
	public void setResponseBody(byte[] responseBody) {
		if (!Objects.equals(this.responseBody, responseBody)) {
			byte[] oldResponseBody = this.responseBody;
			this.responseBody = responseBody;
			informOnPropertyChange(PROPERTYNAME_RESPONSE_BODY, oldResponseBody, responseBody);
		}
	}

	// the functional part:

	public void createClient() {

		HttpClient.Builder builder = HttpClient
			.newBuilder()
			.version(getHttpVersion())
			.followRedirects(getRedirectBehavior())
			.cookieHandler(HTTPConnection.cookieHandler)
			.connectTimeout(Duration.ofSeconds(getTimeout()))
			;

		if (acceptableSSLVersions != null && acceptableSSLVersions.length > 0) {
			SSLParameters sslParameters = new SSLParameters();
			sslParameters.setProtocols(acceptableSSLVersions);
			builder.sslParameters(sslParameters);
		}

		if (trustAllCertificates) {
			SSLContext sslContext = null;
			try {
				sslContext = SSLContext.getInstance("TLS"); //$NON-NLS-1$
				sslContext.init(null, HTTPConnection.trustAllCerts, new java.security.SecureRandom());
			} catch (NoSuchAlgorithmException | KeyManagementException ex) {
				log.error("Error creating SSLContext accepting ALL certificates - ignored", ex); //$NON-NLS-1$
			}
			if (sslContext != null) builder.sslContext(sslContext);
		}

		if (getTargetUid() != null && getTargetPwd() != null) {
			builder.authenticator(new Authenticator()
				{
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						try {
							return new PasswordAuthentication(resolveProperties(getTargetUid()),
							                                  resolveProperties(getTargetPwd()).toCharArray());
						} catch (Exception ex) {
							log.error("uid and/or password resolved to null - no authentication possible", ex); //$NON-NLS-1$
							return null;
						}
					}
				});
		}
		// TODO implement proxied access
//		if (this.proxyHost != null) {
//			builder.proxy(ProxySelector.of(new InetSocketAddress(this.proxyHost, this.proxyPort)));
//			... what to do with proxy-uid and proxy-pwd?
//		}
		setHttpClient(builder.build());
	}

	public void createRequest() throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
			.uri(URI.create(resultingUrlResolved()))
			.timeout(Duration.ofSeconds(getTimeout()));
		if (getHeaders() != null) {
			Map<String, Object> map = KeyValuesConverter.convertStringToMap(resolveProperties(getHeaders()));
			map.forEach((key, value) -> builder.setHeader(key, value.toString()));
		}
		if (this.includeBasicAuthHeader) {
			builder.setHeader("Authorization", //$NON-NLS-1$
			                  "Basic " + new String(Base64.getEncoder() //$NON-NLS-1$
			                                              .encode((resolveProperties(getTargetUid())
			                                                      + ":" //$NON-NLS-1$
			                                                      + resolveProperties(getTargetPwd()))
			                                              .getBytes()),
			                                        DEFAULT_CHARSET));
		}
		switch (getMethod()) {
		case GET:
			setRequestBody(EMPTY_BODY);
			builder.GET();
			break;
		case POST: {
			setRequestBody(resolveProperties(getPayload()));
			byte[] body = getRequestBody();
			builder.POST(BodyPublishers.ofByteArray(body != null ? body : EMPTY_BODY));
			break;
		}
		case PUT: {
			setRequestBody(resolveProperties(getPayload()));
			byte[] body = getRequestBody();
			builder.POST(BodyPublishers.ofByteArray(body != null ? body : EMPTY_BODY));
			break;
		}
		case DELETE:
			setRequestBody(EMPTY_BODY);
			builder.DELETE();
			break;
		default:
			throw new IllegalArgumentException("Unsupported HTTP method: " + getMethod()); //$NON-NLS-1$
		}
		aditionalRequestPreparations(builder);

		setHttpRequest(builder.build());
		setRequestHeaders(getHttpRequest().headers());
		// setResponseHeaders(null);
		// setResponseBody(null);
		logRequestValues();
	}

	/**
	 * Hook allowing to add e.g. additional request headers or add additional request parameters, etc.
	 * @param builder
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	protected void aditionalRequestPreparations(Builder builder) throws Exception {
		// empty
	}

	@SuppressWarnings("null") // the program flow guarantees that request is != null if response is <> null!
	@Override
	public void sendRequest() throws Exception {
		log.debug("sendRequest '{}':", getName()); //$NON-NLS-1$
		HttpClient client = null;
		HttpRequest request = null;
		HttpResponse<byte[]> response = null;
		try {
			setTimestamp(LocalDateTime.now());
			synchronized(this) {
				if (getHttpClient() == null) {
					createClient();
				}
				client = getHttpClient();
			}
			synchronized(this) {
				if (getHttpRequest() == null) {
					createRequest();
				}
				request = getHttpRequest();
			}
			// signal response pending:
			setResponseHeaders(null);
			setResponseBody("<no response received (yet)>".getBytes()); //$NON-NLS-1$
			// ... before executing the actual request:
			long startTime = System.nanoTime();
			response = client.send(request, BodyHandlers.ofByteArray());
			long callDuration = System.nanoTime() - startTime;
			setDuration(callDuration);
			log.trace("responseReceived for '{}' after {} microsecs.", getName(), callDuration/1000); //$NON-NLS-1$
			responseReceived(response);
		} catch (Exception ex) {
			log.debug("exception executing '{}': {}", getName(), ex.getMessage()); //$NON-NLS-1$
			setResponseStatusCode(-1);
			setResponseHeaders(null);
			setResponseBody(ExceptionUtils.exceptionCauseSummary(ex).getBytes());
			setDuration(NO_RESPONSE_DURATION); // signals an exception
			setState(State.FAILED);
			setRequestResult(getState().name() + '/' + ExceptionUtils.exceptionRootCauseMsg(ex));
			if (response != null) {
				throw new Exception(String.format("Error for '%s' processing response from '%s': ", //$NON-NLS-1$
				                                  getName(), request.uri()), ex);
			} else if (request != null) {
				throw new Exception(String.format("Error for '%s' sending request to '%s':", //$NON-NLS-1$
				                                  getName(), request.uri()), ex);
			} else {
				throw new Exception(String.format("Error for '%s' creating request", getName()), ex); //$NON-NLS-1$
			}
		} finally {
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, response);
		}
	}

	public void responseReceived(HttpResponse<byte[]> response) throws Exception {
		setResponseStatusCode(response.statusCode());
		setResponseHeaders(response.headers());
		setResponseBody(response.body());
		logResponseValues();
		deriveState();
		setRequestResult(getState().name() + '/' + response.statusCode());
	}

	private void deriveState() throws Exception {
		State res = State.OK;
		String name = getName();
		if (isCheckResults()) {
			String acceptableCodes = resolveProperties(getAcceptableReturnCodes());
			log.trace("deriveState '{}': acceptableCodes:{}", name, acceptableCodes); //$NON-NLS-1$
			if (acceptableCodes != null && !acceptableCodes.isBlank()) {
				String responseStatusCodeStr = Integer.toString(getResponseStatusCode());
				log.trace("deriveState '{}': responseStatusCode:{} - responseStatusCodeStr:{}", name, getResponseStatusCode(), responseStatusCodeStr); //$NON-NLS-1$
				search: {
					for (String acceptable: acceptableCodes.split("[,\\s]")) { //$NON-NLS-1$
						if (acceptable.equals(responseStatusCodeStr)) {
							log.debug("deriveState '{}': responseStatusCode:{} -> matched: {}", name, getResponseStatusCode(), acceptable); //$NON-NLS-1$
							break search;
						}
						log.trace("deriveState '{}': responseStatusCode:{} -> did not match: {}", name, getResponseStatusCode(), acceptable); //$NON-NLS-1$
					}
					log.debug("deriveState '{}': responseStatusCode:{} -> no match found.", name,getResponseStatusCode()); //$NON-NLS-1$
					res = State.FAILED;
				} // :search
			}
			ResultChecker checker = getResultChecker();
			if (checker != null && checker.getCondition() != null) {
				res = getResultChecker().checkResult(this, responseBodyAsString());
			} else { // we only check for acceptableReturnCodes - might want to check that we did...y
				log.debug("no result check defined."); //$NON-NLS-1$
			}
		}
		log.debug("deriveState '{}': {} - checking:{} (received:'{}', acceptable:'{}', checker:{})", name, res, isCheckResults(), getResponseStatusCode(), getAcceptableReturnCodes(), getResultChecker()); //$NON-NLS-1$
		setState(res);
	}

	public void logRequestValues() {
		Optional<BodyPublisher> optional = getHttpRequest().bodyPublisher();
		log.debug("{}-httpRequest to '{}' / headers: {} / body: {} bytes", //$NON-NLS-1$
		          getHttpRequest().method(), getHttpRequest().uri(), (optional.isEmpty() ? 0 : getRequestBody().length));
		if (!optional.isEmpty()) {
			log.trace("body: \"{}\"", requestBodyAsString()); //$NON-NLS-1$
		}
	}

	public void logResponseValues() {
		log.debug("response: status={} / payload-length={} / headers:{} / body: {} bytes", //$NON-NLS-1$
		          getResponseStatusCode(), getResponseBody().length, getResponseHeaders(), getResponseBody().length);
		log.trace("response: body: \"{}\"", responseBodyAsString()); //$NON-NLS-1$
	}

	// utility methods:

	@JsonIgnore
	public Integer getResponseStatusCode() {
		return (this.responseStatusCode != -1 ? Integer.valueOf(this.responseStatusCode) : null);
	}

	public String requestHeadersAsString() {
		return convertHeadersToString(getRequestHeaders());
	}
	@JsonIgnore
	public String responseHeadersAsString() {
		return convertHeadersToString(getResponseHeaders());
	}

	private static String convertHeadersToString(HttpHeaders headers) {
		return (headers != null ? KeyValuesConverter.convertHeaderMapToString(headers.map()) : ""); //$NON-NLS-1$
	}

	/** extract character set from httpRequest headers - if there is any...
	 * @throws Exception */
	protected Charset extractRequestCharset() {
		HttpHeaders requHdrs = getRequestHeaders();
		return extractRequestCharset(requHdrs != null ? requHdrs.map() : null);
	}
	/** extract character set from response headers - if there is any... */
	protected Charset extractResponseCharset() {
		HttpHeaders respHdrs = getResponseHeaders();
		return extractRequestCharset(respHdrs != null ? respHdrs.map() : null);
	}
	/** extract character set from Content-Type header - if there is any... */
	protected Charset extractRequestCharset(Map<String, List<String>> map) {
		return (Charset)KeyValuesConverter.extractValueFragment(map,
		                                                        CONTENT_TYPE_HEADER, VALUES_FRAGMENT_SEPARATOR,
		                                                        CHARSET_PATTERN, CHARSET_GROUP_NR,
		                                                        (str) -> {
		                                                        	String charsetName = str.toUpperCase();
		                                                        	if (Charset.isSupported(charsetName)) {
		                                                        		Charset cs = Charset.forName(charsetName);
		                                                        		log.debug("extractRequestCharset: found character set: '{}'", cs); //$NON-NLS-1$
		                                                        		return cs;
		                                                        	} else {
		                                                        		log.warn("extractRequestCharset: charset '{}' not supported", charsetName); //$NON-NLS-1$
		                                                        		// throw new Exception("charset '" + match + "' not supported");
		                                                        		return null;
		                                                        	}
		                                                        },
		                                                        DEFAULT_CHARSET);
	}

	public String requestBodyAsString() {
		return convertBodyToString(getRequestBody(), extractRequestCharset());
	}
	public String responseBodyAsString() {
		return convertBodyToString(getResponseBody(), extractResponseCharset());
	}
	private static String convertBodyToString(byte[] body, Charset charset) {
		return (body != null ? new String(body, charset) : VALUE_UNDEFINED);
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
			.append(", acceptableReturnCodes:").append(this.acceptableReturnCodes) //$NON-NLS-1$
			.append(", headers:").append(this.headers != null ? this.headers.replace("\n", "\u2424") : null) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			.append(", payload:").append((this.payload != null ? this.payload.length() : 0)).append(" characters)") //$NON-NLS-1$ //$NON-NLS-2$
			.append(", httpClient:").append(this.httpClient) //$NON-NLS-1$
			.append(", httpRequest:").append(this.httpRequest) //$NON-NLS-1$
			.append(", requestHeaders:").append(this.requestHeaders) //$NON-NLS-1$
			.append(", requestBody:(").append((this.requestBody != null ? this.requestBody.length : 0)).append(" bytes)") // echoing the entire requestBody was flooding the logs  //$NON-NLS-1$ //$NON-NLS-2$
			.append(", responseStatusCode:").append(this.responseStatusCode) //$NON-NLS-1$
			.append(", responseHeaders:").append(this.responseHeaders) //$NON-NLS-1$
			.append(", responseBody:(").append((this.responseBody != null ? this.responseBody.length : 0)).append(" bytes)") // echoing the entire responseBody was flooding the logs //$NON-NLS-1$ //$NON-NLS-2$
			.append("}") //$NON-NLS-1$
			.toString();
	}

}
