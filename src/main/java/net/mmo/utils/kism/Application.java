package net.mmo.utils.kism;

import java.security.Security;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.backend.service.HistoryInfoService;
import net.mmo.utils.kism.backend.service.NodeService;
import net.mmo.utils.kism.entities.nodes.LeafNode;
import net.mmo.utils.kism.entities.nodes.connections.TCPConnection;
import net.mmo.utils.kism.utils.AppProperties;
import net.mmo.utils.kism.utils.StringUtils;
import org.springframework.boot.DefaultApplicationArguments;
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
	// constructor with autowired beans:
	Application(HistoryInfoService requestInfoService) {
		log.debug("{} c'tor", this.getClass()); //$NON-NLS-1$
		LeafNode.requestInfoService = requestInfoService;
	}

	// Code snippet that I found that should start the DB server if it is not already running
	// - but unfortunately it does not work as expected but rather causes the application to crash. :-(
	// CAUTION! The "-tcpAllowOthers" allows others to access the DB from other hosts as well but is a severe security hole and should only be added behind firewalls!

//	@Bean(initMethod = "start", destroyMethod = "stop")
//	public Server h2Server() throws SQLException {
//		return Server.createTcpServer(/*"-ifNotExists" ,*/ "-tcp", "-tcpPort", "8082" /*", -tcpAllowOthers",*/); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//	}

//	// requires implements ApplicationRunner
//	@Override
//	public void run(ApplicationArguments args) throws Exception {
//		log.info("Application started with command-line arguments: {}", Arrays.toString(args.getSourceArgs())); //$NON-NLS-1$
//		log.info("NonOptionArgs: {}", args.getNonOptionArgs()); //$NON-NLS-1$
//		log.info("OptionNames: {}", args.getOptionNames()); //$NON-NLS-1$
//
//		for (String name: args.getOptionNames()) {
//			log.info("Argument '" + name + "'='" + args.getOptionValues(name) + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		}
//	}

	/**
	 * @param args
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		try {
			log.info("main() started:"); //$NON-NLS-1$
			// This should disable the SpringBoot devtools from reloading the thread - but didn't work:
			//System.setProperty("spring.devtools.restart.enabled", "false"); //$NON-NLS-1$ //$NON-NLS-2$

			String JavaHome = System.getProperty("$JAVA HOME"); //$NON-NLS-1$

			Properties appProperties = AppProperties.getProperties();
			TCPConnection.acceptableSSLVersions =
				appProperties.getProperty("SupportedSslVersions", //$NON-NLS-1$
				                          "TLSv1,TLSv1.1,TLSv1.2,TLSv1.3,SSLv3") //$NON-NLS-1$
				             .split(",|\\s"); //$NON-NLS-1$
			log.info("Acceptable SSL-versions are: {}", Arrays.toString(TCPConnection.acceptableSSLVersions)); //$NON-NLS-1$
			// in case the list contains TLSv1(.0) and/or TLSv1.1:
			checkAndRemoveAcceptableProtocolsFromDisabledList();

			TCPConnection.disableHostNameVerification = // we parse this into a boolean as syntax validation:
				Boolean.parseBoolean(appProperties.getProperty("DisableHostNameVerification", //$NON-NLS-1$
				                                               "true")); //$NON-NLS-1$
			System.setProperty("jdk.internal.httpclient.disableHostnameVerification", //$NON-NLS-1$
			                   Boolean.toString(TCPConnection.disableHostNameVerification));
			// For certain connections we set the host header (which is normally automatically added by the stack).
			// To allow this the following option is set - but it doesn't work... :-(
			System.setProperty("jdk.httpclient.allowRestrictedHeaders", //$NON-NLS-1$
			                   "host"); //$NON-NLS-1$

			TCPConnection.trustAllCertificates =
				Boolean.parseBoolean(appProperties.getProperty("TrustAllCertificates", //$NON-NLS-1$
				                                               "true")); //$NON-NLS-1$

			// the truststore for certificates that we trust (only relevant, if we trustAllCertificates == false)
			// not yet supported:
			String trustStoreType =
				appProperties.getProperty("TrustStoreType", //$NON-NLS-1$
				                          "pkcs12"); //$NON-NLS-1$
			String trustStorePath =
				appProperties.getProperty("TrustStorePath", //$NON-NLS-1$
				                          (JavaHome != null ? JavaHome : ".") + "/lib/security/cacerts"); //$NON-NLS-1$ //$NON-NLS-2$
			String trustStorePwd =
				appProperties.getProperty("TrustStorePassword", //$NON-NLS-1$
				                          "changeit"); //$NON-NLS-1$

			log.info("Truststore path: '{}' / nodeType: '{}'", trustStorePath, trustStoreType); //$NON-NLS-1$
			if (!StringUtils.isEmpty(trustStoreType)) System.setProperty("javax.net.ssl.trustStoreType", trustStoreType); //$NON-NLS-1$
			if (!StringUtils.isEmpty(trustStorePath)) System.setProperty("javax.net.ssl.trustStore", trustStorePath); //$NON-NLS-1$
			if (!StringUtils.isEmpty(trustStorePwd))  System.setProperty("javax.net.ssl.trustStorePassword", trustStorePwd); //$NON-NLS-1$

			// the keystore we use to connect to other systems:
			String keyStoreType =
				appProperties.getProperty("KeyStoreType", //$NON-NLS-1$
				                          "jks"); //$NON-NLS-1$
			String keyStorePath =
				appProperties.getProperty("KeyStorePath", //$NON-NLS-1$
				                          "keystore.p12"); //$NON-NLS-1$
			String keyStorePwd =
				appProperties.getProperty("KeyStorePassword", //$NON-NLS-1$
				                          "password"); //$NON-NLS-1$

			log.info("Keystore path: '{}' / nodeType: '{}'", keyStorePath, keyStoreType); //$NON-NLS-1$
			if (!StringUtils.isEmpty(keyStoreType)) System.setProperty("javax.net.ssl.keyStoreType", keyStoreType); //$NON-NLS-1$
			if (!StringUtils.isEmpty(keyStorePath)) System.setProperty("javax.net.ssl.keyStore", keyStorePath); //$NON-NLS-1$
			if (!StringUtils.isEmpty(keyStorePwd))  System.setProperty("javax.net.ssl.keyStorePassword", keyStorePwd); //$NON-NLS-1$

			String localeStr = appProperties.getProperty("Locale"); //$NON-NLS-1$
			if (!StringUtils.isEmpty(localeStr)) {
				Locale locale = Locale.forLanguageTag(localeStr);
				log.info("Setting Locale: given '{}' -> found: '{}' / language: '{}'", //$NON-NLS-1$
				         localeStr, locale, (locale != null ? locale.getLanguage() : null));
				Locale.setDefault(locale);
			}

			// misc. other options I had bumped into:
			String authRetryLimit = appProperties.getProperty("HttpAuthRetryLimit"); //$NON-NLS-1$
			if (authRetryLimit != null) System.setProperty("jdk.httpclient.auth.retrylimit", authRetryLimit); //$NON-NLS-1$
			String redirectsRetryLimit = appProperties.getProperty("HttpRedirectsRetryLimit"); //$NON-NLS-1$
			if (redirectsRetryLimit != null) System.setProperty("jdk.httpclient.redirects.retrylimit", redirectsRetryLimit); //$NON-NLS-1$

			// we need to set this BEFORE the NodeService is created. If we do this in the Application-c'tor we come to late.
			NodeService.applicationArguments = new DefaultApplicationArguments(args);

		} catch (Throwable e) {
			log.error("Error initializing application! Check your settings in application.properties", e); //$NON-NLS-1$
		}

		try {
			SpringApplication.run(Application.class, args);
		} catch (Throwable e) {
			if (e.getClass().getName().contains("SilentExitException")) { // SpringBoot devtools trying to replace the main thread //$NON-NLS-1$
				log.debug("Spring is restarting the main thread - see spring-boot-devtools"); //$NON-NLS-1$
			} else {
				log.error("SpringApplication.run() crashed!", e); //$NON-NLS-1$
			}
		}
		log.info("main() ended."); //$NON-NLS-1$
	}

	/** Java 1.8u61(?) disabled TLSv1 and TLSv1.1, so - if we want to allow that - we
	 *  need to remove it from the disabledAlgorithms-list:
	 **/
	private static void checkAndRemoveAcceptableProtocolsFromDisabledList() {
		String disabledAlgorithms = Security.getProperty("jdk.tls.disabledAlgorithms"); //$NON-NLS-1$
		for (String acceptableSSLVersion: TCPConnection.acceptableSSLVersions) {
			if (disabledAlgorithms.contains(acceptableSSLVersion)) { // we accept an SSL version that the JDK has disabled -> remove it from that list!
				disabledAlgorithms = disabledAlgorithms.replace(acceptableSSLVersion + ",", ""); //$NON-NLS-1$ //$NON-NLS-2$ // this works only if the protocol to be removed is not at the end of the list but this is typically the case as the list ends with something like "include jdk.disabled.namedCurves"
			}
		}
		Security.setProperty("jdk.tls.disabledAlgorithms", disabledAlgorithms); //$NON-NLS-1$
	}
}
