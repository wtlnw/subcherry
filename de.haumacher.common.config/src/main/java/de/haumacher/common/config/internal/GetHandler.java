package de.haumacher.common.config.internal;


import de.haumacher.common.config.internal.ValueDescriptorImpl.ValueImpl;

class GetHandler extends PropertyHandler {

	public GetHandler(int index) {
		super(index);
	}

	@Override
	public Object handlePropertyAccess(Object self, ValueImpl impl, Object[] args) {
		return impl.values[index];
	}

}
