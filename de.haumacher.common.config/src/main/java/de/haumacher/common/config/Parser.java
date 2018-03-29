package de.haumacher.common.config;

public interface Parser<T> extends Initializer<T> {
	T parse(String text);

	String unparse(T value);

	boolean equals(T value1, T value2);

	int hashCode(T value);
}