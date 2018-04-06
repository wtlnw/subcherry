package com.subcherry.ui;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;

/**
 * A {@link ModifyListener} implementation which delays {@link ModifyEvent}
 * processing for the specified amount of time.
 * 
 * <p>
 * The code is guaranteed to process only the very last event which occurred
 * before processing is run. Due to the delayed nature the
 * {@link ModifyEvent#getSource()} may have already been disposed.
 * </p>
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class DelayedModifyListener implements ModifyListener {

	/**
	 * The default delay in milliseconds for the callback to be executed.
	 */
	private static final int DEFAULT_DELAY = 500;

	/**
	 * @see #getCallback()
	 */
	private final Callback _callback;

	/**
	 * @see #getDelay()
	 */
	private final int _delay;

	/**
	 * Create a {@link DelayedModifyListener}.
	 * 
	 * @param listener
	 *            the {@link ModifyListener} to be executed with the default delay
	 */
	public DelayedModifyListener(final ModifyListener listener) {
		this(listener, DEFAULT_DELAY);
	}

	/**
	 * Create a {@link DelayedModifyListener}.
	 * 
	 * @param listener
	 *            the {@link ModifyListener} to be executed with the given delay
	 * @param delay
	 *            see {@link #getDelay()}
	 */
	public DelayedModifyListener(final ModifyListener listener, final int delay) {
		_callback = new Callback(listener);
		_delay = delay;
	}

	@Override
	public void modifyText(final ModifyEvent event) {
		final Callback callback = getCallback();

		if (!callback.isScheduled()) {
			callback.schedule(event);
			event.display.timerExec(getDelay(), callback);
		} else {
			callback.schedule(event);
		}
	}

	/**
	 * @return the amount of milliseconds after which to execute
	 *         {@link #getCallback()}
	 */
	protected int getDelay() {
		return _delay;
	}

	/**
	 * @return the {@link Callback} to be executed after {@link #getDelay()}
	 */
	protected Callback getCallback() {
		return _callback;
	}
	
	/**
	 * A {@link Runnable} implementation to be executed in the user interface thread
	 * after the configured delay.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	private static final class Callback implements Runnable {

		/**
		 * The {@link ModifyListener} to dispatch event processing to.
		 */
		private final ModifyListener _listener;

		/**
		 * The last {@link ModifyEvent} which was scheduled for processing.
		 */
		private ModifyEvent _event;

		/**
		 * @see #isScheduled()
		 */
		private boolean _scheduled;

		/**
		 * Create a {@link Callback}.
		 * 
		 * @param listener
		 *            the {@link ModifyListener} to dispatch event processing to
		 */
		public Callback(final ModifyListener listener) {
			_listener = listener;
		}

		/**
		 * Schedule processing of the given event.
		 * 
		 * @param event
		 *            the {@link ModifyEvent} to be processed when this callback is run
		 */
		void schedule(final ModifyEvent event) {
			_event = event;
			_scheduled = true;
		}

		/**
		 * @return {@code true} if this callback has already been scheduled
		 */
		boolean isScheduled() {
			return _scheduled;
		}

		@Override
		public void run() {
			try {
				if(_event != null) {
					_listener.modifyText(_event);
				}
			} finally {
				_scheduled = false;
				_event = null;
			}
		}
	}
}
