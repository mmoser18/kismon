/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 06.12.2020
 */

package net.mmo.utils.kism.entities.connections;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;
import net.mmo.utils.kism.entities.LeafNode;
import net.mmo.utils.kism.entities.ResultChecker;

/** Name should have been just "Connection" but that class name was already used by JDBC */
@Entity
@Setter
@Getter
// @Slf4j
abstract public class IPConnection extends LeafNode
{
	private static final long serialVersionUID = -2584969925210734417L;

//	protected String proxyHost;
//	protected int    proxyPort;
//	protected String proxyUid;
//	protected String proxyPwd;

	@SuppressWarnings("nls")
	@NotNull
	@NotEmpty
	@NotBlank
	protected String url = "http://www.example.com:8080";

	@JsonIgnore
	protected transient String resultingUrl;

	@OneToOne
	@JoinColumn(name = "result_checker_id")
	protected ResultChecker resultChecker;


	protected IPConnection() {
		super();
	}

	protected IPConnection(String name, String description) {
		super(name, description);
	}

	@Override
	public void updateResolvableValues() {
		super.updateResolvableValues();
		updateResultingUrl();
	}

	public void setUrl(String url) {
		if (!Objects.equal(this.url, url)) {
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
			.append(", checkResults:").append(this.checkResults) //$NON-NLS-1$
			.append(", resultChecker:").append(this.resultChecker) //$NON-NLS-1$
			.append("}") //$NON-NLS-1$
			.toString();
	}
}
