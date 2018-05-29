/*
 * SubCherry - Cherry Picking with Trac and Subversion
 * Copyright (C) 2018 Bernhard Haumacher and others
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.subcherry.ui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;

import com.subcherry.Configuration;
import com.subcherry.MergeCommitHandler.UpdateableRevisionRewriter;
import com.subcherry.PortingTickets;
import com.subcherry.commit.CommitContext;
import com.subcherry.commit.MessageRewriter;
import com.subcherry.merge.MergeHandler;
import com.subcherry.repository.command.Client;
import com.subcherry.repository.command.ClientManager;
import com.subcherry.repository.command.merge.CommandExecutor;
import com.subcherry.trac.TracConnection;
import com.subcherry.ui.model.SubcherryTree;
import com.subcherry.ui.model.SubcherryTreeRevisionNode;
import com.subcherry.ui.model.SubcherryTreeTicketNode;
import com.subcherry.utils.PathParser;

/**
 * Instances of this class provide access to necessary data for the entire merge
 * process.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class SubcherryMergeContext {

	/**
	 * @see #addMergeListener()
	 * @see #removeMergeListener()
	 */
	private final ListenerList<SubcherryMergeListener> _listeners = new ListenerList<>();
	
	/**
	 * @see #getClientManager()
	 */
	private final ClientManager _manager;
	
	/**
	 * @see #getConfiguration()
	 */
	private final Configuration _config;
	
	/**
	 * @see #getTracConnection()
	 */
	private final TracConnection _trac;
	
	/**
	 * @see #getCommitContext()
	 */
	private final CommitContext _commitContext;
	
	/**
	 * @see #getMergeHandler()
	 */
	private final MergeHandler _mergeHandler;
	
	/**
	 * @see #getMessageRewriter()
	 */
	private final MessageRewriter _messageRewriter;
	
	/**
	 * @see #getEntries();
	 */
	private final List<SubcherryMergeEntry> _entries;
	
	/**
	 * Create a {@link SubcherryMergeContext}.
	 * 
	 * @param tree
	 *            the {@link SubcherryTree} to create the merge context for
	 */
	public SubcherryMergeContext(final SubcherryTree tree) {
		_manager = tree.getClientManager();
		_config = tree.getConfiguration();
		_trac = tree.getTracConnection();
		_commitContext = new CommitContext(_manager.getClient(), _manager.getClient());
		_mergeHandler = new MergeHandler(_manager, _config, new PathParser(_config), new HashSet<>(Arrays.asList(_config.getModules())));
		_messageRewriter = MessageRewriter.createMessageRewriter(getConfiguration(), new PortingTickets(_config, _trac), new UpdateableRevisionRewriter());
		_entries = init(tree);
	}
	
	/**
	 * @param tree
	 *            the {@link SubcherryTree} to initialize the context with
	 * @return a (possibly empty) {@link List} of {@link SubcherryMergeEntry}s to be
	 *         merged in this context
	 */
	private List<SubcherryMergeEntry> init(final SubcherryTree tree) {
		final List<SubcherryMergeEntry> entries = new ArrayList<>();
		
		// resolve only selected changes for selected tickets
		for (final SubcherryTreeTicketNode ticket : tree.getSelectedTickets()) {
			for (final SubcherryTreeRevisionNode revision : ticket.getSelectedChanges()) {
				entries.add(new SubcherryMergeEntry(this, revision.getChange()));
			}
		}
		
		// make sure to sort the entries by their revision in ascending order
		Collections.sort(entries, new Comparator<SubcherryMergeEntry>() {
			@Override
			public int compare(final SubcherryMergeEntry o1, final SubcherryMergeEntry o2) {
				return Long.valueOf(o1.getChange().getRevision()).compareTo(Long.valueOf(o2.getChange().getRevision()));
			}
		});
		
		return entries;
	}
	
	/**
	 * Register the given listener for merge updates.
	 * 
	 * <p>
	 * Note: Has no effect if the given listener has already been registered.
	 * </p>
	 * 
	 * @param listener
	 *            the {@link SubcherryMergeListener} to register
	 */
	public void addMergeListener(final SubcherryMergeListener listener) {
		_listeners.add(listener);
	}
	
	/**
	 * Unregister the given listener from merge update notification.
	 * 
	 * <p>
	 * Note: Has no effect if the given listener was not registered.
	 * </p>
	 * 
	 * @param listener
	 *            the {@link SubcherryMergeListener} to unregister
	 */
	public void removeMergeListener(final SubcherryMergeListener listener) {
		_listeners.remove(listener);
	}

	/**
	 * Notify all currently registered {@link SubcherryMergeListener}s that the
	 * given {@link SubcherryMergeEntry} state has changed.
	 * 
	 * @param entry
	 *            the {@link SubcherryMergeEntry} whose state changed
	 * @param oldState
	 *            the previous {@link SubcherryMergeState}
	 * @param newState
	 *            the new {@link SubcherryMergeState}
	 */
	/* package private */ void notifyStateChanged(final SubcherryMergeEntry entry, final SubcherryMergeState oldState, final SubcherryMergeState newState) {
		_listeners.forEach(listener -> listener.onStateChanged(entry, oldState, newState));
	}
	
	/**
	 * Notify all currently registered {@link SubcherryMergeListener}s that the
	 * given {@link SubcherryMergeEntry} property has changed.
	 * 
	 * @param entry
	 *            the {@link SubcherryMergeEntry} whose property changed
	 */
	/* package private */ void notifyEntryChanged(final SubcherryMergeEntry entry) {
		_listeners.forEach(listener -> listener.onEntryChanged(entry));
	}
	
	/**
	 * @return the {@link ClientManager} to be used for {@link Client} access
	 */
	public ClientManager getClientManager() {
		return _manager;
	}

	/**
	 * @return the {@link Configuration} instance for the entire merge process
	 */
	public Configuration getConfiguration() {
		return _config;
	}
	
	/**
	 * @return the {@link TracConnection} for ticket access
	 */
	public TracConnection getTracConnection() {
		return _trac;
	}
	
	/**
	 * @return the {@link MergeHandler} to be used for merge operations
	 */
	public MergeHandler getMergeHandler() {
		return _mergeHandler;
	}
	
	/**
	 * @return the {@link CommandExecutor} to be used for merge operation execution
	 */
	public CommandExecutor getCommandExecutor() {
		return getClientManager().getOperationsFactory().getExecutor();
	}
	
	/**
	 * @return the {@link CommitContext} to commit the changes in
	 */
	public CommitContext getCommitContext() {
		return _commitContext;
	}
	
	/**
	 * @return the {@link MessageRewriter} to be used for commit message editing
	 */
	public MessageRewriter getMessageRewriter() {
		return _messageRewriter;
	}

	/**
	 * @return a (possibly empty) {@link List} of {@link SubcherryMergeEntry} to be
	 *         merged in this context
	 */
	public List<SubcherryMergeEntry> getAllEntries() {
		return _entries;
	}

	/**
	 * @return a (possibly empty) {@link List} of pending
	 *         {@link SubcherryMergeEntry}s
	 */
	public List<SubcherryMergeEntry> getPendingEntries() {
		final List<SubcherryMergeEntry> entries = getAllEntries();
		
		for (int i = 0; i < entries.size(); i++) {
			// continue with the next pending entry
			if(entries.get(i).getState().isPending()) {
				return entries.subList(i, entries.size());
			}
		}
		
		return Collections.emptyList();
	}
	
	/**
	 * @return the current {@link SubcherryMergeEntry} being the very first pending
	 *         entry or {@code null} if none
	 */
	public SubcherryMergeEntry getCurrentEntry() {
		final Iterator<SubcherryMergeEntry> pending = getPendingEntries().iterator();

		if (pending.hasNext()) {
			return pending.next();
		}

		return null;
	}
	
	/**
	 * Dispose this instance and free all allocated system resources.
	 */
	public void dispose() {
		final SubcherryMergeEntry entry = getCurrentEntry();

		if (entry != null) {
			entry.reset();
		}
	}
}
