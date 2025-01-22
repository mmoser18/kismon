/**
 * Copyright © 2020-2025 by Michael Moser
 *
 * @author Michael Moser (17732576+mmoser18@users.noreply.github.com)
 */

package net.mmo.utils.kism.entities.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.mmo.utils.kism.entities.AbstractEntity;
import net.mmo.utils.kism.entities.nodes.Node.State;

/**
 * Provides the functionality to check the result of a query or request for specific content
 */
@SuppressWarnings("javadoc")
@Setter
@Getter
@Slf4j
public class ResultChecker extends AbstractEntity
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
		RangesAscending(Integer.class, Integer.class),
		RangesDescending(Integer.class, Integer.class),
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
	protected ArrayList<String> operands;

	@JsonIgnore
	transient ArrayList<Object> resolvedOperands;

	@JsonIgnore
	public void setOperandN(int index, String operand) {
		synchronized(this) { // synchronized so that the operands can't be changed while we use them
			int nrExpectedArgs = this.condition.getNrArgs();
			if (this.operands == null) {
				this.operands = new ArrayList<>(this.condition != null ? nrExpectedArgs : 0);
			}
			if (index >= nrExpectedArgs) {
				throw new IllegalArgumentException(String.format("Illegal nr. of arguments for condition '%s': %d (max: %d)", //$NON-NLS-1$
				                                                 this.condition, index+1, this.operands.size()));

			} else if (index >= this.operands.size()) {
				while (index > this.operands.size()) this.operands.add("-undefined-"); //$NON-NLS-1$
				this.operands.add(operand);
			} else { // index < size:
				this.operands.set(index, operand);
			}
			this.resolvedOperands = null; // after changing any operand we need to resolve them again.
		}
 	}
	@JsonIgnore
	public String getOperandN(int index) {
		return (this.operands != null ? (index < this.operands.size() ? this.operands.get(index) : null) : null);
	}

	@SuppressWarnings({"null", "unused"})
	public State checkResult(Node contextNode, String value) throws Exception {
		State result = State.OK;
		if (this.condition != null) {
			int nrExpectedArgs = this.condition.getNrArgs();
			Object operand1;
			Object operand2;
			Object operand3;
			synchronized(this) { // synchronized so that the operands can't be modified while we change them
				if (this.resolvedOperands == null) {
					this.resolvedOperands = resolveOperands(contextNode);
					if (this.operands.size() != nrExpectedArgs) {
						throw new IllegalArgumentException(String.format("Illegal nr. of arguments for condition '%s': %d (expected: %d)", //$NON-NLS-1$
						                                   this.condition, this.operands.size(), nrExpectedArgs));
					}
				}
				operand1 = (nrExpectedArgs >= 1 ? this.resolvedOperands.get(0) : null);
				operand2 = (nrExpectedArgs >= 2 ? this.resolvedOperands.get(1) : null);
				operand3 = (nrExpectedArgs >= 3 ? this.resolvedOperands.get(2) : null);
			}
			switch (this.condition) {
			case Equals:
				result = (Objects.equals(value, operand1) ? State.OK : State.FAILED);
				break;
			case EqualsIgnoreCase:
				result = (value != null && value.equalsIgnoreCase((String)operand1) ? State.OK : State.FAILED);
				break;
			case StartsWith:
				result = (value != null && value.startsWith((String)operand1) ? State.OK : State.FAILED);
				break;
			case StartsWithIgnoreCase:
				result = (value != null && operand1 != null && value.toUpperCase().startsWith(((String)operand1).toUpperCase()) ? State.OK : State.FAILED);
				break;
			case EndsWith:
				result = (value != null && value.endsWith((String)operand1) ? State.OK : State.FAILED);
				break;
			case EndsWithIgnoreCase:
				result = (value != null && operand1 != null && value.toUpperCase().endsWith(((String)operand1).toUpperCase()) ? State.OK : State.FAILED);
				break;
			case Contains:
				result = (value != null && value.contains((String)operand1) ? State.OK : State.FAILED);
				break;
			case ContainsIgnoreCase:
				result = (value != null && operand1 != null && value.toUpperCase().contains(((String)operand1).toUpperCase()) ? State.OK : State.FAILED);
				break;
			case Matches:
				result = (value != null && value.matches((String)operand1) ? State.OK : State.FAILED); // we might want to cache the regexp some day...
				break;
			case GreaterThan:
				result = (value != null && (Integer.parseInt(value.trim()) > (Integer)operand1) ? State.OK : State.FAILED);
				break;
			case GreaterOrEqual:
				result = (value != null && (Integer.parseInt(value.trim()) >= (Integer)operand1) ? State.OK : State.FAILED);
				break;
			case LessThan:
				result = (value != null && (Integer.parseInt(value.trim()) < (Integer)operand1) ? State.OK : State.FAILED);
				break;
			case LessOrEqual:
				result = (value != null && (Integer.parseInt(value.trim()) <= (Integer)operand1) ? State.OK : State.FAILED);
				break;
			case Within:
				if (value != null) {
					int intVal = Integer.parseInt(value.trim());
					result = ((intVal >= (Integer)operand1) && (intVal <= (Integer)operand2) ? State.OK : State.FAILED);
				} else {
					result = State.FAILED;
				}
				break;
			case Outside:
				if (value != null) {
					int intVal = Integer.parseInt(value.trim());
					result = ((intVal < (Integer)operand1) || (intVal > (Integer)operand2) ? State.OK : State.FAILED);
				} else {
					result = State.FAILED;
				}
				break;
			case RangesAscending:
				if (value != null) {
					int intVal = Integer.parseInt(value.trim());
					if (intVal <= (Integer)operand1) result = State.OK;
					else if (intVal <= (Integer)operand2) result = State.DEGRADED;
					else result = State.FAILED;
				} else {
					result = State.FAILED;
				}
				break;
			case RangesDescending:
				if (value != null) {
					int intVal = Integer.parseInt(value.trim());
					if (intVal >= (Integer)operand1) result = State.OK;
					else if (intVal >= (Integer)operand2) result = State.DEGRADED;
					else result = State.FAILED;
				} else {
					result = State.FAILED;
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
		for (int idx = 0; idx < this.condition.argClasses.length; idx++) {
			String operand = this.operands.get(idx);
			Object resolvedValue;
			try {
				resolvedValue = operand.isEmpty() ? operand : contextNode.resolveProperties(operand);
				log.trace("ResultChecker.resolveOperands(): operand {}: '{}', resolved value:'{}'", idx, operand, resolvedValue); //$NON-NLS-1$
				Class<?> argClass = this.condition.argClasses[idx];
				if (Integer.class.isAssignableFrom(argClass)) {
					resolvedValue = Integer.valueOf((String)resolvedValue);
				} else if (Double.class.isAssignableFrom(argClass)) {
					resolvedValue = Double.valueOf((String)resolvedValue);
				}
			} catch (Exception ex) {
				resolvedValue = "Error resolving/converting '" + this.condition + "'-operand " + idx + ": '" + operand + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				log.error(resolvedValue.toString(), ex);
				resolvedValue += " - " + ex.getMessage(); //$NON-NLS-1$
			}
			resolvedOps.add(resolvedValue);
		}
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
