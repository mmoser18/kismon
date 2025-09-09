/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
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
import net.mmo.utils.kism.utils.HTTP_Authorization;
import net.mmo.utils.kism.utils.KeyValuesConverter;
import net.mmo.utils.kism.utils.StringUtils;

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
	public final static byte[] EMPTY_BODY = new byte[0];

	public final static String VALUE_UNDEFINED = ""; //$NON-NLS-1$

	private final static String VALUES_FRAGMENT_SEPARATOR = ";"; //$NON-NLS-1$

	public final static Charset DEFAULT_HTTP_CHARSET = StandardCharsets.ISO_8859_1; // the default HTTP 1.1 charset
	private final static String  CONTENT_TYPE_HEADER = "Content-Type"; //$NON-NLS-1$
	private final static String  LEGAL_CHARSET_NAME_CHARS = "[A-Za-z0-9+\\-\\.:_]"; // according to java.nio.charset.Charset //$NON-NLS-1$
	private final static String  CONTENT_TYPE_CHARSET_REGEXP = VALUES_FRAGMENT_SEPARATOR + "\\s*(?i:charset)\\s*=\\s*(\\\"?)(" + LEGAL_CHARSET_NAME_CHARS + "+)\\1"; //$NON-NLS-1$ //$NON-NLS-2$
	private final static int     CONTENT_TYPE_CHARSET_GROUP_NR  = 2; // the group name containing the character set
	private final static Pattern CONTENT_TYPE_CHARSET_PATTERN = Pattern.compile(CONTENT_TYPE_CHARSET_REGEXP);

	private final static String DOCTYPE_HTML = "<!doctype html>"; // must be in lower-case!  //$NON-NLS-1$
	private final static Pattern META_CHARSET_PATTERN = Pattern.compile("(?s:.)*<head>(?s:.)*<meta charset=\"(" + LEGAL_CHARSET_NAME_CHARS + "+)\"(?s:.)*/>(?s:.)*"); //$NON-NLS-1$ //$NON-NLS-2$ - must be in lowercase!
	private final static int     META_CHARSET_GROUP_NR  = 1; // the group name containing the character set


	private final static int MAX_REDIRECTIONS = 10;
	private final static int MAX_AUTH_ATTEMPTS = 3;


	protected HTTP_Method method = HTTP_Method.GET;

	protected String targetUid = "<user>"; //$NON-NLS-1$
	protected String targetPwd = "<password>"; //$NON-NLS-1$
	protected boolean includeBasicAuthHeader = false;

	// these two we don't support via the UI (yet):
	protected Version httpVersion = Version.HTTP_1_1;
	protected Redirect redirectBehavior = Redirect.NORMAL; // "NORMAL" means: allow redirection except from https to http-URLs

	protected String headers;
	protected String payload;

	// for response checking:
	protected String acceptableReturnCodes = "200"; //$NON-NLS-1$

	// transient stuff:
	@JsonIgnore
	transient protected URI resolvedURI;
	@JsonIgnore
	transient protected HttpClient httpClient;
	@JsonIgnore
	transient protected HttpRequest httpRequest;

	// result stuff:
	// TODO: the below should be kept in some kind of "history"-list:
	//	transient protected history = new ArrayList();

	@JsonIgnore // TODO just for now!
	transient protected HttpHeaders requestHeaders;
	@JsonIgnore
	transient protected byte[] requestBody;

	// response:
	@JsonIgnore
	transient protected int responseStatusCode = -1;
	@JsonIgnore
	transient protected HttpHeaders responseHeaders;
	@JsonIgnore
	transient protected byte[] responseBody;

	// Stuff for the handling of Digest authenticaton:
	@JsonIgnore
	transient protected String authorizationValue;
	@JsonIgnore
	transient private String previousNonce;
	transient private int nonceCount;


	// If set then we use a TrustManager that trusts ALL (i.e. also self signed) certificates:
	protected static final TrustManager trustAllCerts =
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
			@Override
			public String toString() {
				return "trustAllCerts(" + X509TrustManager.class.getSimpleName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		};

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
		String oldUrl = this.getResultingUrl();
		if (!Objects.equals(resultingUrl, oldUrl)) {
			super.setResultingUrl(resultingUrl);
			setHttpRequest(null); // trigger a recreation of the request if the URL changed
		}
	}

	@Override
	public void setTimeout(int timeout) {
		int oldTimeout = this.getTimeout();
		if (timeout != oldTimeout) {
			super.setTimeout(timeout);
			setHttpClient(null); // trigger a recreation of the connection if the timeout changes
		}
	}

	public void setMethod(HTTP_Method method) {
		this.method = method;
		setHttpRequest(null); // trigger a recreation of the request if the method changes
		setRequestHeaders(null);
		setRequestBody(EMPTY_BODY);
	}

	public void setHttpVersion(Version httpVersion) {
		this.httpVersion = httpVersion;
		setHttpClient(null); // trigger a recreation of the connection if the http version ever changes
	}

	public void setTargetUid(String targetUid) {
		this.targetUid = targetUid;
		setHttpClient(null); // trigger a recreation of the connection if the dbUid changes
	}

	@JsonIgnore
	public String getResolvedTargetUid() throws Exception {
		return resolveProperties(getTargetUid());
	}

	public void setTargetPwd(String targetPwd) {
		this.targetPwd = targetPwd;
		setHttpClient(null); // trigger a recreation of the connection if the dbPwd changes
	}

	@JsonIgnore
	public String getResolvedTargetPwd() throws Exception {
		return resolveProperties(getTargetPwd());
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
		this.authorizationValue = null; // setting a new client also invalidates any old authentication header
		this.previousNonce = null;
		this.nonceCount = 0;
		setHttpRequest(null); // setting a new client invalidates old request
	}
	private synchronized void setHttpRequest(HttpRequest httpRequest) {
		this.httpRequest = httpRequest;
	}

	protected void updateRequestHeaders() {
		if (!initializing) {
			try {
				createRequest(); // this sets the requestHeaders as side-effect
			} catch (Exception ex) {
				log.error("Error in createRequest", ex.getMessage()); //$NON-NLS-1$
				this.requestHeaders = null;
			}
		}
	}

	protected void updateRequestBody() {
		if (!initializing) {
			updateResultingValue(getPayload(), (str) -> setRequestBody(str));
		}
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

	public HttpClient createClient() {

		HttpClient.Builder builder = HttpClient
			.newBuilder()
			.version(getHttpVersion())
			.followRedirects(getRedirectBehavior())
			.cookieHandler(createCookieHandler())
			.connectTimeout(Duration.ofSeconds(getTimeout()))
			;

		log.debug("creating HTTP client for: {}", this.resolvedURI); //$NON-NLS-1$
		if (this.resolvedURI.getScheme().equals("https")) { //$NON-NLS-1$
			// for SSL/TLS we need to jump through a few extra-loops:
			if (acceptableSSLVersions != null && acceptableSSLVersions.length > 0) {
				SSLParameters sslParameters = new SSLParameters();
				sslParameters.setProtocols(acceptableSSLVersions);
				builder.sslParameters(sslParameters);
			}

			KeyManager kms [] = null;
			TrustManager tms [] = null;

			KeyManagerFactory kmf =
				this.getRootNode()
					.getCertificateHandling()
					.getKeyManagerFactory(this.resolvedURI.getHost()); // extract only the host part from the URL
			if (kmf != null) {
				kms = kmf.getKeyManagers();
			}

			if (trustAllCertificates) {
				tms = new TrustManager[] { trustAllCerts };
				log.debug("Trusting ALL SSL certificates - available trust-managers are: {}", Arrays.asList(tms)); //$NON-NLS-1$
			}
			if (kms != null || tms != null) {
				SSLContext sslContext = null;
				try {
					sslContext = SSLContext.getInstance("TLS"); //$NON-NLS-1$
					sslContext.init(kms, tms, new java.security.SecureRandom());
					builder.sslContext(sslContext);
				} catch (Exception ex) {
					log.error("Error creating an SSLContext accepting ALL certificates - ignored", ex); //$NON-NLS-1$
				}
			}
		}
		if (!StringUtils.isEmpty(getTargetUid()) && !StringUtils.isEmpty(getTargetPwd())) {
			try {
				String resolvedUid = getResolvedTargetUid();
				String resolvedPwd = getResolvedTargetPwd();
				if (!StringUtils.isEmpty(resolvedUid) && !StringUtils.isEmpty(resolvedPwd)) {
					builder.authenticator(new Authenticator()
					{
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(resolvedUid, resolvedPwd.toCharArray());
						}
					});
				} else {
					log.error("Node '{}': uid and/or password resolved to blank - no authentication possible", getName()); //$NON-NLS-1$
				}
			} catch (Exception ex) {
				log.error(String.format("Node '%s': uid and/or password resolved to blank - no authentication possible", getName()), ex); //$NON-NLS-1$
			}
		} else {
			log.debug("uid and/or password defined as blank - no authentication."); //$NON-NLS-1$
		}

		// TODO implement proxy access
//		if (this.proxyHost != null) {
//			builder.proxy(ProxySelector.of(new InetSocketAddress(this.proxyHost, this.proxyPort)));
//			... what to do with proxy-uid and proxy-pwd?
//		}
		return builder.build();
	}

	@JsonIgnore
	protected CookieHandler createCookieHandler() {
		return new CookieManager(null, // CookieStore to be used by cookie manager. If null, cookie manager
		                         // will use a default one, which is an in-memory CookieStore implementation.
		                         // Note: must be per connection or else session-cookies will get mixed up!
		                         CookiePolicy.ACCEPT_ALL); // CookiePolicy instance to be used by cookie
		                                                   // manager as policy callback.
		                                                   // If null, ACCEPT_ORIGINAL_SERVER will be used.
	}

	@SuppressWarnings("resource")
	public void ensureValidClient() {
		if (getHttpClient() == null) {
			setHttpClient(createClient());
		}
	}

	/**
	 * As side-effect this sets resolvedURIs
	 * @return
	 * @throws Exception
	 */
	public HttpRequest createRequest() throws Exception {
		String resultingUrlResolved;
		try { // trying to get better error logs when the URL does not properly resolve:
			resultingUrlResolved = resultingUrlResolved();
		} catch (Exception ex) {
			log.error("createRequest: error resolving URL '{}': {}", this.getUrl(), ex.getMessage()); //$NON-NLS-1$
			throw ex;
		}
		try { // trying to get better error logs if this fails:
			this.resolvedURI = URI.create(resultingUrlResolved);
		} catch (Exception ex) {
			log.error("createRequest: error creating URI from '{}': {}", resultingUrlResolved, ex.getMessage()); //$NON-NLS-1$
			throw ex;
		}
		return createRequest(this.resolvedURI);
	}

	/*
	 * This method is parameterized with a uri (and not taking resolvedURI) because it is also used for redirections
	 */
	public HttpRequest createRequest(URI uri) throws Exception {
		HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
			.uri(uri)
			.timeout(Duration.ofSeconds(getTimeout()));
		if (getHeaders() != null) {
			Map<String, Object> map = KeyValuesConverter.convertStringToMap(resolveProperties(getHeaders()));
			map.forEach((key, value) -> httpRequestBuilder.setHeader(key, value.toString()));
		}
		if (this.authorizationValue == null && this.includeBasicAuthHeader) {
			createBasicAuthorizationValue();
		}
		if (this.authorizationValue != null) {
			httpRequestBuilder.setHeader("Authorization", this.authorizationValue); //$NON-NLS-1$
		}
		switch (getMethod()) {
		case GET:
			setRequestBody(EMPTY_BODY);
			httpRequestBuilder.GET();
			break;
		case POST: {
			setRequestBody(resolveProperties(getPayload()));
			byte[] body = getRequestBody();
			httpRequestBuilder.POST(BodyPublishers.ofByteArray(body != null ? body : EMPTY_BODY));
			break;
		}
		case PUT: {
			setRequestBody(resolveProperties(getPayload()));
			byte[] body = getRequestBody();
			httpRequestBuilder.POST(BodyPublishers.ofByteArray(body != null ? body : EMPTY_BODY));
			break;
		}
		case DELETE:
			setRequestBody(EMPTY_BODY);
			httpRequestBuilder.DELETE();
			break;
		default:
			throw new IllegalArgumentException("Unsupported HTTP method: " + getMethod()); //$NON-NLS-1$
		}
		aditionalRequestPreparations(httpRequestBuilder);
		return httpRequestBuilder.build(); // Note: returns an immutable request which can be sent multiple times.
	}

	public void ensureValidRequest() throws Exception {
		HttpRequest request = getHttpRequest();
		if (request == null) {
			setHttpRequest(createRequest());
			if (getHttpRequest() == null) { // we had such cases - beats me why
				throw new Exception("HttpRequest still null after just setting it!?!"); //$NON-NLS-1$
			}
		}
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

	@SuppressWarnings({"null", "resource"}) // the program flow guarantees that request is != null if response is <> null!
	@Override
	public void sendRequest() throws Exception {
		log.debug("sendRequest '{}':", getName()); //$NON-NLS-1$
		HttpClient client = null;
		HttpRequest request = null;
		HttpResponse<byte[]> response = null;
		try {
			setTimestamp(LocalDateTime.now());
			synchronized(this) {
				ensureValidClient();
				client = getHttpClient();
				ensureValidRequest();
				request = getHttpRequest();
			}
			int nrRedirections = 0;
			int nrAuthAttempts = 0;
			do {
				setRequestHeaders(request.headers());
				// signal response pending:
				setResponseStatusCode(-1);
				setResponseHeaders(null);
				setResponseBody("<no response received (yet)>".getBytes()); //$NON-NLS-1$
				logRequestValues();
				// ... before executing the actual request:
				long startTime = System.nanoTime();
				// send the request / receive response:
				response = client.send(request, BodyHandlers.ofByteArray());
				// ... and after executing the actual request:
				long callDuration = System.nanoTime() - startTime;
				setDuration(callDuration);
				log.trace("responseReceived for '{}' after {} microsecs.", getName(), callDuration/1000); //$NON-NLS-1$
				int statusCode = response.statusCode();
				processResponseReceived(response);
				if (statusCode == 401) { // Unauthorized
					if (++nrAuthAttempts > MAX_AUTH_ATTEMPTS) { // to avoid endless loops if our header is wrong or the server keeps responding with 401 responses...
						// a log entry is created in an outer catch
						throw new Exception("too many attempts (" + nrAuthAttempts + ") trying to create a valid authentication response"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					String authHeader = response.headers().firstValue("WWW-Authenticate").orElseGet(null); //$NON-NLS-1$
					if (authHeader != null) {
						log.debug("received response 401 with auth-header: '{}' - creating authorization request:", authHeader); //$NON-NLS-1$
						try {
							createAuthorizationValue(authHeader, request.method(), request.uri());
							request = createRequest(request.uri()); // creating a new request using same method and URI but including the new authorizationValue
							setHttpRequest(request);
							continue;
						} catch (Exception ex) {
							// a log entry is created in an outer catch
							throw new Exception(String.format("error creating new request for authentication header '%s'", authHeader), ex); //$NON-NLS-1$
						}
					} else {
						throw new Exception(String.format("status code 401 received but without indication re. expected authentication")); //$NON-NLS-1$
					}
				} else if (statusCode < 300 || statusCode >= 400) {
					processResponseReceived(response);
					break; // no redirection
				}
				// still here: we got a redirection - process it:
				if (++nrRedirections > MAX_REDIRECTIONS) {
					throw new Exception(String.format("Too many redirections: %d", nrRedirections)); //$NON-NLS-1$
				}
				String location = response.headers().firstValue("Location").orElseGet(null); //$NON-NLS-1$
				log.info("Request '{}' received redirection ({}) to '{}'", getName(), statusCode, location); //$NON-NLS-1$
				if (location == null || location.length() <= 0) {
					// a log entry is created in an outer catch
					throw new Exception(String.format("Received redirect-response %d but without a 'Location:'-header", statusCode)); //$NON-NLS-1$
				}
				try {
					request = createRequest(new URI(location));
					setHttpRequest(request);
				} catch (Exception ex) {
					String errMsg = String.format("error creating new request from received redirection location '%s' for '%s'", location, getName()); //$NON-NLS-1$
					throw new Exception(errMsg, ex);
				}
			} while (true); // exit is via break or exception...

		} catch (Throwable ex) {
			if (shortRequestLogEntries) {
				if (ex instanceof java.net.http.HttpConnectTimeoutException) {
					log.debug("exception executing '{}': {}", getName(), ExceptionUtils.exceptionRootCauseMsg(ex)); //$NON-NLS-1$
				} else {
					log.trace(String.format("exception executing '%s':", getName()), ex); //$NON-NLS-1$
				}
			} else if (log.isTraceEnabled()) { // log with stack trace - this is for tough nuts:
				log.trace(String.format("exception executing '%s':", getName()), ex); //$NON-NLS-1$
			} else {
				log.debug("exception executing '{}': {}", getName(), ExceptionUtils.exceptionCauseSummary(ex)); //$NON-NLS-1$
			}
			if (client != null) {
				client.close(); // we close the client (in case of an error, else we keep it).
			}

			// in case there *was* a response (e.g. a 401 or a redirection we leave it in the display for potential analysis)
			// setResponseStatusCode(-1);
			// setResponseHeaders(null);

			// instead we prefix the received body with the error message
			final byte[] oldContent = getResponseBody();
			final byte[] prefix     = ("Note: this prefix is an internal error message - not a response from the contacted server!\n" //$NON-NLS-1$
			                         + ExceptionUtils.exceptionCauseSummary(ex) + "\nlast response from server:\n---\n").getBytes(); //$NON-NLS-1$
			final byte[] errMsg = new byte[oldContent.length + prefix.length];
			System.arraycopy(prefix, 0, errMsg, 0, prefix.length);
			System.arraycopy(oldContent, 0, errMsg, prefix.length, oldContent.length);
			setResponseBody(errMsg);

			setDuration(NO_RESPONSE_DURATION); // signals an exception
			setState(State.FAILED);
			setRequestResult(getState().name() + '/' + ExceptionUtils.exceptionRootCauseMsg(ex));
			if (response != null) {
				throw new Exception(String.format("Error for '%s' processing response from '%s': %s", //$NON-NLS-1$
				                                  getName(), request.uri(), ex),
				                    ex);
			} else if (request != null) {
				throw new Exception(String.format("Error for '%s' sending request '%s': %s", //$NON-NLS-1$
				                                  getName(), request.uri(), ex),
				                    ex);
			} else {
				throw new Exception(String.format("Error for '%s' creating request: %s", //$NON-NLS-1$
				                                  getName(), ex) ,
				                    ex);
			}
		} finally {
			informOnPropertyChange(PROPERTYNAME_RESPONSE_COMPLETE, null, response);
		}
	}

	public void processResponseReceived(HttpResponse<byte[]> response) throws Exception {
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
			if (!StringUtils.isEmpty(acceptableCodes)) {
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
			if (res == State.OK) {
				ResultChecker checker = getResultChecker();
				if (checker != null && checker.getCondition() != null) {
					log.trace("checking {}:", checker); //$NON-NLS-1$
					res = getResultChecker().checkResult(this, responseBodyAsString());
				} else { // we only check for acceptableReturnCodes - might want to check that we did...y
					log.debug("no check defined."); //$NON-NLS-1$
				}
			}
		}
		log.debug("deriveState '{}': {} - checking:{} (received:'{}', acceptable:'{}', checker:{})", name, res, isCheckResults(), getResponseStatusCode(), getAcceptableReturnCodes(), getResultChecker()); //$NON-NLS-1$
		setState(res);
	}

	@SuppressWarnings("resource")
	public void logRequestValues() throws IOException {
		if (log.isDebugEnabled()) {
			HttpRequest request = getHttpRequest();
			Optional<BodyPublisher> optionalBody = request.bodyPublisher();
			log.debug("{}-httpRequest to '{}' / headers: {} / body: {} bytes / cookies: {}", //$NON-NLS-1$
			         request.method(), request.uri(), request.headers().map(),
			          (optionalBody.isEmpty() ? 0 : getRequestBody().length),
			          getHttpClient().cookieHandler().get().get(request.uri(), Collections.emptyMap()));

			if (log.isTraceEnabled() && !optionalBody.isEmpty()) {
				log.trace("body: \"{}\"", requestBodyAsString()); //$NON-NLS-1$
			}
		}
	}

	public void logResponseValues() {
		log.debug("response: status={} / headers:{} / body: {} bytes", //$NON-NLS-1$
		          getResponseStatusCode(), getResponseHeaders().map(), getResponseBody().length);
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

	public String requestBodyAsString() {
		return convertBodyToString(getRequestBody(), extractRequestCharset());
	}

	enum Encodings {
		ZIP,
		GZIP,
		DEFLATE
	}
	public String responseBodyAsString() {
		byte[] body = getResponseBody();
		Charset charset = extractResponseCharset(body);
		HttpHeaders respHdrs = getResponseHeaders();
		if (respHdrs != null) {
			List<String> encodingList = respHdrs.map().get("content-encoding"); //$NON-NLS-1$
			if (encodingList != null) {
				Encodings encoding = null;
				try {
					if (encodingList.contains("zip")) { //$NON-NLS-1$
						encoding = Encodings.ZIP;
					} else if (encodingList.contains("gzip")) { //$NON-NLS-1$
						encoding = Encodings.GZIP;
					} else if (encodingList.contains("deflate")) { //$NON-NLS-1$
						encoding = Encodings.DEFLATE;
					}
					if (encoding != null) {
						body = unzip(body, encoding);
						if (charset == DEFAULT_HTTP_CHARSET) { // the charset might have been specified in the just decoded body, so we need to try again:
							charset = extractResponseCharset(body);
						}
						log.debug("charset {}: {} {}", this.resolvedURI, charset, charset == DEFAULT_HTTP_CHARSET ? "(default)" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						return "[>> Decompressed (using " + encoding+ "): <<]\n" + convertBodyToString(body, charset); //$NON-NLS-1$ //$NON-NLS-2$
					}
				} catch (IOException ex) {
					String errMsg = "Error inflating response-body (using " + encoding + "): " + ExceptionUtils.exceptionCauseSummary(ex); //$NON-NLS-1$ //$NON-NLS-2$
					log.error(errMsg);
					return errMsg + '\n' + convertBodyToString(body, charset);
				}
			}
		}
		log.debug("charset {}: {}", this.resolvedURI, charset); //$NON-NLS-1$
		return convertBodyToString(body, charset);
	}
	protected byte[] unzip(byte[] bytes, Encodings decompression) throws IOException {
		if (bytes == null) return null;
		if (bytes.length == 0) return new byte[0];
		try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		     InflaterInputStream inflaterInputStream =
		    	switch (decompression) {
		    	case ZIP -> new ZipInputStream(byteArrayInputStream);
		    	case GZIP -> new GZIPInputStream(byteArrayInputStream);
		    	case DEFLATE ->  new InflaterInputStream(byteArrayInputStream);
		    	default -> throw new IllegalArgumentException("Unknown decompression: " + decompression); //$NON-NLS-1$
		     };
		     ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			int read;
			while ((read = inflaterInputStream.read()) != -1) {
				byteArrayOutputStream.write(read);
			}
			return byteArrayOutputStream.toByteArray();
		}
	}
	private static String convertBodyToString(final byte[] body, Charset charset) {
		return (body != null ? new String(body, charset) : VALUE_UNDEFINED);
	}

	/** extract character set from httpRequest headers - if there is any...
	 * @throws Exception */
	protected Charset extractRequestCharset() {
		HttpHeaders requHdrs = getRequestHeaders();
		return extractCharset(requHdrs != null ? requHdrs.map() : null, null, "request"); //$NON-NLS-1$
	}

	/** extract character set from response headers - if there is any... */
	protected Charset extractResponseCharset(final byte[] body) {
		HttpHeaders respHdrs = getResponseHeaders();
		return extractCharset(respHdrs != null ? respHdrs.map() : null, body, "response"); //$NON-NLS-1$
	}
	/** extract character set from Content-Type header - if there is any... */
	protected Charset extractCharset(final Map<String, List<String>> map, final byte[] body, final String logSnippet) {
		// 1.: attempt to extract the charset from the ContentType-header:
		Charset contentTypeCharset =
			(Charset)KeyValuesConverter.extractValueFragment(map,
			                                                 CONTENT_TYPE_HEADER,
			                                                 VALUES_FRAGMENT_SEPARATOR,
			                                                 CONTENT_TYPE_CHARSET_PATTERN,
			                                                 CONTENT_TYPE_CHARSET_GROUP_NR,
			                                                 (str) -> convertNameToCharset(str),
			                                                 null);
		if (contentTypeCharset != null) {
			log.trace("extractCharset: found '{}' character set in content-header: '{}'", logSnippet, contentTypeCharset); //$NON-NLS-1$
			return contentTypeCharset;
		}
		// 2. for <!DOCTYPE html>: look for "<meta charSet="..."/>" present in the HTML header (<head>...</head>):
		if (body != null && body.length > DOCTYPE_HTML.length()+32) { // +32: minimal length of a minimal HTML-header containing a charset string like: '<head><meta charset="X"/></head>'
			// To scan for HTML meta-data we need to convert the header-bytes to String even though we don't
			// know the Charset, yet. But HTML header stuff (at the least meta-data part) should be in US_ASCII only!
			// Limiting the length of the converted string to avoid memory overflow (+ possible security issues).
			// We assume/hope that the meta-data item we are seeking is within that length.
			String bodyString= new String(body, 0, Math.min(body.length, 5000)).toLowerCase();
			if (bodyString.startsWith(DOCTYPE_HTML)) {
				Matcher m = META_CHARSET_PATTERN.matcher(bodyString);
				if (m.matches()) {
					contentTypeCharset = convertNameToCharset(m.group(META_CHARSET_GROUP_NR));
					if (contentTypeCharset != null) {
						log.trace("extractCharset: found '{}' character set in meta header: '{}'", logSnippet, contentTypeCharset); //$NON-NLS-1$
						return contentTypeCharset;
					}
				} else if (log.isTraceEnabled()) {
					log.trace("no 'meta charset=...' found: '{}'", bodyString.substring(0, Math.min(5000, bodyString.length()))); //$NON-NLS-1$
				}
			} else {
				log.trace("no doctype html."); //$NON-NLS-1$
			}
		} else {
			log.trace("{}-body too short ({}) to contain a doctype specification.", logSnippet, (body != null ? body.length : "empty")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// 3. if no (legal) charset indication was found: we assume the default HTTP charset:
		log.trace("extractCharset: found no '{}' character set -> assuming default charset '{}'", logSnippet, DEFAULT_HTTP_CHARSET); //$NON-NLS-1$
		return DEFAULT_HTTP_CHARSET;
	}

	Charset convertNameToCharset(String str) {
		String charsetName = str.toUpperCase();
		if (Charset.isSupported(charsetName)) {
			Charset cs = Charset.forName(charsetName);
			log.trace("convertNameToCharset: found character set: '{}'", cs); //$NON-NLS-1$
			return cs;
		} else {
			log.warn("convertNameToCharset: charset '{}' is not supported", charsetName); //$NON-NLS-1$
			// throw new Exception("charset '" + str + "' not supported"); // we rather warn and continue...
			return null;
		}
	}

	private void createBasicAuthorizationValue() throws Exception {
		createAuthorizationValue("Basic", null, null); //$NON-NLS-1$
	}

	/**
	 * For Digest-authentication we are dealing with this string as defined in
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.2.1">https://datatracker.ietf.org/doc/html/rfc2617#section-3.2.1</a>
	 * and we need to create a response as described in
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.2.2">https://datatracker.ietf.org/doc/html/rfc2617 section-3.2.2</a>:
	 */

	private void createAuthorizationValue(final String authHeader,
	                                      final String requestMethod,
	                                      final URI uri) throws Exception {
		try {
			this.authorizationValue =
				HTTP_Authorization.createAuthorizationValue(authHeader,
				                                            getResolvedTargetUid(),
				                                            getResolvedTargetPwd(),
				                                            requestMethod,
				                                            getRequestBody(),
				                                            uri.getPath(),
				                                            (nonce) -> generateNonceCount(nonce),
				                                            () -> HTTP_Authorization.createCNonce(8)
				                                           );
			log.debug("response auth-header: '{}'", this.authorizationValue); //$NON-NLS-1$
		} catch (Exception ex) {
			setResponseBody(ex.getMessage().getBytes());
			throw ex;
		}
	}

	private String generateNonceCount(final String nonce) {
		if (nonce.equals(this.previousNonce)) { // same nonce reused:
			++this.nonceCount; // increment the counter: "00000001" --> "00000002", etc.
		} else {
			this.previousNonce = nonce; //  memorize this new nonce
			this.nonceCount = 1; // first use of a new nonce: --> nc="00000001"
		}
		return String.format("%08x", this.nonceCount); //$NON-NLS-1$
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
