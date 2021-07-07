/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 15.01.2021
 */

package net.mmo.utils.kism.entities.nodes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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

		final Class<?>[] argClasses;

		Condition(Class<?> ... argClasses) {
			this.argClasses = argClasses;
		}
		public Class<?>[] getArgClasses() {
			return this.argClasses;
		}
		public int getNrArgs() {
			return this.argClasses.length;
		}

		@Override
		public String toString() {
			return this.name() + "(" + Arrays.toString(this.argClasses) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected Condition condition;
	protected ArrayList<String> operands = new ArrayList<>(0);

	@JsonIgnore
	transient ArrayList<Object> resolvedOperands;

	@JsonIgnore
	public void setOperandN(int index, String operand) {
		synchronized(this) {
			if (this.operands == null || this.operands.size() < index) {
				this.operands = new ArrayList<>(index);
				while (this.operands.size() < index-1) this.operands.add(""); //$NON-NLS-1$
				this.operands.add(operand);
			} else {
				this.operands.set(index-1, operand);
			}
			this.resolvedOperands = null; // after changing any operand we need to resolve them again.
		}
 	}
	@JsonIgnore
	public String getOperandN(int index) {
		return (this.operands.size() >= index ? this.operands.get(index-1) : null);
	}

	@SuppressWarnings("null")
	public boolean checkResult(Node contextNode, String value) throws Exception {
		boolean result = true;
		if (this.condition != null) {
			int nrArgs = this.condition.getNrArgs();
			Object operand1;
			Object operand2;
			synchronized(this) {
				if (this.resolvedOperands == null) {
					this.resolvedOperands = resolveOperands(contextNode);
					if (this.operands.size() != nrArgs) {
						throw new IllegalArgumentException(String.format("Illegal nr of arguments for condition '%s': %d (expected: %d)", //$NON-NLS-1$
						                                   this.condition, this.operands.size(), nrArgs));
					}
				}
				operand1 = (nrArgs >= 1 ? this.resolvedOperands.get(0) : null);
				operand2 = (nrArgs >= 2 ? this.resolvedOperands.get(1) : null);
			}
			switch (this.condition) {
			case Equals:
				result = Objects.equals(value, operand1);
				break;
			case EqualsIgnoreCase:
				result = value != null && value.equalsIgnoreCase((String)operand1);
				break;
			case StartsWith:
				result = value != null && value.startsWith((String)operand1);
				break;
			case StartsWithIgnoreCase:
				result = value != null && operand1 != null && value.toUpperCase().startsWith(((String)operand1).toUpperCase());
				break;
			case EndsWith:
				result = value != null && value.endsWith((String)operand1);
				break;
			case EndsWithIgnoreCase:
				result = value != null && operand1 != null && value.toUpperCase().endsWith(((String)operand1).toUpperCase());
				break;
			case Contains:
				result = value != null && value.contains((String)operand1);
				break;
			case ContainsIgnoreCase:
				result = value != null && operand1 != null && value.toUpperCase().contains(((String)operand1).toUpperCase());
				break;
			case Matches:
				result = value != null && value.matches((String)operand1); // we might want to cache the regexp some day...
				break;
			case GreaterThan:
				result = value != null && (Integer.parseInt(value) > (Integer)operand1);
				break;
			case GreaterOrEqual:
				result = value != null && (Integer.parseInt(value) >= (Integer)operand1);
				break;
			case LessThan:
				result = value != null && (Integer.parseInt(value) < (Integer)operand1);
				break;
			case LessOrEqual:
				result = value != null && (Integer.parseInt(value) <= (Integer)operand1);
				break;
			case Within: {
					int intVal = Integer.parseInt(value);
					result = (intVal >= (Integer)operand1) && (intVal <= (Integer)operand2);
				}
				break;
			case Outside: {
					int intVal = Integer.parseInt(value);
					result = (intVal < (Integer)operand1) || (intVal > (Integer)operand2);
				}
				break;
			default:
				throw new RuntimeException("Unexpected Condition: " + this.condition); //$NON-NLS-1$
			}
		}
		log.trace("checking {}: operands: {}, value: {} => {}", //$NON-NLS-1$
		          this.condition, this.resolvedOperands, value.length() < 100 ? value : value.substring(0,99) + "...", result); //$NON-NLS-1$
		return result;
	}

	public ArrayList<Object> resolveOperands(Node contextNode) {
		ArrayList<Object> resolvedOps = new ArrayList<>(this.operands.size());
		this.operands.forEach(operand -> {
			Object resolvedValue;
			try {
				resolvedValue = contextNode.resolveProperties(operand);
				if (Integer.class.isAssignableFrom(this.condition.argClasses[resolvedOps.size()])) {
					resolvedValue = Integer.valueOf((String)resolvedValue);
				}
			} catch (Exception ex) {
				resolvedValue = "Error resolving value '" + operand + "'"; //$NON-NLS-1$ //$NON-NLS-2$
				log.error(resolvedValue.toString(), ex);
				resolvedValue += " - " + ex.getMessage(); //$NON-NLS-1$
			}
			resolvedOps.add(resolvedValue);
		});
		return resolvedOps;
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
