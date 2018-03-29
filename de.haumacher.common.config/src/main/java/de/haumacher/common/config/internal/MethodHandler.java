package de.haumacher.common.config.internal;

import de.haumacher.common.config.internal.ValueDescriptorImpl.ValueImpl;


interface MethodHandler {

	Object handlePropertyAccess(Object self, ValueImpl impl, Object...args);

}
