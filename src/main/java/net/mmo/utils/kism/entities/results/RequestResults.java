
package net.mmo.utils.kism.entities.results;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;
import net.mmo.utils.kism.entities.AbstractEntity;

@Entity
@Setter
@Getter
public class RequestResults extends AbstractEntity implements Serializable
{
	private static final long serialVersionUID = -974098342485642213L;

	public final static String PROPERTYNAME_DURATION  = "duration";  //$NON-NLS-1$
	public final static String PROPERTYNAME_TIMESTAMP  = "timestamp";  //$NON-NLS-1$

	protected String name;

	protected long duration;

	//	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	//	@JsonSerialize(using = LocalDateTimeSerializer.class)
	//	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
	protected LocalDateTime timestamp;

	protected String result;


	public RequestResults(String name, long duration, LocalDateTime timestamp, String result) {
		setName(name);
		setDuration(duration);
		setTimestamp(timestamp);
		setResult(result);
	}


	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.setLength(buf.length()-1);
		return buf
			.append(", name:").append(getName()) //$NON-NLS-1$
			.append(", duration:").append(this.duration) //$NON-NLS-1$
			.append(", timestamp:").append(this.timestamp) //$NON-NLS-1$
			.append(", result:").append(this.result) //$NON-NLS-1$
			.append('}')
			.toString();
	}
}
