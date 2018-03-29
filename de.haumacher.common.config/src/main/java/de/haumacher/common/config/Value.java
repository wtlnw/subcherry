package de.haumacher.common.config;

public interface Value {

	ValueDescriptor<?> descriptor();
	
	Object value(Property property);
	
	void putValue(Property property, Object value);
	
}
