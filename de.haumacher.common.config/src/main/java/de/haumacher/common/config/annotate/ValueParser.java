package de.haumacher.common.config.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.haumacher.common.config.Parser;

/**
 * @version   $Revision: 106 $  $Author: dbu $  $Date: 2012-04-26 10:00:26 +0200 (Thu, 26 Apr 2012) $
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ValueParser {
	
	Class<? extends Parser> value();

}

