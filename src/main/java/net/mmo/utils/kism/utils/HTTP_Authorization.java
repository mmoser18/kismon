/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.HexFormat;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.connections.HTTPConnection;

/**
 * separated this into an own class to keep HTTPConnection manageable.
 * The method generates Authorization headers for Basic and Digest authentication (according to RFC-7616).
 */
@SuppressWarnings("javadoc")
@Slf4j
public class HTTP_Authorization
{
	/* Note: these must be listed in order of preference. If multiple options are
	 * offered by the server the code below picks the first matching entry. */
	private static final String SUPPORTED_HASH_ALGOS[] =  { "SHA-256", "SHA-256-sess", "SHA-512-256", "SHA-512-256-sess", "MD5", "MD5-sess"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	private static final String DEFAULT_HASH_ALGO =  "MD5"; //$NON-NLS-1$
	public static final String AUTH_SEP = ","; //$NON-NLS-1$
	public static final String MD5_SEP = ":"; //$NON-NLS-1$


	/** separated to allow simpler unit-testing:
	 * This method generates an Authorization header value according to RFC-7616
	 * @param authHeader
	 * @param uid the user's id
	 * @param pwd the users credential
	 * @param requestMethod
	 * @param requestBody
	 * @param uri
	 * @param nonceCountGen
	 * @return the generated authorization response header
	 * @throws Exception
	 */
	public static String createAuthorizationValue(final String authHeader,
	                                              final String uid,
	                                              final String pwd,
	                                              final String requestMethod,
	                                              final byte[] requestBody,
	                                              final URI uri,
	                                              final Function<String, String> nonceCountGen,
	                                              final Supplier<String> cnonceGen) throws Exception {

		if (!StringUtils.isEmpty(uid)) { // the pwd can be empty but the uid must not be!
			final String password = (pwd != null ? pwd : ""); //$NON-NLS-1$
			if (authHeader.startsWith("Basic")) { // Basic access authentication required //$NON-NLS-1$
				return "Basic " //$NON-NLS-1$
				       + new String(Base64.getEncoder().encode((uid
				                                               + ":" //$NON-NLS-1$
				                                               + password
				                                               ).getBytes(StandardCharsets.UTF_8)),
				                    HTTPConnection.DEFAULT_HTTP_CHARSET);
			} else if (authHeader.startsWith("Digest")) { // Digest access authentication required //$NON-NLS-1$
				// According to RFC 7616: the digest-uri value should be the complete request-target including query parameters.
				// Using getRawPath() and getRawQuery() avoids potential double-encoding issues.
				final String rawQuery = uri.getRawQuery();
				final String rawPath = uri.getRawPath();
				final String path = (rawPath != null && rawPath.length() > 0 ? rawPath : "/") //$NON-NLS-1$
				                  + (rawQuery != null ? "?" + rawQuery : ""); //$NON-NLS-1$ //$NON-NLS-2$

				log.debug("uri:'" + path + "', authHeader :'" + authHeader + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				final String realm = extractValue(authHeader, "realm"); //$NON-NLS-1$
				final String domains = extractValue(authHeader, "domain"); //$NON-NLS-1$
				final String nonce = extractValue(authHeader, "nonce"); //$NON-NLS-1$
				final String opaque = extractValue(authHeader, "opaque"); //$NON-NLS-1$
				final String qop = extractValue(authHeader, "qop"); //$NON-NLS-1$
				final String algorithm = extractValue(authHeader, "algorithm"); //$NON-NLS-1$
				final String stale = extractValue(authHeader, "stale"); //$NON-NLS-1$
				final String charset = extractValue(authHeader, "charset"); //$NON-NLS-1$
				final String userhash = extractValue(authHeader, "userhash"); //$NON-NLS-1$

				log.debug("extracted auth-header values: " //$NON-NLS-1$
				          + "realm:'" + realm + "'" //$NON-NLS-1$ //$NON-NLS-2$
				          + ", domain:'" + domains + "'" //$NON-NLS-1$ //$NON-NLS-2$
				          + ", nonce:'" + nonce + "'" //$NON-NLS-1$ //$NON-NLS-2$
				          + ", opaque:'" + opaque + "'" //$NON-NLS-1$ //$NON-NLS-2$
				          + ", qop:'" + qop + "'" //$NON-NLS-1$ //$NON-NLS-2$
				          + ", algorithm:'" + algorithm + "'" //$NON-NLS-1$ //$NON-NLS-2$
				          + ", stale:'" + stale + "'" //$NON-NLS-1$ //$NON-NLS-2$
				          + ", charset:'" + charset + "'" //$NON-NLS-1$ //$NON-NLS-2$
				          + ", userhash:'" + userhash + "'" //$NON-NLS-1$ //$NON-NLS-2$
				         );

				assertProvided("realm", realm, authHeader); //$NON-NLS-1$
				assertProvided("nonce", nonce, authHeader); //$NON-NLS-1$
				final Charset responseCharset;
				if (charset != null) {
					if (charset.equalsIgnoreCase("UTF-8")) { //$NON-NLS-1$
						responseCharset =  StandardCharsets.UTF_8;
					} else {
						log.warn("Invalid charset value in authentication header: '{}' - only UTF-8 is allowed", charset); //$NON-NLS-1$
						responseCharset = StandardCharsets.ISO_8859_1;
					}
				} else { // RFC 2617: default is ISO-8859-1 if charset not specified or not UTF-8
					responseCharset = StandardCharsets.ISO_8859_1;
				}

				final String algorithmUsed;
				String hashAlgo;
				if (algorithm == null) {
					hashAlgo = algorithmUsed = DEFAULT_HASH_ALGO;
				} else if ((algorithmUsed = directiveContains(algorithm, SUPPORTED_HASH_ALGOS)) != null) {
					final int pos = algorithmUsed.indexOf("-sess"); //$NON-NLS-1$
					hashAlgo = (pos > 0 ? algorithmUsed.substring(0, pos) : algorithmUsed);
					// Stupidly Java names some algorithms as "SHA-x/y" (i.e. with a slash between the numbers)
					// while the RFC names the same as "SHA-x-y" (i.e. with a dash between numbers). Duuuh ||-(
					// We thus need to map these:
					hashAlgo = hashAlgo.replaceFirst("SHA\\-(\\d*)\\-(\\d*)", "SHA\\-$1/$2"); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					throw new Exception("Unexpected authentication hash algorithm(s) '" + algorithm + "' encountered in directive '" + algorithm + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}

				final MessageDigest md;
				try {
					md = MessageDigest.getInstance(hashAlgo);
				} catch (Exception ex) {
					throw new Exception("Authentication hash algorithm '" + hashAlgo + "' not supported - available algos are: " + Security.getAlgorithms("MessageDigest"), ex); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}

				String qopUsed = null;
				String cnonce = null;
				String nc = null;

				if (qop != null) {
					qopUsed = directiveContains(qop, "auth", "auth-int"); // //$NON-NLS-1$ //$NON-NLS-2$
					if (qopUsed == null) {
						throw new Exception("Server offered unsupported qop(s): '" + qop + "'"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					cnonce = cnonceGen.get();
					nc = nonceCountGen.apply(nonce);
					log.trace("generateNonceCount: {}", nc); //$NON-NLS-1$

					if ("auth-int".equals(qopUsed)) { //$NON-NLS-1$
						log.warn("qop-variant \"auth-int\" not yet implemented!"); //$NON-NLS-1$
					}
				}
				if (qop == null && algorithmUsed.endsWith("-sess")) { //$NON-NLS-1$
					throw new Exception(String.format("Digest authentication requested with \"...-sess\" algorithm but no \"qop\"-directive specified in received header: '%s'", authHeader)); //$NON-NLS-1$
				}

				final String username;
				boolean usedUserhash = false;
				boolean usernameQuotable = true;
				if (Boolean.parseBoolean(userhash)) { // parseBoolean() also takes care of null-check
					username = H(md, uid + MD5_SEP + realm, responseCharset);
					usedUserhash = true;
				} else if (uid.contains(":") || uid.contains("\"")) { // we can't send that as quoted string //$NON-NLS-1$ //$NON-NLS-2$
					usernameQuotable = false;
					username = urlEncode(uid);
				} else {
					username = uid;
				}

				String HA1 = H(md,
				               algorithmUsed.endsWith("-sess") //$NON-NLS-1$
				               ? H(md, uid + MD5_SEP + realm + MD5_SEP + password, responseCharset) + MD5_SEP + nonce + MD5_SEP + cnonce
				               : uid + MD5_SEP + realm + MD5_SEP + password,
				               responseCharset);

				String HA2 = H(md,
				               "auth-int".equals(qopUsed) //$NON-NLS-1$
				               ? requestMethod.toUpperCase() + MD5_SEP + path + MD5_SEP + H(md, requestBody)
				               : requestMethod.toUpperCase() + MD5_SEP + path,
				               responseCharset
				              );

				final String response = (qopUsed != null)
				                         ? KD(md, HA1, nonce + MD5_SEP + nc + MD5_SEP + cnonce + MD5_SEP + qopUsed + MD5_SEP + HA2, responseCharset)
				                         : KD(md, HA1, nonce + MD5_SEP + HA2, responseCharset);

				// creating response string strictly following the order in https://datatracker.ietf.org/doc/html/rfc2617:
				final String authValue = "Digest" //$NON-NLS-1$
				       + " username" + (usernameQuotable  //$NON-NLS-1$
				    				   ? "=\"" + username + "\"" //$NON-NLS-1$ //$NON-NLS-2$
				    				   : "*=UTF-8''" + username //$NON-NLS-1$
				    				   )
				       + AUTH_SEP + "realm=\"" + realm + "\"" //$NON-NLS-1$ //$NON-NLS-2$
				       + AUTH_SEP + "nonce=\"" + nonce +"\"" //$NON-NLS-1$ //$NON-NLS-2$
				       + AUTH_SEP + "uri=\"" + path + "\"" //$NON-NLS-1$ //$NON-NLS-2$
				       + AUTH_SEP + "response=\"" + response + "\"" //$NON-NLS-1$ //$NON-NLS-2$
				       + (algorithm != null // we only include the algorithm in the response if it had been sent by the server
				         ? AUTH_SEP + "algorithm=" + algorithmUsed // unquoted! //$NON-NLS-1$
				         : "") //$NON-NLS-1$
				       + (qop != null // cnonce is only to be added if server provided a qop in its response
				         ? AUTH_SEP + "cnonce=\"" + cnonce + "\"" //$NON-NLS-1$ //$NON-NLS-2$
				         : "") //$NON-NLS-1$
				       + (opaque != null
				         ? AUTH_SEP + "opaque=\"" + opaque + "\"" //$NON-NLS-1$ //$NON-NLS-2$
				         : "") //$NON-NLS-1$
				       + (qop != null // qop and nc are only to be added if server provided a qop in its response
				         ? AUTH_SEP + "qop=" + qopUsed // unquoted! //$NON-NLS-1$
				           + AUTH_SEP + "nc=" + nc // unquoted! //$NON-NLS-1$
				         : "") //$NON-NLS-1$
				       + (usedUserhash
				          ? AUTH_SEP + "userhash=true" // unquoted! //$NON-NLS-1$
				          : "") //$NON-NLS-1$
				       + (charset != null && charset.equalsIgnoreCase("UTF-8") //$NON-NLS-1$
				          ? AUTH_SEP + "charset=utf-8" // unquoted, lowercase per RFC 7616 //$NON-NLS-1$
				          : "") //$NON-NLS-1$
				       // auth-param - "Any unrecognized directive MUST be ignored."!
				       ;
				log.debug("generated auth value: '{}'.", authValue); //$NON-NLS-1$
				return authValue;
			} else {
				throw new Exception(String.format("unsupported authentication method: '%s'", authHeader)); //$NON-NLS-1$
			}
// TODO implement token-based authentication
//		} else if (authHeader.toLowerCase().startsWith("token")) { //$NON-NLS-1$
//			final String token = "???"; //$NON-NLS-1$
//			return "token=\"" + token + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			throw new Exception(String.format("authentication requested, but no uid and/or pwd specified: '%s'", authHeader)); //$NON-NLS-1$
		}
	}

	/**
	 * We URL-encode a string with the addition that ' ' is encoded as "%20" instead of '+'
	 * example from the specs: "Jäsøn Doe" -> J%C3%A4s%C3%B8n%20Doe
	 * @param str
	 * @return the URL-encoded string
	 */
	public static String urlEncode(String str) {
		return URLEncoder.encode(str, StandardCharsets.UTF_8).replace("+", "%20"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	static void assertProvided(final String directiveName, final String value, final String authHeader) throws Exception {
		if (value == null || value.isBlank()) {
			throw new Exception(String.format("Digest authentication requested but no '%s'-directive specified in received header: '%s'", directiveName, authHeader)); //$NON-NLS-1$
		}
	}

	/**
	 * Certain digest directives can contain comma-separates lists (e.g. qop).
	 * This method checks whether a specific value is contained in it.
	 * @param directiveList
	 * @param patterns
	 * @return whether a specific value is contained in the options.
	 */
	public static String directiveContains(final String directiveList, final String ... patterns) {
		if (directiveList == null || directiveList.isBlank()) return null;
		for (String component: directiveList.split(",")) { //$NON-NLS-1$
			final String candidate = component.trim();
			for (String pattern: patterns) {
				if (candidate.equalsIgnoreCase(pattern)) {
					log.debug("choice selected: '{}'", pattern); //$NON-NLS-1$
					return pattern;
				}
			}
		}
		return null;
	}


	public static String createCNonce(final int nrBytes) {
		final byte[] bytes = new byte[nrBytes];
		new SecureRandom().nextBytes(bytes);

		final StringBuilder result = new StringBuilder();
		for (byte temp: bytes) {
			result.append(String.format("%02x", temp)); //$NON-NLS-1$
		}
		final String res = result.toString();
		log.trace("createCNonce: '{}'", res); //$NON-NLS-1$
		return res;
	}
	/**
	 * This methods calculates an 16-byte hash of the input string using the algorithm chosen and
	 * returns it in lowercase hexadecimal representation, i.e. as a 32 character string.
	 * @param input
	 * @return the hash value
	 */
	public static String H(final MessageDigest md, final String input, final Charset charset) {
		final byte[] bytes = input.getBytes(charset != null ? charset : StandardCharsets.ISO_8859_1);
		// log.debug("Hashing bytes: {}", HexFormat.of().withDelimiter(" ").formatHex(bytes)); //$NON-NLS-1$ //$NON-NLS-2$
		final String res = H(md, bytes);
		log.debug("{}('{}') = '{}'", md.getAlgorithm(), input, res); //$NON-NLS-1$
		return res;
	}
	static String H(final MessageDigest md, final byte[] bytes) {
		md.reset();
		md.update(bytes);
		final String res = HexFormat.of().withLowerCase().formatHex(md.digest());
		// log.trace("{}('{}') = '{}'", md.getAlgorithm(), HexFormat.of().withUpperCase().formatHex(bytes), res); //$NON-NLS-1$
		return res;
	}

	/* for some reason they differentiated between KD and H in the specs, so
	 * I kept it that way even though it makes very little sense */
	static String KD(final MessageDigest md, final String secret, final String data, final Charset charset) {
		return H(md, secret + MD5_SEP + data, charset);
	}

	/**
	 * Extract a given directive:
	 * @param headerValue
	 * @param directive the received directive-options
	 * @return the extracted directive
	 */
	public static String extractValue(final String headerValue, final String directive) {
		final String searchString = directive + "="; //$NON-NLS-1$
		int valueStartPos = headerValue.toLowerCase().indexOf(searchString); // directive names are to be handled case insensitive!
		if (valueStartPos >= 0) {
			valueStartPos += searchString.length();
			final boolean quoted = headerValue.charAt(valueStartPos) == '"';
			int valueEndPos;
			if (quoted) {
				valueStartPos++;
				valueEndPos = headerValue.indexOf("\"", valueStartPos); //$NON-NLS-1$
			} else {
				valueEndPos = headerValue.indexOf(",", valueStartPos); //$NON-NLS-1$
				if (valueEndPos <= 0) valueEndPos = headerValue.length();
			}
			if (valueEndPos > valueStartPos) {
				final String value = headerValue.substring(valueStartPos, valueEndPos);
				log.trace("value for directive '{}': '{}'", directive, value); //$NON-NLS-1$\
				return value.isBlank() ? null : value; // unify empty string to null
			} else {
				log.info("no end-quote found for directive '{}' in responseString '{}'", directive, headerValue); //$NON-NLS-1$\
			}
		} else {
			log.trace("no value for directive '{}'", directive); //$NON-NLS-1$
		}
		return null;
	}
}
