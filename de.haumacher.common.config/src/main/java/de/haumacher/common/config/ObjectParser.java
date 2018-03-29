package de.haumacher.common.config;


public abstract class ObjectParser<T> implements Parser<T> {
	
	@Override
	public T init() {
		return null;
	}
	
	@Override
	public boolean equals(T value1, T value2) {
		if (value1 == null) {
			return value2 == null;
		} else {
			return value1.equals(value2);
		}
	}

	@Override
	public int hashCode(T value) {
		if (value == null) {
			return 0;
		}

		return value.hashCode();
	}
}