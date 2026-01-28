/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

@SuppressWarnings("javadoc")
public class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime>
{
	private static final long serialVersionUID = -5040408858032478856L;

	private DateTimeFormatter formatter =
		DateTimeFormatter.ofPattern(AppProperties.getProperties().getProperty("LocalDateTimeStampFormat",  //$NON-NLS-1$
		                                                                      "yyyy-MM-dd HH:mm:ss.SSS")); //$NON-NLS-1$

	public CustomLocalDateTimeDeserializer() {
		this(null);
	}

	public CustomLocalDateTimeDeserializer(Class<LocalDateTime> dt) {
		super(dt);
	}

	@Override
	public LocalDateTime deserialize(JsonParser jsonparser, DeserializationContext context) throws IOException {
		String dateTime = jsonparser.getText();
		return StringUtils.isEmpty(dateTime) ? null : LocalDateTime.parse(dateTime, this.formatter);
	}
}
