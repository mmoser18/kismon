/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;


@SuppressWarnings("javadoc")
public class CustomLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime>
{
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
	public LocalDateTime deserialize(JsonParser jsonparser, DeserializationContext context) {
		String dateTime = jsonparser.getString();
		return StringUtils.isEmpty(dateTime) ? null : LocalDateTime.parse(dateTime, this.formatter);
	}
}
