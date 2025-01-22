/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@SuppressWarnings("javadoc")
public class CustomLocalDateTimeSerializer extends StdSerializer<LocalDateTime>
{
	private static final long serialVersionUID = -5040408858032478855L;

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
	public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider arg2)
		throws IOException, JsonProcessingException {
		gen.writeString(this.formatter.format(value));
	}
}
