/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for the handling of client certificates for specific hosts
 */
@NoArgsConstructor // for serialization
@Getter
@Setter

@Slf4j
public class CertificateHandling
{
	final static String DefaultKeyAndCertificateFileType = "PKCS12"; //$NON-NLS-1$

	/**
	 * descriptor class for a key and certificate file
	 */
	@NoArgsConstructor // for serialization
	@AllArgsConstructor
	@Getter
	@Setter

	@ToString
	public static class KeyAndCertificateDescriptor {
		String hostPattern;
		String keyAndCertFileName;
		String keyAndCertFileType;
		String keyAndCertFilePwd;
		String keyAndCertKeyPwd;
		// byte[] keyAndCertFileContent;  // later...
	}

	protected ArrayList<KeyAndCertificateDescriptor> descriptors;

	@JsonIgnore
	transient HashMap<String, KeyAndCertificateDescriptor> keyAndCertificateFiles = new HashMap<>();

	@JsonIgnore
	transient HashMap<String, KeyManagerFactory> keyManagerFactories = new HashMap<>();

	void init () {
		this.descriptors = new ArrayList<>();

//		this.descriptors.add(
//			new KeyAndCertificateDescriptor("<example-domain>", //$NON-NLS-1$
//			                                "<example-keystore>.p12", //$NON-NLS-1$
//			                                DefaultKeyAndCertificateFileType,
//			                                "<example-keystore-file-password>", //$NON-NLS-1$
//			                                "<example-keystore-key-password>") //$NON-NLS-1$
//		);
	}

	@SuppressWarnings("javadoc")
	public void preloadKeysAndCerts() {
		log.info("preloadKeysAndCerts - supported Keystore Filetypes are: {}", //$NON-NLS-1$
		         Arrays.asList(java.security.Security.getProviders()).stream().map(p -> p.getName()).collect(Collectors.toList()));
		if (this.descriptors == null) {
			init(); // preload the hard-coded certificates if none defined, yet (this is to bootstrap the whole thing...)
		}
		log.info("Preloaded certificate descriptors are: {}", this.descriptors); //$NON-NLS-1$

		this.descriptors.forEach(desc -> this.keyAndCertificateFiles.put(desc.getHostPattern(), desc));
		this.keyAndCertificateFiles.forEach((key, fd) -> {
			getKeyManagerFactory(fd.getHostPattern());
		});
	}

	/**
	 * @param host
	 * @return KeyManagerFactory if a specific keystore is provided  for the host
	 */
	public KeyManagerFactory getKeyManagerFactory(String host) {
		KeyManagerFactory kmf = this.keyManagerFactories.get(host); // TODO: should search via pattern-match
		if (kmf == null) {
			KeyAndCertificateDescriptor fileDescriptor = this.keyAndCertificateFiles.get(host); // TODO: should search via pattern-match
			if (fileDescriptor != null) {
				log.info("Creating key manager for host '{}':", host); //$NON-NLS-1$
				File file = null;
				FileInputStream fis = null;
				try {
					KeyStore ks = KeyStore.getInstance(fileDescriptor.getKeyAndCertFileType());
					file = new File(fileDescriptor.getKeyAndCertFileName());
					fis = new FileInputStream(file);
					String filePwd = fileDescriptor.getKeyAndCertFilePwd();
					ks.load(fis, (filePwd != null ? filePwd.toCharArray() : null));
					kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
					String keyPwd = fileDescriptor.getKeyAndCertKeyPwd();
					if (keyPwd.equals("=")) keyPwd = filePwd; //$NON-NLS-1$
					kmf.init(ks, (keyPwd != null ? keyPwd.toCharArray() : null));
					this.keyManagerFactories.put(host, kmf);
					log.info("Key manager created for host '{}' from file '{}'.", host, file.getAbsolutePath()); //$NON-NLS-1$
				} catch (Exception ex) {
					log.error("Error reading certificate or creating key manager factory for host '{}' from file '{}': {}", //$NON-NLS-1$
					          host,
					          (file != null ? file.getAbsolutePath() : fileDescriptor.getKeyAndCertFileName()),
					          ExceptionUtils.exceptionCauseSummary(ex));
				} finally {
					if (fis != null) {
						try { fis.close(); } catch (IOException ex) { /*ignore */ }
					}
				}
			}
		}
		return kmf;
	}
}
