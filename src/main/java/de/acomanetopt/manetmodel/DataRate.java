package de.acomanetopt.manetmodel;

import de.acomanetopt.manetmodel.Unit.Type;

public class DataRate {
	private long bits;

	public DataRate(double value, Unit.Type type) {
		this.bits = toBits(value, type);
	}

	public DataRate(long bits) {
		this.bits = bits;
	}

	public DataRate() {
	}

	protected long toBits(double value, Unit.Type type) {
		long factor = Unit.getFactor(type);
		return Math.round(value * factor);
	}

	public void set(long bits) {
		this.bits = bits;
	}

	public final long get() {
		return bits;
	}

	@Override
	public String toString() {
		Type type = Unit.getNextLowerType(bits);
		return new StringBuffer().append(bits / (double) Unit.getFactor(type)).append(" ").append(type).toString();
	}
}
