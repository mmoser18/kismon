/**
 * @author Michael Moser (michael.moser@elca.ch)
 * @since 02.12.2020
 */

package net.mmo.utils.kism.utils;

import java.io.InputStream;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("javadoc")
@Slf4j
public class AppProperties
{
	private final static String APP_PROPS_NAME = "application.properties"; //$NON-NLS-1$

	private static Properties appProperties;

	/**
	 * @return top level (i.e. application) properties
	 */
	public final static Properties getProperties() {
		if (appProperties == null) {
			appProperties = new Properties(System.getProperties());
			try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(APP_PROPS_NAME)) { // only with getResourceAsStream() one can read from a .jar file
				appProperties.load(is);
			} catch (Exception ex) {
				log.error("Error opening/reading application properties '{}': {}", APP_PROPS_NAME, ex); //$NON-NLS-1$
			}
		}
		return appProperties;
	}
}

