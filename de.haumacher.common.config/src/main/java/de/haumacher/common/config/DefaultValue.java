package de.haumacher.common.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {
	
	boolean booleanValue() default false;
	
	byte byteValue() default 0;
	
	char charValue() default 0;
	
	short shortValue() default 0;
	
	int intValue() default 0;
	
	long longValue() default 0l;
	
	float floatValue() default 0f;
	
	double doubleValue() default 0d;
	
	String stringValue() default "";
	
	Class<? extends Initializer> initializer() default Initializer.class;
	
}
