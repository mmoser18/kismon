/**
 * Copyright © 2020-2026 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

@SuppressWarnings("javadoc")
public class CustomLocalDateTimeSerializer extends StdSerializer<LocalDateTime>
{
	private SimpleDateFormat formatter =
		new SimpleDateFormat(AppProperties.getProperties().getProperty("LocalDateTimeStampFormat", //$NON-NLS-1$
		                                                               "yyyy-MM-dd HH:mm:ss.SSS")); //$NON-NLS-1$

	public CustomLocalDateTimeSerializer() {
		this(null);
	}

	public CustomLocalDateTimeSerializer(Class<LocalDateTime> dt) {
		super(dt);
	}

	@Override
	public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt)
		throws JacksonException {
		gen.writeString(this.formatter.format(value));
	}
}
