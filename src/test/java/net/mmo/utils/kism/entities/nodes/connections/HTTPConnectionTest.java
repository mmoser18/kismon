/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 27 Nov 2024
 */

package net.mmo.utils.kism.entities.nodes.connections;

import static net.mmo.utils.kism.entities.nodes.connections.HTTPConnection.AUTH_SEP;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"nls", "javadoc"})
public class HTTPConnectionTest
{

	@Test
	void ExtractQuotedValueTest() {
		final String authHeader = "Digest realm=\"Some realm\", nonce=\"2955cc6154daf65f8259755919c0041e\", qop=\"auth\", opaque=\"5ccc069c403ebaf9f0171e9517f40e41\", stale=\"FALSE\"";
		String extracted;

		extracted = HTTPConnection.extractQuotedValue(authHeader, "realm");
		Assertions.assertEquals("Some realm", extracted);

		extracted = HTTPConnection.extractQuotedValue(authHeader, "qop");
		Assertions.assertEquals("auth", extracted);

		extracted = HTTPConnection.extractQuotedValue(authHeader, "stale");
		Assertions.assertEquals("FALSE", extracted);
	}

	@Test
	void DirectiveContainsTest() {
		String directive;
		directive = HTTPConnection.directiveContains(null, "auth");
		Assertions.assertEquals(null, directive);

		directive = HTTPConnection.directiveContains("foobar", "auth");
		Assertions.assertEquals(null, directive);

		directive = HTTPConnection.directiveContains("auth,auth-int", "auth");
		Assertions.assertEquals("auth", directive);
	}

	@Test
	void MD5Test() throws Exception {
		// example taken from: https://en.wikipedia.org/wiki/Digest_access_authentication

		final String ha1  = HTTPConnection.H("Mufasa:testrealm@host.com:Circle Of Life", "MD5");
		Assertions.assertEquals("939e7578ed9e3c518a452acee763bce9", ha1);

		final String ha2 = HTTPConnection.H("GET:/dir/index.html", "MD5");
		Assertions.assertEquals("39aff3a2bab6126f332b942af96d3366", ha2);

		final String exampleResponse = HTTPConnection.H("939e7578ed9e3c518a452acee763bce9:"
		                                                + "dcd98b7102dd2f0e8b11d0f600bfb0c093:"
		                                                + "00000001:0a4f113b:auth:"
		                                                + "39aff3a2bab6126f332b942af96d3366",
		                                                 "MD5");
		Assertions.assertEquals("6629fae49393a05397450978507c4ef1", exampleResponse);
	}

	@Test
	void createAuthenticationValueWithoutQopTest() throws Exception {
		final String username  = "Mufasa";
		final String realm     = "testrealm@host.com";
		final String password  = "Circle Of Life";
		final String nonce     = "dcd98b7102dd2f0e8b11d0f600bfb0c093";
		final String opaque    = "5ccc069c403ebaf9f0171e9517f40e41";
		final String method    = "GET";
		final String uri       = "/dir/index.html";
		final String response  = "670fd8c2df070c60b045671b8b24ff02";

		final String receivedAuthHeader =
			"Digest"
			+ " realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			;
		final String responseHeader =
			HTTPConnection.createAuthorizationValue(receivedAuthHeader,
			                                         username,
			                                         password,
			                                         method,
			                                         uri,
			                                         null,
			                                         null);

		final String expectedHeader =
			"Digest"
			+ " username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			;
		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	@Test
	void createAuthenticationValueWithQopTest() throws Exception {
		final String username  = "Mufasa";
		final String realm     = "testrealm@host.com";
		final String password  = "Circle Of Life";
		final String nonce     = "dcd98b7102dd2f0e8b11d0f600bfb0c093";
		final String opaque    = "5ccc069c403ebaf9f0171e9517f40e41";
		final String qop       = "auth,auth-int";
		final String nc        = "00000001";
		final String cnonce    = "0a4f113b";
		final String method    = "GET";
		final String uri       = "/dir/index.html";
		final String response  = "6629fae49393a05397450978507c4ef1";

		final String receivedAuthHeader =
			"Digest"
			+ " realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=\"" + qop + "\""
			;
		final String responseHeader =
			HTTPConnection.createAuthorizationValue(receivedAuthHeader,
			                                         username,
			                                         password,
			                                         method,
			                                         uri,
			                                         (str) -> nc,
			                                         () -> cnonce);

		final String qopSelected = "auth";

		final String expectedHeader =
			"Digest"
			+ " username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			+ AUTH_SEP + "cnonce=\"" + cnonce + "\""
			+ AUTH_SEP + "opaque=\"" + opaque + "\""
			+ AUTH_SEP + "qop=" + qopSelected
			+ AUTH_SEP + "nc=" + nc
			;
		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	/* Example from: https://jigsaw.w3.org/HTTP/Digest/
	 * correct response:
	 * 'Authorization: Digest username="guest", realm="test", nonce="7305ce98f17d2606368f79912e667225", uri="/HTTP/Digest/", response="ada89f170cbbe92c41bb075e0e1f0c65"'
	 */
	@Test
	void createAuthenticationValue3Test() throws Exception {
		final String username  = "guest";
		final String domain    = "/HTTP/Digest";
		final String realm     = "test";
		final String password  = "guest";
		final String nonce     = "7cc031b9a618a8c228d08200917c6f4e";
		final String method    = "GET";
		final String response  = "66adf3c3ffb9006583954b6143c649f4";
		final String uri       = "/HTTP/Digest/"; // << this was the magic bullet!

		final String receivedAuthHeader =
			"Digest" +
			" realm=\"" + realm + "\""
			+ AUTH_SEP + "domain=\"" + domain + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			;


		final String responseHeader =
			HTTPConnection.createAuthorizationValue(receivedAuthHeader,
			                                         username,
			                                         password,
			                                         method,
			                                         uri,
			                                         null,
			                                         null);

		final String expectedHeader =
			"Digest"
			+ " username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			;
		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	@Test
	void createAuthenticationValue4Test() throws Exception {
		final String username  = "guest";
		final String domain    = "/HTTP/Digest";
		final String realm     = "test";
		final String password  = "guest";
		final String nonce     = "cd98475e08083eb6da9040b77e4bb398";
		final String method    = "GET";
		final String response  = "d71cbebb6405232b9b874a891c5cfb44";
		final String uri       = "/HTTP/Digest/"; // << this was the magic bullet!

		final String receivedAuthHeader =
			"Digest" +
			" realm=\"" + realm + "\""
			+ AUTH_SEP + "domain=\"" + domain + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			;


		final String responseHeader =
			HTTPConnection.createAuthorizationValue(receivedAuthHeader,
			                                         username,
			                                         password,
			                                         method,
			                                         uri,
			                                         null,
			                                         null);

		final String expectedHeader =
			"Digest"
			+ " username=\"" + username + "\""
			+ AUTH_SEP + "realm=\"" + realm + "\""
			+ AUTH_SEP + "nonce=\"" + nonce + "\""
			+ AUTH_SEP + "uri=\"" + uri + "\""
			+ AUTH_SEP + "response=\"" + response + "\""
			;
		Assertions.assertEquals(expectedHeader, responseHeader);
	}

	/**
	 * Response:
	 * Www-Authenticate: Digest realm="AdoraDish V2000", nonce="f2da10eb273197fc94c103a75c27b175", qop="auth", opaque="5ccc069c403ebaf9f0171e9517f40e41", stale="FALSE"
	 * Request:
	 * Authorization: Digest username="mmo", realm="AdoraDish V2000", nonce="f2da10eb273197fc94c103a75c27b175", uri="/", response="bc011c7524cedb20389a939e45da68e9", cnonce="aec6f558", opaque="5ccc069c403ebaf9f0171e9517f40e41", qop=auth, nc=00000001
	 * @throws Exception
	 */
	@Test
	void createAuthenticationValue5Test() throws Exception {
		final String username  = "mmo";
		final String realm     = "AdoraDish V2000";
		final String password  = "zI3EVaMOsT6P5k";
		final String nonce     = "f2da10eb273197fc94c103a75c27b175";
		final String method    = "GET";
		final String uri       = "/";
		final String qop       = "auth";
		final String opaque    = "5ccc069c403ebaf9f0171e9517f40e41";

		final String cnonce    = "aec6f558";
		final String nc        = "00000001";
		final String response  = "bc011c7524cedb20389a939e45da68e9";

		final String receivedAuthHeader =
			"Digest realm=\"AdoraDish V2000\", nonce=\"f2da10eb273197fc94c103a75c27b175\", qop=\"auth\", opaque=\"5ccc069c403ebaf9f0171e9517f40e41\", stale=\"FALSE\"";

		final String responseHeader =
			HTTPConnection.createAuthorizationValue(receivedAuthHeader,
			                                         username,
			                                         password,
			                                         method,
			                                         uri,
			                                         (str) -> nc,
			                                         () -> cnonce);

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
		Assertions.assertEquals(expectedHeader, responseHeader);
	}
}
