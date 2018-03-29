package de.haumacher.common.config.internal;

import de.haumacher.common.config.Initializer;
import de.haumacher.common.config.internal.ValueDescriptorImpl.ValueImpl;

public class NonNullSetHandler extends SetHandler {

	private final Initializer<?> initializer;

	public NonNullSetHandler(int index, Initializer<?> initializer) {
		super(index);
		
		this.initializer = initializer;
	}

	@Override
	public Object handlePropertyAccess(Object self, ValueImpl impl, Object[] args) {
		if (args[0] == null) {
			args[0] = initializer.init();
		}
		return super.handlePropertyAccess(self, impl, args);
	}

}
