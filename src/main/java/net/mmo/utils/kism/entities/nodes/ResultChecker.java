/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 15.01.2021
 */

package net.mmo.utils.kism.entities.nodes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.AbstractEntity;

@Entity
@Setter
@Getter
@Slf4j
public class ResultChecker extends AbstractEntity implements Cloneable, Serializable
{
	private static final long serialVersionUID = -2258681869832385199L;

	public enum Condition {
		Equals(String.class),
		EqualsIgnoreCase(String.class),
		StartsWith(String.class),
		StartsWithIgnoreCase(String.class),
		EndsWith(String.class),
		EndsWithIgnoreCase(String.class),
		Contains(String.class),
		ContainsIgnoreCase(String.class),
		Matches(String.class),
		GreaterThan(Integer.class),
		GreaterOrEqual(Integer.class),
		LessThan(Integer.class),
		LessOrEqual(Integer.class),
		Within(Integer.class, Integer.class),
		Outside(Integer.class, Integer.class),
		;

		final Class<?>[] args;

		Condition(Class<?> ... args) {
			this.args = args;
		}
		int getNrArgs() {
			return this.args.length;
		}
	}

	protected Condition condition;
	protected ArrayList<String> operands = new ArrayList<>(0);

	@JsonIgnore
	transient ArrayList<Object> resolvedOperands;

	@JsonIgnore
	public void setOperand(String operand) {
		if (this.operands == null || this.operands.size() < 1) {
			this.operands = new ArrayList<>(1);
			this.operands.add(operand);
		} else {
			this.operands.set(0, operand);
		}
	}
	@JsonIgnore
	public String getOperand() {
		return (this.operands.size() > 0 ? this.operands.get(0) : null);
	}

	public boolean checkResult(Node contextNode, String value) throws Exception {
		if (this.condition != null) {
			if (this.resolvedOperands == null) {
				translateOperands(contextNode);
				if (this.operands.size() != this.condition.getNrArgs()) {
					throw new IllegalArgumentException(String.format("Illegal nr of arguments for condition '%s': %d (expected: %d)", //$NON-NLS-1$
					                                   this.condition, this.operands.size(),  this.condition.getNrArgs()));
				}
			}
			switch (this.condition) {
			case Equals:
				return Objects.equals(value, this.resolvedOperands.get(0));
			case EqualsIgnoreCase:
				return value != null && value.equalsIgnoreCase(((String)this.resolvedOperands.get(0)));
			case StartsWith:
				return value != null && value.startsWith(((String)this.resolvedOperands.get(0)));
			case StartsWithIgnoreCase:
				return value != null && this.resolvedOperands.get(0) != null && value.toUpperCase().startsWith(((String)this.resolvedOperands.get(0)).toUpperCase());
			case EndsWith:
				return value != null && value.endsWith(((String)this.resolvedOperands.get(0)));
			case EndsWithIgnoreCase:
				return value != null && this.resolvedOperands.get(0) != null && value.toUpperCase().endsWith(((String)this.resolvedOperands.get(0)).toUpperCase());
			case Contains:
				return value != null && value.contains(((String)this.resolvedOperands.get(0)));
			case ContainsIgnoreCase:
				return value != null && this.resolvedOperands.get(0) != null && value.toUpperCase().contains(((String)this.resolvedOperands.get(0)));
			case Matches:
				return value != null && value.matches(((String)this.resolvedOperands.get(0))); // we might want to cache the regexp some day...
			case GreaterThan:
				return value != null && (Integer.parseInt(value) > ((Integer)this.resolvedOperands.get(0)));
			case GreaterOrEqual:
				return value != null && (Integer.parseInt(value) >= ((Integer)this.resolvedOperands.get(0)));
			case LessThan:
				return value != null && (Integer.parseInt(value) < ((Integer)this.resolvedOperands.get(0)));
			case LessOrEqual:
				return value != null && (Integer.parseInt(value) <= ((Integer)this.resolvedOperands.get(0)));
			case Within:
				if (value == null) return false;
				{ // to overcome the stupid scoping in switch statements
					int intVal = Integer.parseInt(value);
					return (intVal >= (Integer)this.resolvedOperands.get(0)) && (intVal <= (Integer)this.resolvedOperands.get(1));
				}
			case Outside:
				if (value == null) return false;
				{ // to overcome the stupid scoping in switch statements
					int intVal = Integer.parseInt(value);
					return (intVal < (Integer)this.resolvedOperands.get(0)) || (intVal > (Integer)this.resolvedOperands.get(1));
				}
			default:
				throw new RuntimeException("Unexpected Condition: " + this.condition); //$NON-NLS-1$
			}
		}
		return true;
	}

	public void translateOperands(Node contextNode) {
		this.resolvedOperands = new ArrayList<>(this.operands.size());
		this.operands.forEach(operand -> {
			String resolvedValue;
			try {
				resolvedValue = contextNode.resolveProperties(operand);
			} catch (Exception ex) {
				resolvedValue = "Error resolving value '" + operand + "'"; //$NON-NLS-1$ //$NON-NLS-2$
				log.error(resolvedValue, ex);
				resolvedValue += " - " + ex.getMessage(); //$NON-NLS-1$
			}
			this.resolvedOperands.add(resolvedValue);
		});
	}



	@Override
	public String toString() {
		return new StringBuffer(this.getClass().getSimpleName())
			.append("{condition:").append(this.condition) //$NON-NLS-1$
			.append(", operand:'").append(this.operands) //$NON-NLS-1$
			.append("'}") //$NON-NLS-1$
			.toString();
	}
}
