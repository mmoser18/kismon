/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 15.01.2021
 */

package net.mmo.utils.kism.entities;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class ResultChecker extends AbstractEntity implements Cloneable, Serializable
{
	private static final long serialVersionUID = -2258681869832385199L;

	public enum Condition {
		Equals,
		EqualsIgnoreCase,
		StartsWith,
		StartsWithIgnoreCase,
		EndsWith,
		EndsWithIgnoreCase,
		Contains,
		ContainsIgnoreCase,
		Matches,
		;
	}

	protected Condition condition;
	protected String operand;

	public boolean checkResult(Node contextNode, String value) throws Exception {
		String resolvedOperand = (this.operand != null ? contextNode.resolveProperties(this.operand) : null);
		switch (this.condition) {
		case Equals:
			return Objects.equals(value, resolvedOperand);
		case EqualsIgnoreCase:
			return value != null && value.equalsIgnoreCase(resolvedOperand);
		case StartsWith:
			return value != null && value.startsWith(resolvedOperand);
		case StartsWithIgnoreCase:
			return value != null && resolvedOperand != null && value.toUpperCase().startsWith(resolvedOperand.toUpperCase());
		case EndsWith:
			return value != null && value.endsWith(resolvedOperand);
		case EndsWithIgnoreCase:
			return value != null && resolvedOperand != null && value.toUpperCase().endsWith(resolvedOperand.toUpperCase());
		case Contains:
			return value != null && value.contains(resolvedOperand);
		case ContainsIgnoreCase:
			return value != null && resolvedOperand != null && value.toUpperCase().contains(resolvedOperand.toUpperCase());
		case Matches:
			return value != null && value.matches(resolvedOperand); // we might want to cache the regexp some day...
		default:
			throw new RuntimeException("Unexpected Condition: " + this.condition); //$NON-NLS-1$
		}
	}

	@Override
	public String toString() {
		return new StringBuffer(this.getClass().getSimpleName())
			.append("{condition:").append(this.condition) //$NON-NLS-1$
			.append(", operand:'").append(this.operand) //$NON-NLS-1$
			.append("'}") //$NON-NLS-1$
			.toString();
	}
}
