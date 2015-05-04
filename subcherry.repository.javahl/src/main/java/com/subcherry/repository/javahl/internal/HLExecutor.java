/*
 * TimeCollect records time you spent on your development work.
 * Copyright (C) 2015 Bernhard Haumacher and others
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
package com.subcherry.repository.javahl.internal;

import static com.subcherry.repository.javahl.internal.Conversions.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.callback.CommitCallback;
import org.apache.subversion.javahl.callback.CommitMessageCallback;
import org.apache.subversion.javahl.types.Depth;
import org.apache.subversion.javahl.types.Revision;
import org.apache.subversion.javahl.types.RevisionRange;

import com.subcherry.repository.command.Command;
import com.subcherry.repository.command.CommandVisitor;
import com.subcherry.repository.command.copy.Copy;
import com.subcherry.repository.command.copy.CopySource;
import com.subcherry.repository.command.merge.CommandContext;
import com.subcherry.repository.command.merge.CommandExecutor;
import com.subcherry.repository.command.merge.ConflictAction;
import com.subcherry.repository.command.merge.ConflictReason;
import com.subcherry.repository.command.merge.Merge;
import com.subcherry.repository.command.merge.ScheduledTreeConflict;
import com.subcherry.repository.command.merge.TreeConflictDescription;
import com.subcherry.repository.command.wc.LocalDelete;
import com.subcherry.repository.command.wc.LocalMkDir;
import com.subcherry.repository.core.RepositoryException;
import com.subcherry.repository.core.RepositoryRuntimeException;
import com.subcherry.repository.core.Target;
import com.subcherry.repository.core.Target.FileTarget;

class HLExecutor extends CommandExecutor implements CommandVisitor<Void, HLCommandContext> {

	@Override
	public void execute(CommandContext context, Command command) throws RepositoryException {
		try {
			command.visit(this, (HLCommandContext) context);
		} catch (RepositoryRuntimeException ex) {
			throw ex.getCause();
		}
	}

	@Override
	public CommandContext createContext() {
		return new HLCommandContext();
	}

	@Override
	public Void visitCopy(Copy command, HLCommandContext arg) {
		HLClient client = client(command);

		File target = unwrapFile(command.getTarget());
		if (target.exists()) {
			arg.getConflicts().put(target,
				list(new TreeConflictDescription(ConflictAction.ADDED, ConflictReason.OBSTRUCTED)));
		} else {
			CopySource[] sources = new CopySource[] { command.getCopySource() };
			boolean isMove = command.getMove();
			boolean makeParents = command.getMakeParents();
			boolean failWhenDstExists = command.getFailWhenDstExists();
			try {
				client.copy(sources, target, isMove, makeParents, failWhenDstExists);
			} catch (RepositoryException ex) {
				throw unchecked(ex);
			}
		}
		
		return null;
	}

	@Override
	public Void visitMerge(Merge command, HLCommandContext arg) {
		String path = unwrap(command.getSource());
		Revision pegRevision = unwrap(command.getSource().getPegRevision());

		List<RevisionRange> revisions = Collections.singletonList(unwrap(command.getRevisionRange()));
		String localPath = unwrap(unwrapFile(command.getTarget()));
        boolean force = false;
        Depth depth = unwrap(command.getDepth());
        boolean ignoreAncestry = command.getIgnoreAnchestry();
        boolean dryRun = false; 
		boolean recordOnly = command.getRecordOnly();
        
		try {
			client(command).impl().merge(path, pegRevision, revisions, localPath, force, depth, ignoreAncestry, dryRun,
				recordOnly);
		} catch (ClientException ex) {
			throw unchecked(wrap(ex));
		}
		return null;
	}

	@Override
	public Void visitLocalDelete(LocalDelete command, HLCommandContext arg) {
		File target = unwrapFile(command.getTarget());
		if (!target.exists()) {
			arg.getConflicts().put(target,
				list(new TreeConflictDescription(ConflictAction.DELETED, ConflictReason.MISSING)));
		} else {
			Set<String> path = Collections.singleton(unwrap(target));
			boolean force = false;
			boolean keepLocal = false;
			Map<String, String> revpropTable = null;
			CommitMessageCallback handler = null;
			CommitCallback callback = null;
			try {
				client(command).impl().remove(path, force, keepLocal, revpropTable, handler, callback);
			} catch (ClientException ex) {
				throw unchecked(wrap(ex));
			}
		}
		return null;
	}

	@Override
	public Void visitLocalMkDir(LocalMkDir command, HLCommandContext arg) {
		File target = unwrapFile(command.getTarget());
		if (target.exists()) {
			if (target.isDirectory()) {
				// Silently ignore conflict on directories.
			} else {
				arg.getConflicts().put(target,
					list(new TreeConflictDescription(ConflictAction.ADDED, ConflictReason.OBSTRUCTED)));
			}
		} else {
			Set<String> path = Collections.singleton(unwrap(target));
			boolean makeParents = false;
			Map<String, String> revpropTable = null;
			CommitMessageCallback handler = null;
			CommitCallback callback = null;
			try {
				client(command).impl().mkdir(path, makeParents, revpropTable, handler, callback);
			} catch (ClientException ex) {
				throw unchecked(wrap(ex));
			}
		}
		return null;
	}

	@Override
	public Void visitScheduledTreeConflict(ScheduledTreeConflict command, HLCommandContext arg) {
		File target = unwrapFile(command.getTarget());
		arg.getConflicts().put(target, list(new TreeConflictDescription(command.getAction(), command.getReason())));
		return null;
	}

	private static HLClient client(Command command) {
		return (HLClient) ((HLOperationFactory) command.getOperationFactory()).getClientManager().getClient();
	}

	static File unwrapFile(Target target) {
		File dst;
		switch (target.kind()) {
			case FILE:
				dst = ((FileTarget) target).getFile();
				break;
			default:
				throw new UnsupportedOperationException("Cannot directly copy to server.");
		}
		return dst;
	}

	static RepositoryRuntimeException unchecked(RepositoryException ex) {
		return new RepositoryRuntimeException(ex);
	}

}