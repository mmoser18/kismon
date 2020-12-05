/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 02.12.2020
 */

package net.mmo.utils.kism.utils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppProperties
{
	private final static String APP_PROPS_NAME = "application.properties"; //$NON-NLS-1$

	private static Properties appProperties;

	public final static Properties getProperties() {
		if (appProperties == null) {
			String appConfigPath = Thread.currentThread().getContextClassLoader().getResource(APP_PROPS_NAME).getPath();
			appProperties = new Properties(System.getProperties());
			try (InputStream is = new FileInputStream(appConfigPath)) {
				appProperties.load(is);
			} catch (Exception ex) {
				log.error("Error opening/reading application properties {}: {}", appConfigPath, ex); //$NON-NLS-1$
			}
		}
		return appProperties;
	}
}

