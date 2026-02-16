/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes.connections;

import static net.mmo.utils.kism.utils.HTTP_Authorization.AUTH_SEP;
import static net.mmo.utils.kism.utils.HTTP_Authorization.MD5_SEP;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.utils.HTTP_Authorization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"nls", "javadoc"})
@Slf4j
public class HTTPConnectionTest
{
	@Test
	void ExtractValue() {
		final String authHeader = "Digest realm=\"Some realm\", nonce=\"2955cc6154daf65f8259755919c0041e\", qop=\"auth\", opaque=\"5ccc069c403ebaf9f0171e9517f40e41\", stale=\"FALSE\"";
		String extracted;

		extracted = HTTP_Authorization.extractValue(authHeader, "realm");
		Assertions.assertEquals("Some realm", extracted);

		extracted = HTTP_Authorization.extractValue(authHeader, "qop");
		Assertions.assertEquals("auth", extracted);

		extracted = HTTP_Authorization.extractValue(authHeader, "stale");
		Assertions.assertEquals("FALSE", extracted);
	}

	@Test
	void DirectiveContains() {
		String directive;
		directive = HTTP_Authorization.directiveContains(null, "auth");
		Assertions.assertEquals(null, directive);

		directive = HTTP_Authorization.directiveContains("foobar", "auth");
		Assertions.assertEquals(null, directive);

		directive = HTTP_Authorization.directiveContains("auth,auth-int", "auth");
		Assertions.assertEquals("auth", directive);
	}

	@Test
	void MD5() throws Exception {
		// example taken from: https://en.wikipedia.org/wiki/Digest_access_authentication

		final MessageDigest md = MessageDigest.getInstance("MD5");

		final String username = "Mufasa";
		final String realm = "testrealm@host.com";
		final String password = "Circle Of Life";

		final String ha1  = HTTP_Authorization.H(md,
		                                         username + MD5_SEP + realm + MD5_SEP + password,
		                                         HTTPConnection.DEFAULT_HTTP_CHARSET);
		Assertions.assertEquals("939e7578ed9e3c518a452acee763bce9", ha1);

		final String method = "GET";
		final String uri = "/dir/index.html";
		final String ha2 = HTTP_Authorization.H(md,
		                                        method + MD5_SEP + uri,
	                                            HTTPConnection.DEFAULT_HTTP_CHARSET);
		Assertions.assertEquals("39aff3a2bab6126f332b942af96d3366", ha2);

		final String nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093";
		final String qop = "auth";
		final String nc = "00000001";
		final String cnonce = "0a4f113b";

		final String exampleResponse = HTTP_Authorization.H(md,
		                                                    ha1 + MD5_SEP + nonce + MD5_SEP + nc + MD5_SEP + cnonce + MD5_SEP + qop + MD5_SEP + ha2,
				                                            HTTPConnection.DEFAULT_HTTP_CHARSET
		                                                   );
		Assertions.assertEquals("6629fae49393a05397450978507c4ef1", exampleResponse);
	}

	@Test
	void MD5_2() throws Exception {
		// Example from: https://jigsaw.w3.org/HTTP/Digest/

		final MessageDigest md = MessageDigest.getInstance("MD5");

		final String username = "guest";
		final String realm = "test";
		final String password = "guest";

		final String ha1  = HTTP_Authorization.H(md,
		                                         username + MD5_SEP + realm + MD5_SEP + password,
		                                         HTTPConnection.DEFAULT_HTTP_CHARSET);
		Assertions.assertEquals("871a43cd67a0196cf4f801935973deb1", ha1);

		final String method = "GET";
		final String uri = "/HTTP/Digest";
		final String ha2 = HTTP_Authorization.H(md,
		                                        method + MD5_SEP + uri,
	                                            HTTPConnection.DEFAULT_HTTP_CHARSET);
		Assertions.assertEquals("14d9f2c023f4c72d2765dfcca0128c91", ha2);

		final String nonce = "039bf2e77159808b6bbfb56ea05a1c6a";
		final String exampleResponse = HTTP_Authorization.H(md,
		                                                    ha1 + MD5_SEP + nonce + MD5_SEP + ha2,
				                                            HTTPConnection.DEFAULT_HTTP_CHARSET
		                                                   );
		Assertions.assertEquals("ed5985e8d25f79ca435e9366caf81320", exampleResponse);
	}

	@Test
	void MD5_3() throws Exception {
		// Example from: https://jigsaw.w3.org/HTTP/Digest/

		final MessageDigest md = MessageDigest.getInstance("MD5");

		final String username = "guest";
		final String realm = "test";
		final String password = "guest";

		final String ha1  = HTTP_Authorization.H(md,
		                                         username + MD5_SEP + realm + MD5_SEP + password,
		                                         HTTPConnection.DEFAULT_HTTP_CHARSET);
		Assertions.assertEquals("871a43cd67a0196cf4f801935973deb1", ha1);

		final String method = "GET";
		final String uri = "/HTTP/Digest";
		final String ha2 = HTTP_Authorization.H(md,
		                                        method + MD5_SEP + uri,
	                                            HTTPConnection.DEFAULT_HTTP_CHARSET);
		Assertions.assertEquals("14d9f2c023f4c72d2765dfcca0128c91", ha2);

		// final String nonce = "039bf2e77159808b6bbfb56ea05a1c6a";
		final String nonce = "fb7101f5e109ce8c24153124dcf2d545";

		final String exampleResponse = HTTP_Authorization.H(md,
		                                                    ha1 + MD5_SEP + nonce + MD5_SEP + ha2,
				                                            HTTPConnection.DEFAULT_HTTP_CHARSET
		                                                   );
		// Assertions.assertEquals("ed5985e8d25f79ca435e9366caf81320", exampleResponse);
		Assertions.assertEquals("0059a2e36926b5e3df69b47e4391efdf", exampleResponse);
	}

	@Test
	void MD5_4() throws Exception {
		// Example from: Dishwasher:
		// WWW-Authenticate: Digest realm="AdoraDish V2000", nonce="6b486cf5dc34d2ddb87295cbd279c5a7", qop="auth", opaque="5ccc069c403ebaf9f0171e9517f40e41", stale="FALSE"
		// Authorization: Digest username="mmo",realm="AdoraDish V2000",nonce="6b486cf5dc34d2ddb87295cbd279c5a7",uri="/",cnonce="2f996bd4a3093d7b5fc3e79817378583",nc=00000001,response="498a0ce1d0420ebbdbef446912817a79",qop="auth",opaque="5ccc069c403ebaf9f0171e9517f40e41"


		final MessageDigest md = MessageDigest.getInstance("MD5");

		final String username = "mmo";
		final String realm = "AdoraDish V2000";
		final String password = "zI3EVaMOsT6P5k";

		final String ha1  = HTTP_Authorization.H(md,
		                                         username + MD5_SEP + realm + MD5_SEP + password,
		                                         HTTPConnection.DEFAULT_HTTP_CHARSET);
		Assertions.assertEquals("49f5db5e94977dd05b5cad34f3bec2e9", ha1);

		final String method = "GET";
		final String uri = "/";
		final String ha2 = HTTP_Authorization.H(md,
		                                        method + MD5_SEP + uri,
	                                            HTTPConnection.DEFAULT_HTTP_CHARSET);
		Assertions.assertEquals("71998c64aea37ae77020c49c00f73fa8", ha2);

		final String nonce = "6b486cf5dc34d2ddb87295cbd279c5a7";
		final String nc = "00000001";
		final String cnonce = "2f996bd4a3093d7b5fc3e79817378583";
		final String qop = "auth";

		final String exampleResponse = HTTP_Authorization.H(md,
		                                                    ha1 + MD5_SEP + nonce + MD5_SEP + nc + MD5_SEP + cnonce + MD5_SEP + qop + MD5_SEP + ha2,
				                                            HTTPConnection.DEFAULT_HTTP_CHARSET
		                                                   );
		Assertions.assertEquals("498a0ce1d0420ebbdbef446912817a79", exampleResponse);
	}



	/*
	 * example: "Jäsøn Doe" -> "J%C3%A4s%C3%B8n%20Doe" //$NON-NLS-1$
	 */
	@Test
	void urlEncode() {
		final String input = "Jäsøn Doe";
		final String expected = "J%C3%A4s%C3%B8n%20Doe";

		Assertions.assertEquals(expected, HTTP_Authorization.urlEncode(input));
	}

	@Test
	void createAuthenticationValueWithoutQop() throws Exception {
		final String username  = "Mufasa";
		final String realm     = "testrealm@host.com";
		final String nonce     = "dcd98b7102dd2f0e8b11d0f600bfb0c093";
		final String opaque    = "5ccc069c403ebaf9f0171e9517f40e41";

		final String password  = "Circle Of Life";
		final String method    = "GET";
		final String uri       = "/dir/index.html";

		final String response  = "670fd8c2df070c60b045671b8b24ff02";

		final String receivedAuthHeader =
			"Digest "
			           + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			;

		final String expectedHeader =
			"Digest "
			           + "username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			;
		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            null,
			                                            null);

		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	@Test
	void createAuthenticationValueWithQop() throws Exception {
		final String username   = "Mufasa";
		final String realm      = "testrealm@host.com";
		final String nonce      = "dcd98b7102dd2f0e8b11d0f600bfb0c093";
		final String opaque     = "5ccc069c403ebaf9f0171e9517f40e41";
		final String qopOptions = "auth,auth-int";

		final String password   = "Circle Of Life";
		final String method     = "GET";
		final String uri        = "/dir/index.html";
		final String qopChosen  = "auth";
		final String nc         = "00000001";
		final String cnonce     = "0a4f113b";

		final String response    = "6629fae49393a05397450978507c4ef1";

		final String receivedAuthHeader =
			"Digest "
			           + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=\"" + qopOptions + "\""
			;

		final String expectedHeader =
			"Digest "
			           + "username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "cnonce=\"" + cnonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qopChosen
			+ AUTH_SEP + "nc=" + nc
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            (str) -> nc,
			                                            () -> cnonce);

		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	@Test
	void createAuthenticationValueWithQop2() throws Exception {
		final String username   = "user";
		final String realm      = "me@kennethreitz.com";
		final String nonce      = "8d575ab1bfe31720d40237a8e5fc999a";
		final String opaque     = "ad9ce9a3f1f08520c15c825068a29ce2";
		final String qopOptions = "auth,auth-int";

		final String password   = "passwd";
		final String method     = "GET";
		final String uri        = "/digest-auth/auth/user/passwd";
		final String qopChosen  = "auth";
		final String nc         = "00000005";
		final String cnonce     = "6bfdcf3f73da7d4f";

		final String response    = "4950920d9b242085bfe78bd690d3eb30";

		final String receivedAuthHeader =
			"Digest "
			           + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=\"" + qopOptions + "\""
			;

		final String expectedHeader =
			"Digest "
			           + "username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "cnonce=\"" + cnonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qopChosen
			+ AUTH_SEP + "nc=" + nc
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            (str) -> nc,
			                                            () -> cnonce);

		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	/* Example from: https://jigsaw.w3.org/HTTP/Digest/ (captured using curl)
	 * 401 Response:
	 * 		www-authenticate: Digest realm="test", domain="/HTTP/Digest", nonce="039bf2e77159808b6bbfb56ea05a1c6a"
	 * Correct response:
	 * 		Authorization: Digest username="guest",realm="test",nonce="039bf2e77159808b6bbfb56ea05a1c6a",uri="/HTTP/Digest",response="ed5985e8d25f79ca435e9366caf81320"
	 * ==> yielded a 302 (Found) response:
	 */
	@Test
	void createAuthenticationValue3() throws Exception {
		final String username  = "guest";
		final String domain    = "/HTTP/Digest";
		final String realm     = "test";
		final String nonce     = "039bf2e77159808b6bbfb56ea05a1c6a";

		final String password  = "guest";
		final String method    = "GET";
		final String uri       = "/HTTP/Digest";
		final String response  = "ed5985e8d25f79ca435e9366caf81320";

		final String receivedAuthHeader =
			"Digest "
			           + "realm=\"" + realm + "\""
			+ AUTH_SEP + "domain=\"" + domain + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			;

		final String expectedHeader =
			"Digest "
			           + "username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            null,
			                                            null);

		Assertions.assertEquals(expectedHeader, responseHeader);
	}


	/**
	 * Captured using Wireshark:
	 *
	 * www-authenticate: Digest realm="test", domain="/HTTP/Digest", nonce="1be0167f214fa00d5d7999110740d824", stale=true
	 * authorization: Digest username="guest", realm="test", nonce="1be0167f214fa00d5d7999110740d824", uri="/HTTP/Digest/", response="f3378661fc3497a903b5bf9d1966cafa"
	 */
	@Test
	void createAuthenticationValue4() throws Exception {
		final String username  = "guest";
		final String domain    = "/HTTP/Digest";
		final String realm     = "test";
		final String nonce     = "1be0167f214fa00d5d7999110740d824";

		final String password  = "guest";
		final String method    = "GET";
		final String uri       = "/HTTP/Digest/"; // << this was the magic bullet!

		final String response  = "f3378661fc3497a903b5bf9d1966cafa";

		final String receivedAuthHeader =
			"Digest "
			           + "realm=\"" + realm + "\""
			+ AUTH_SEP + "domain=\"" + domain + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			;

		final String expectedHeader =
			"Digest "
			           + "username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            null,
			                                            null);

		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	// unfortunately we don't know the password used for this example...
	// @Test
	void createAuthenticationValue5() throws Exception {
		final String username  = "alice";
		final String realm     = "example.com";
		final String password  = "Wonderland";
		final String nonce     = "dcd98b7102dd2f0e8b11d0f600bfb0c093";
		final String opaque    = "5ccc069c403ebaf9f0171e9517f40e41";

		final String method    = "GET";
		final String uri       = "/protected";

		final String response  = "6629fae49393a05397450978507c4ef1";


		final String receivedAuthHeader =
			"Digest" +
			" realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			;

		final String expectedHeader =
			"Digest "
			           + "username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            null,
			                                            null);

		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	/**
	 * Taken from https://github.com/postmanlabs/httpbin/issues/397
	 * @throws Exception
	 *
	 * Www-Authenticate: Digest nonce="eed98ba4117bad8ca4d952b08cb26f9e", algorithm=MD5, opaque="2c78a425134e7f63edf10cf49b0da0b9", realm="me@kennethreitz.com", stale=FALSE, qop="auth"
	 * Authorization: Digest username="user", realm="me@kennethreitz.com", nonce="eed98ba4117bad8ca4d952b08cb26f9e", uri="/digest-auth/auth/user/passwd/MD5/never", cnonce="MTQ2MTY5MjJkYTVhMWE2ZTMzMjFiNzdjZmE0YjdmOWE=", nc=00000001, qop=auth, response="aed114dd1aa23cbbc6988e7508fb9417", opaque="2c78a425134e7f63edf10cf49b0da0b9", algorithm="MD5"
	 */
	@Test
	void createAuthenticationValue6() throws Exception {
		final String username  = "user";
		final String realm     = "me@kennethreitz.com";
		final String password  = "passwd";
		final String nonce     = "eed98ba4117bad8ca4d952b08cb26f9e";
		final String opaque    = "2c78a425134e7f63edf10cf49b0da0b9";
		final String algorithm = "MD5";
		final String stale     = "FALSE";
		final String qop       = "auth";

		final String method    = "GET";
		final String uri       = "/digest-auth/auth/user/passwd/MD5/never";
		final String cnonce    = "MTQ2MTY5MjJkYTVhMWE2ZTMzMjFiNzdjZmE0YjdmOWE=";
		final String nc        = "00000001";

		final String response  = "aed114dd1aa23cbbc6988e7508fb9417";


		final String receivedAuthHeader =
			"Digest "
			           + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "algorithm=" + algorithm + ""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "stale=" + stale + ""
			+ AUTH_SEP + "qop=" + qop + ""
			;

		final String expectedHeader =
			"Digest "
			           + "username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "algorithm=" + algorithm + ""
			+ AUTH_SEP + "cnonce=\"" + cnonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qop + ""
			+ AUTH_SEP + "nc=" + nc + ""
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            (str) -> nc,
			                                            () -> cnonce);
		Assertions.assertEquals(expectedHeader, responseHeader);
	}


	/**
	 * From https://datatracker.ietf.org/doc/html/rfc7616#section-3.9.1
	 *
	 * URL: http://api.example.org/doe.json
	 * www-authenticate: Digest
	 *   realm="http-auth@example.org",
	 *   qop="auth, auth-int",
	 *   algorithm=SHA-256,
	 *   nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v",
	 *   opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS"
	 *
	 * Response:
	 * Authorization: Digest
	 *   username="Mufasa",
	 *   realm="http-auth@example.org",
	 *   uri="/dir/index.html",
	 *   algorithm=SHA-256,
	 *   nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v",
	 *   nc=00000001,
	 *   cnonce="f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ",
	 *   qop=auth,
	 *   response="753927fa0e85d155564e2e272a28d1802ca10daf4496794697cf8db5856cb6c1",
	 *   opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS"
	 */
	@Test
	void createAuthenticationValue5_MD5() throws Exception {
		final String username   = "Mufasa";
		final String realm      = "http-auth@example.org";

		final String nonce      = "7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v";
		final String opaque     = "FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS";
		final String algorithm  = "MD5";
		final String qopOptions = "auth, auth-int";

		final String password   = "Circle of Life";
		final String method     = "GET";
		final String uri        = "/dir/index.html";
		final String qopChosen  = "auth";
		final String nc         = "00000001";
		final String cnonce     = "f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ";

		final String response   = "8ca523f5e9506fed4657c9700eebdbec";

		final String receivedAuthHeader =
			"Digest" +
			" realm=\"" + realm + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qopOptions
			+ AUTH_SEP + "algorithm="+ algorithm
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			;

		final String expectedHeader =
			"Digest"
			+ " username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "algorithm=" + algorithm
			+ AUTH_SEP + "cnonce=\"" + cnonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qopChosen
			+ AUTH_SEP + "nc=" + nc
		;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            (str) -> nc,
			                                            () -> cnonce);
		Assertions.assertEquals(expectedHeader, responseHeader);
	}


	/**
	 * From https://datatracker.ietf.org/doc/html/rfc7616#section-3.9.1
	 *
	 * URL: http://api.example.org/doe.json
	 * www-authenticate: Digest
	 *   realm="http-auth@example.org",
	 *   qop="auth, auth-int",
	 *   algorithm=SHA-256,
	 *   nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v",
	 *   opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS"
	 *
	 * Response:
	 * Authorization: Digest
	 *   username="Mufasa",
	 *   realm="http-auth@example.org",
	 *   uri="/dir/index.html",
	 *   algorithm=SHA-256,
	 *   nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v",
	 *   nc=00000001,
	 *   cnonce="f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ",
	 *   qop=auth,
	 *   response="753927fa0e85d155564e2e272a28d1802ca10daf4496794697cf8db5856cb6c1",
	 *   opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS"
	 */
	@Test
	void createAuthenticationValue5_SHA256() throws Exception {
		final String username   = "Mufasa";
		final String domain     = "/HTTP/Digest";
		final String realm      = "http-auth@example.org";
		final String nonce      = "7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v";
		final String opaque     = "FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS";
		final String algorithm  = "SHA-256";
		final String qopOptions = "auth, auth-int";

		final String password  = "Circle of Life";
		final String method    = "GET";
		final String uri       = "/dir/index.html";

		final String qopChosen = "auth";
		final String nc        = "00000001";
		final String cnonce    = "f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ";

		final String response  = "753927fa0e85d155564e2e272a28d1802ca10daf4496794697cf8db5856cb6c1";

		final String receivedAuthHeader =
			"Digest "
			           + "realm=\"" + realm + "\""
			+ AUTH_SEP + "domain=\"" + domain + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qopOptions
			+ AUTH_SEP + "algorithm="+ algorithm
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			;

		final String expectedHeader =
			"Digest "
			           + "username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "algorithm=" + algorithm
			+ AUTH_SEP + "cnonce=\"" + cnonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qopChosen
			+ AUTH_SEP + "nc=" + nc
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            (str) -> nc,
			                                            () -> cnonce);
		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	/**
	 * From RFC-7616:
	 *
	 * URL: http://api.example.org/doe.json
	 * www-authenticate: Digest
	 *   realm="api@example.org",
	 *   qop="auth",
	 *   algorithm=SHA-512-256,
	 *   nonce="5TsQWLVdgBdmrQ0XsxbDODV+57QdFR34I9HAbC/RVvkK",
	 *   opaque="HRPCssKJSGjCrkzDg8OhwpzCiGPChXYjwrI2QmXDnsOS",
	 *   charset=UTF-8,
	 *   userhash=true
	 *
	 * Response: with username-hash:
	 * authorization: Digest
	 *   username="488869477bf257147b804c45308cd62ac4e25eb717b12b298c79e62dcea254ec",
	 *   realm="api@example.org",
	 *   uri="/doe.json",
	 *   algorithm=SHA-512-256,
	 *   nonce="5TsQWLVdgBdmrQ0XsxbDODV+57QdFR34I9HAbC/RVvkK",
	 *   nc=00000001,
	 *   cnonce="NTg6RKcb9boFIAS3KrFK9BGeh+iDa/sm6jUMp2wds69v",
	 *   qop=auth,
	 *   response="ae66e67d6b427bd3f120414a82e4acff38e8ecd9101d6c861229025f607a79dd",
	 *   opaque="HRPCssKJSGjCrkzDg8OhwpzCiGPChXYjwrI2QmXDnsOS",
	 *   userhash=true

	 * Response: without username-hash:
	 * authorization: Digest
	 *   username*=UTF-8''J%C3%A4s%C3%B8n%20Doe,
	 *   realm="api@example.org",
	 *   uri="/doe.json",
	 *   algorithm=SHA-512-256,
	 *   nonce="5TsQWLVdgBdmrQ0XsxbDODV+57QdFR34I9HAbC/RVvkK",
	 *   nc=00000001,
	 *   cnonce="NTg6RKcb9boFIAS3KrFK9BGeh+iDa/sm6jUMp2wds69v",
	 *   qop=auth,
	 *   response="ae66e67d6b427bd3f120414a82e4acff38e8ecd9101d6c861229025f607a79dd",
	 *   opaque="HRPCssKJSGjCrkzDg8OhwpzCiGPChXYjwrI2QmXDnsOS",
	 *   userhash=false

	 */
	//@Test
	void createAuthenticationValue_with_SHA_512_256_Charset_and_Userhash() throws Exception {
		final String username   = "Jäsøn Doe";
		final String domain     = "/HTTP/Digest";
		final String realm      = "api@example.org";
		final String nonce      = "5TsQWLVdgBdmrQ0XsxbDODV+57QdFR34I9HAbC/RVvkK";
		final String opaque     = "HRPCssKJSGjCrkzDg8OhwpzCiGPChXYjwrI2QmXDnsOS";
		final String algorithm  = "SHA-512-256";
		final String qopOptions = "auth";
		final String charset    = "UTF-8";
		final String userhash   = "true";

		final String password  = "Secret, or not?";
		final String method    = "GET";
		final String uri       = "/doe.json";

		final String qopChosen = "auth";
		final String nc        = "00000001";
		final String cnonce    = "NTg6RKcb9boFIAS3KrFK9BGeh+iDa/sm6jUMp2wds69v";

		final String usernameHashed   = "793263caabb707a56211940d90411ea4a575adeccb7e360aeb624ed06ece9b0b";
		final String usernameUnhashed = "UTF-8''J%C3%A4s%C3%B8n%20Doe";
		final String response  = "3798d4131c277846293534c3edc11bd8a5e4cdcbff78b05db9d95eeb1cec68a5";

		final String receivedAuthHeader =
			"Digest" +
			" realm=\"" + realm + "\""
			+ AUTH_SEP + "domain=\"" + domain + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qopOptions
			+ AUTH_SEP + "algorithm="+ algorithm
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "charset=" + charset
			+ AUTH_SEP + "userhash=" + userhash
			;

		final boolean usernameHashable = true;

		final String expectedHeader =
			"Digest "
			           + "username=" + (usernameHashable
			                            ? "\"" + usernameHashed + "\""
			                            : usernameUnhashed
			                           )
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "algorithm=" + algorithm
			+ AUTH_SEP + "cnonce=\"" + cnonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qopChosen
			+ AUTH_SEP + "nc=" + nc
			+ (Boolean.parseBoolean(userhash) ? AUTH_SEP + "userhash=" + usernameHashable : "")
			+ AUTH_SEP + "charset=" + charset.toLowerCase()
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            (str) -> nc,
			                                            () -> cnonce);
		Assertions.assertEquals(expectedHeader, responseHeader);
	}


	/**
	 * Example from our dish washer:
	 * Response:
	 * Www-Authenticate: Digest realm="AdoraDish V2000", nonce="69a2ec597ec116a31d8a0076d00c695d", qop="auth", opaque="5ccc069c403ebaf9f0171e9517f40e41", stale="FALSE"
	 * Request:
	 * Authorization: Digest username="mmo", realm="AdoraDish V2000", nonce="f2da10eb273197fc94c103a75c27b175", uri="/", response="bc011c7524cedb20389a939e45da68e9", cnonce="aec6f558", opaque="5ccc069c403ebaf9f0171e9517f40e41", qop=auth, nc=00000001
	 * @throws Exception
	 */
	@Test
	void createAuthenticationValueAtHome1() throws Exception {
		final String username  = "mmo";
		final String realm     = "AdoraDish V2000";
		final String password  = "zI3EVaMOsT6P5k";
		final String nonce     = "69a2ec597ec116a31d8a0076d00c695d";
		final String qop       = "auth";
		final String opaque    = "5ccc069c403ebaf9f0171e9517f40e41";

		final String method    = "GET";
		final String uri       = "/";

		final String cnonce    = "de3eb37f8fec50a4";
		final String nc        = "00000002";
		final String response  = "fdb25e9fd304a04d50000bd66ffbd5e4";

		final String receivedAuthHeader =
			"Digest realm=\"" + realm + "\", nonce=\"" + nonce + "\", qop=\"" + qop + "\", opaque=\"" + opaque + "\", stale=\"FALSE\"";

		final String expectedHeader =
			"Digest "
			           + "username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "cnonce=\"" + cnonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qop // unquoted!
			+ AUTH_SEP + "nc=" + nc // unquoted!
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            (str) -> nc,
			                                            () -> cnonce);
		Assertions.assertEquals(expectedHeader, responseHeader, "created response header doesn't match the expected result");
	}


	/**
	 * Example from our dish washer:
	 * Response:
	 * WWW-Authenticate: Digest realm="AdoraDish V2000", nonce="6b486cf5dc34d2ddb87295cbd279c5a7", qop="auth", opaque="5ccc069c403ebaf9f0171e9517f40e41", stale="FALSE"
	 * Request:
	 * Authorization: Digest username="mmo",realm="AdoraDish V2000",nonce="6b486cf5dc34d2ddb87295cbd279c5a7",uri="/",cnonce="2f996bd4a3093d7b5fc3e79817378583",nc=00000002,response="bcddc238f725d20b7eb1c275f0390737",qop="auth",opaque="5ccc069c403ebaf9f0171e9517f40e41"
	 * @throws Exception
	 */
	@Test
	void createAuthenticationValueAtHome2() throws Exception {
		final String username  = "mmo";
		final String realm     = "AdoraDish V2000";
		final String password  = "zI3EVaMOsT6P5k";
		final String nonce     = "6b486cf5dc34d2ddb87295cbd279c5a7";
		final String qop       = "auth";
		final String opaque    = "5ccc069c403ebaf9f0171e9517f40e41";
		final String stale     = "FALSE";

		final String method    = "GET";
		final String uri       = "/";

		final String cnonce    = "2f996bd4a3093d7b5fc3e79817378583";
		final String nc        = "00000002";
		final String response  = "bcddc238f725d20b7eb1c275f0390737";

		final String receivedAuthHeader =
			"Digest realm=\"" + realm + "\", nonce=\"" + nonce + "\", qop=\"" + qop + "\", opaque=\"" + opaque + "\", stale=\"" + stale + "\"";

		final String expectedHeader =
			"Digest "
			           + "username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "cnonce=\"" + cnonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qop // unquoted!
			+ AUTH_SEP + "nc=" + nc // unquoted!
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            new URI(uri),
			                                            (str) -> nc,
			                                            () -> cnonce);
		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	public static void main(String[] args) throws Exception {
		// Test 1: Simple MD5
		MessageDigest md = MessageDigest.getInstance("MD5");
		String str = "guest:test:guest";
		byte[] input = str.getBytes(StandardCharsets.UTF_8);
		System.out.println("\ninput bytes (hex): " + HexFormat.of().withDelimiter(" ").withUpperCase().formatHex(input));
		md.update(input);
		byte[] digest = md.digest();
		String hash = HexFormat.of().withLowerCase().formatHex(digest);

		System.out.println("Input: " + str);
		System.out.println("gen:  " + hash);
		System.out.println("Expected: 087bdc75b7211e1f2c84c571bc39f212");
		System.out.println("Match: " + hash.equals("087bdc75b7211e1f2c84c571bc39f212"));

		System.out.println("MD5(\"" + str + "\") = " + hash);
	}
}
