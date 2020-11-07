
package net.mmo.utils.kism.entities.history;

import java.time.LocalDateTime;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;
import net.mmo.utils.kism.entities.AbstractEntity;
import net.mmo.utils.kism.entities.nodes.VisibleNodeType;

@SuppressWarnings("javadoc")
@Entity
@Setter
@Getter
public class HistoryInfo extends AbstractEntity
{
	private static final long serialVersionUID = -974098342485642213L;

	public final static String PROPERTYNAME_DURATION  = "duration";  //$NON-NLS-1$
	public final static String PROPERTYNAME_TIMESTAMP  = "timestamp";  //$NON-NLS-1$

	public static final double NanoToMilliFactor = 1000000.0;

	/** name of node that generated this result */
	protected String name;

	/** simple name of node class that generated this result */
	protected VisibleNodeType nodeType;

	/** duration of request (in milli-seconds) */
	protected double duration;

	/** timestamp of request(-start) */
	//	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	//	@JsonSerialize(using = LocalDateTimeSerializer.class)
	//	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
	protected LocalDateTime timestamp;

	/** node-specific string describing the result */
	protected String result;


	protected HistoryInfo() { // required for deserialization
		// empty
	}

	public HistoryInfo(String name, Class<?> nodeClass, Long duration, LocalDateTime timestamp, String result) {
		this(name, nodeClass.getSimpleName(), (duration / NanoToMilliFactor), timestamp, result);
	}

	public HistoryInfo(String name, String nodeType, double duration, LocalDateTime timestamp, String result) {
		setName(name);
		setNodeType(VisibleNodeType.valueOf(nodeType));
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
			.append(", nodeType:").append(getNodeType()) //$NON-NLS-1$
			.append(", duration:").append(this.duration) //$NON-NLS-1$
			.append(", timestamp:").append(this.timestamp) //$NON-NLS-1$
			.append(", result:").append(this.result) //$NON-NLS-1$
			.append('}')
			.toString();
	}
}
