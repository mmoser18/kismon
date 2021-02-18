package net.mmo.utils.kism;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.nodes.connections.IPConnection;
import net.mmo.utils.kism.utils.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * The entry point of the Spring Boot application.
 *
 * Note: We need to disable Spring MVC auto configuration on the Application class,
 * as this interferes with how Vaadin works and can cause strange reloading behavior.
 */
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@Slf4j
public class Application extends SpringBootServletInitializer
{
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		try {
			// This should disable the SpringBoot devtools from reloading the thread - but didn't work:
			//System.setProperty("spring.devtools.restart.enabled", "false"); //$NON-NLS-1$ //$NON-NLS-2$

			String JavaHome = System.getProperty("$JAVA HOME"); //$NON-NLS-1$

			IPConnection.sslVersions =
				AppProperties.getProperties().getProperty("SupportedSslVersions", //$NON-NLS-1$
				                                          "TLSv1,TLSv1.1,TLSv1.2,TLSv1.3,SSLv3"); //$NON-NLS-1$

			IPConnection.disableHostNameVerification = // we parse this into a boolean as syntax validation:
				Boolean.parseBoolean(AppProperties.getProperties().getProperty("DisableHostNameVerification", //$NON-NLS-1$
				                                                               "true")); //$NON-NLS-1$
			System.getProperties().setProperty("jdk.internal.httpclient.disableHostnameVerification", //$NON-NLS-1$
			                                   Boolean.toString(IPConnection.disableHostNameVerification));

			IPConnection.trustAllCertificates =
				Boolean.parseBoolean(AppProperties.getProperties().getProperty("TrustAllCertificates", //$NON-NLS-1$
				                                                               "true")); //$NON-NLS-1$

			// the truststore for certificates that we trust (only relevant, if we trustAllCertificates == false)
			// not yet supported:
			String trustStoreType =
				AppProperties.getProperties().getProperty("TrustStoreType", //$NON-NLS-1$
				                                          "pkcs12"); //$NON-NLS-1$
			String trustStorePath =
				AppProperties.getProperties().getProperty("TrustStorePath", //$NON-NLS-1$
				                                          (JavaHome != null ? JavaHome : ".") + "/lib/security/cacerts"); //$NON-NLS-1$ //$NON-NLS-2$
			String trustStorePwd =
				AppProperties.getProperties().getProperty("TrustStorePassword", //$NON-NLS-1$
				                                          "changeit"); //$NON-NLS-1$

			log.info("Truststore path: '{}' / type: '{}'", trustStorePath, trustStoreType); //$NON-NLS-1$
			if (trustStoreType != null) System.setProperty("javax.net.ssl.trustStoreType", trustStoreType); //$NON-NLS-1$
			if (trustStorePath != null) System.setProperty("javax.net.ssl.trustStore", trustStorePath); //$NON-NLS-1$
			if (trustStorePwd  != null) System.setProperty("javax.net.ssl.trustStorePassword", trustStorePwd); //$NON-NLS-1$

			// the keystore we use to connect to other systems:
			// not yet supported:
			String keyStoreType =
				AppProperties.getProperties().getProperty("KeyStoreType", //$NON-NLS-1$
				                                          "jks"); //$NON-NLS-1$
			String keyStorePath =
				AppProperties.getProperties().getProperty("KeyStorePath", //$NON-NLS-1$
				                                          "D:\\Projects\\KStA_ZH_ZHQuest\\spec\\Testing\\Testsystems\\Testcertificates\\certificates - test systems\\localhost-keystore.p12"); //$NON-NLS-1$
			String keyStorePwd =
				AppProperties.getProperties().getProperty("KeyStorePassword", //$NON-NLS-1$
				                                          "password"); //$NON-NLS-1$

			log.info("Keystore path: '{}' / type: '{}'", keyStorePath, keyStoreType); //$NON-NLS-1$
			if (keyStoreType != null) System.setProperty("javax.net.ssl.keyStoreType", keyStoreType); //$NON-NLS-1$
			if (keyStorePath != null) System.setProperty("javax.net.ssl.keyStore", keyStorePath); //$NON-NLS-1$
			if (keyStorePwd  != null) System.setProperty("javax.net.ssl.keyStorePassword", keyStorePwd); //$NON-NLS-1$

			SpringApplication.run(Application.class, args);
		} catch (Throwable e) {
			if (e.getClass().getName().contains("SilentExitException")) { // SpringBoot devtools trying to replace the main thread //$NON-NLS-1$
				log.debug("Spring is restarting the main thread - see spring-boot-devtools"); //$NON-NLS-1$
			} else {
				log.error("Application crashed!", e); //$NON-NLS-1$
			}
		}
	}
}
