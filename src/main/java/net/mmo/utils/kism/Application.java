package net.mmo.utils.kism;

import lombok.extern.slf4j.Slf4j;
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
		// This should disable the SpringBoot devtools from reloading the thread - but didn't work:
		//System.setProperty("spring.devtools.restart.enabled", "false"); //$NON-NLS-1$ //$NON-NLS-2$

		String JavaHome = System.getProperty("$JAVA HOME"); //$NON-NLS-1$
		String certificatesTrustStorePath =
			AppProperties.getProperties().getProperty("TrustStorePath", //$NON-NLS-1$
			                                          (JavaHome != null ? JavaHome : ".") + "/lib/security/cacerts"); //$NON-NLS-1$ //$NON-NLS-2$
		log.info("Certificates truststore path: '{}'", certificatesTrustStorePath); //$NON-NLS-1$
		System.setProperty("javax.net.ssl.trustStore", certificatesTrustStorePath); //$NON-NLS-1$

		String certificatesTrustStorePwd =
			AppProperties.getProperties().getProperty("TrustStorePassword", //$NON-NLS-1$
			                                          "changeit"); //$NON-NLS-1$
		System.setProperty("javax.net.ssl.trustStorePassword", certificatesTrustStorePwd); //$NON-NLS-1$

//		// in case we ever need this:
//		System.setProperty("javax.net.ssl.keyStore", "C:/keystoreFile.jks");
//		System.setProperty("javax.net.ssl.keyStorePassword", "mypassword");

		try {
			SpringApplication.run(Application.class, args);
		} catch (Throwable e) {
			if (e.getClass().getName().contains("SilentExitException")) { // SpringBoot devtools trying to replace the main thread //$NON-NLS-1$
				log.debug("Spring is restarting the main thread - See spring-boot-devtools"); //$NON-NLS-1$
			} else {
				log.error("Application crashed!", e); //$NON-NLS-1$
			}
		}
	}
}
