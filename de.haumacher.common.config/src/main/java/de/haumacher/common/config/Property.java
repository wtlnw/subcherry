package de.haumacher.common.config;

public interface Property {

	ValueDescriptor<?> getDescriptor();

	String getName();
	
	Kind getKind();

	Class<?> getType();
	
	Property getIndexProperty();

	Parser<Object> getParser();
	
}
