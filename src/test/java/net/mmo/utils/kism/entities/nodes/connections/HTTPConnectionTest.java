/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes.connections;

import static net.mmo.utils.kism.utils.HTTP_Authorization.AUTH_SEP;

import java.security.MessageDigest;

import net.mmo.utils.kism.utils.HTTP_Authorization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"nls", "javadoc"})
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

		final String ha1  = HTTP_Authorization.H(md, "Mufasa:testrealm@host.com:Circle Of Life");
		Assertions.assertEquals("939e7578ed9e3c518a452acee763bce9", ha1);

		final String ha2 = HTTP_Authorization.H(md, "GET:/dir/index.html");
		Assertions.assertEquals("39aff3a2bab6126f332b942af96d3366", ha2);

		final String exampleResponse = HTTP_Authorization.H(md,
		                                                    "939e7578ed9e3c518a452acee763bce9:"
		                                                    + "dcd98b7102dd2f0e8b11d0f600bfb0c093:"
		                                                    + "00000001:0a4f113b:auth:"
		                                                    + "39aff3a2bab6126f332b942af96d3366"
		                                                   );
		Assertions.assertEquals("6629fae49393a05397450978507c4ef1", exampleResponse);
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
			"Digest"
			+ " realm=\"" + realm + "\""
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
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			;
		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            uri,
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
			"Digest"
			+ " realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=\"" + qopOptions + "\""
			;

		final String expectedHeader =
			"Digest"
			+ " username=\"" + username + "\""
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
			                                            uri,
			                                            (str) -> nc,
			                                            () -> cnonce);

		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	/* Example from: https://jigsaw.w3.org/HTTP/Digest/
	 * correct response:
	 * 'Authorization: Digest username="guest", realm="test", nonce="7305ce98f17d2606368f79912e667225", uri="/HTTP/Digest/", response="ada89f170cbbe92c41bb075e0e1f0c65"'
	 */
	@Test
	void createAuthenticationValue3() throws Exception {
		final String username  = "guest";
		final String domain    = "/HTTP/Digest";
		final String realm     = "test";
		final String nonce     = "7cc031b9a618a8c228d08200917c6f4e";

		final String password  = "guest";
		final String method    = "GET";
		final String uri       = "/HTTP/Digest/"; // << this was the magic bullet!
		final String response  = "66adf3c3ffb9006583954b6143c649f4";

		final String receivedAuthHeader =
			"Digest" +
			" realm=\"" + realm + "\""
			+ AUTH_SEP + "domain=\"" + domain + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			;

		final String expectedHeader =
						"Digest"
						+ " username=\"" + username + "\""
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
			                                            uri,
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
			"Digest" +
			" realm=\"" + realm + "\""
			+ AUTH_SEP + "domain=\"" + domain + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			;

		final String expectedHeader =
						"Digest"
						+ " username=\"" + username + "\""
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
			                                            uri,
			                                            null,
			                                            null);

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
			                                            uri,
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
			"Digest" +
			" realm=\"" + realm + "\""
			+ AUTH_SEP + "domain=\"" + domain + "\""
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
			                                            uri,
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
	@Test
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
			"Digest"
			+ " username=" + (usernameHashable
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
			;

		final String responseHeader =
			HTTP_Authorization.createAuthorizationValue(receivedAuthHeader,
			                                            username,
			                                            password,
			                                            method,
			                                            HTTPConnection.EMPTY_BODY,
			                                            uri,
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
			"Digest"
			+ " username=\"" + username + "\""
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
			                                            uri,
			                                            (str) -> nc,
			                                            () -> cnonce);

		Assertions.assertEquals(expectedHeader, responseHeader, "created response header doesn't match the expected result");
	}


	/**
	 * Example from our dish washer:
	 * Response:
	 * Www-Authenticate: Digest realm="AdoraDish V2000", nonce="de8d452441f462175d9f53a35610dbd7", qop="auth", opaque="5ccc069c403ebaf9f0171e9517f40e41", stale="TRUE"
	 * Request:
	 * Authorization: Digest username="mmo", realm="AdoraDish V2000", nonce="de8d452441f462175d9f53a35610dbd7", uri="/", response="71cf87e1ca823843d34e2b60cbd4ee29", opaque="5ccc069c403ebaf9f0171e9517f40e41", qop=auth, nc=00000001, cnonce="b96c5559aa3971e1"
	 * @throws Exception
	 */
	@Test
	void createAuthenticationValueAtHome2() throws Exception {
		final String username  = "mmo";
		final String realm     = "AdoraDish V2000";
		final String password  = "zI3EVaMOsT6P5k";
		final String nonce     = "de8d452441f462175d9f53a35610dbd7";
		final String qop       = "auth";
		final String opaque    = "5ccc069c403ebaf9f0171e9517f40e41";

		final String method    = "GET";
		final String uri       = "/";

		final String cnonce    = "b96c5559aa3971e1";
		final String nc        = "00000001";
		final String response  = "71cf87e1ca823843d34e2b60cbd4ee29";

		final String receivedAuthHeader =
			"Digest realm=\"AdoraDish V2000\", nonce=\"de8d452441f462175d9f53a35610dbd7\", qop=\"auth\", opaque=\"5ccc069c403ebaf9f0171e9517f40e41\", stale=\"TRUE\"";

//		final String expectedHeader =
//			"Digest username=\"mmo\", realm=\"AdoraDish V2000\", nonce=\"de8d452441f462175d9f53a35610dbd7\", uri=\"/\", response=\"71cf87e1ca823843d34e2b60cbd4ee29\", opaque=\"5ccc069c403ebaf9f0171e9517f40e41\", qop=auth, nc=00000001, cnonce=\"b96c5559aa3971e1\"";

		final String expectedHeader =
			"Digest"
			+ " username=\"" + username + "\""
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
			                                            uri,
			                                            (str) -> nc,
			                                            () -> cnonce);

		Assertions.assertEquals(expectedHeader, responseHeader);
	}



}
