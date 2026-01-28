/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes.connections;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import net.mmo.utils.kism.entities.nodes.CheckableNode;

@SuppressWarnings("javadoc")
@Setter
@Getter
abstract public class TCPConnection extends CheckableNode
{
	private static final long serialVersionUID = 6679464784537111431L;

//	protected String proxyHost;
//	protected int    proxyPort;
//	protected String proxyUid;
//	protected String proxyPwd;

	@SuppressWarnings("nls")
	@NotNull
	@NotEmpty
	@NotBlank
	protected String url = "https://www.example.com:443";

	@JsonIgnore
	protected transient String resultingUrl;

	@JsonIgnore
	public transient static String[] acceptableSSLVersions;
	@JsonIgnore
	public transient static boolean disableHostNameVerification;
	@JsonIgnore
	public transient static boolean trustAllCertificates;


	protected TCPConnection() {
		super();
	}

	protected TCPConnection(String name, String description) {
		super(name, description);
	}

	@Override
	public void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingUrl();
	}

	public void setUrl(String url) {
		if (!Objects.equals(this.url, url)) {
			this.url = url;
			updateResultingUrl();
		}
	}

	private void updateResultingUrl() {
		updateResultingValue(getUrl(), (str) -> setResultingUrl(str));
	}

	public String resultingUrlResolved() throws Exception {
		return resultingFieldResolved(getUrl(), () -> getResultingUrl(), (str) -> setResultingUrl(str));
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
//			.append(", proxyHost:").append(this.proxyHost) //$NON-NLS-1$
//			.append(", proxyPort:").append(this.proxyPort) //$NON-NLS-1$
//			.append(", proxyUid:").append(this.proxyUid) //$NON-NLS-1$
//			.append(", proxyPwd:").append(this.proxyPwd) //$NON-NLS-1$
			.append(", url:").append(this.url) //$NON-NLS-1$
			.append(", resultingUrl:").append(this.resultingUrl) //$NON-NLS-1$
			.append(", resultChecker:").append(this.resultChecker) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}
}
