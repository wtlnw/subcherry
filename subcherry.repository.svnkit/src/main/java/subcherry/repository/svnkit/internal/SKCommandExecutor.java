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
package subcherry.repository.svnkit.internal;

import static subcherry.repository.svnkit.internal.Conversions.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNConflictVersion;
import org.tmatesoft.svn.core.wc.SVNConflictAction;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNOperation;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;
import org.tmatesoft.svn.core.wc2.SvnCopy;
import org.tmatesoft.svn.core.wc2.SvnMerge;
import org.tmatesoft.svn.core.wc2.SvnOperation;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

import subcherry.repository.svnkit.impl.MergeConflictCollector;
import subcherry.repository.svnkit.impl.SVNCommandContext;
import subcherry.repository.svnkit.impl.SVNDeleteLocalFile;
import subcherry.repository.svnkit.impl.SVNLocalMkDir;
import subcherry.repository.svnkit.impl.SVNScheduledTreeConflict;
import subcherry.repository.svnkit.impl.TouchCollector;

import com.subcherry.repository.command.Command;
import com.subcherry.repository.command.CommandVisitor;
import com.subcherry.repository.command.copy.Copy;
import com.subcherry.repository.command.merge.CommandContext;
import com.subcherry.repository.command.merge.CommandExecutor;
import com.subcherry.repository.command.merge.Merge;
import com.subcherry.repository.command.merge.ScheduledTreeConflict;
import com.subcherry.repository.command.wc.LocalDelete;
import com.subcherry.repository.command.wc.LocalMkDir;
import com.subcherry.repository.core.Depth;
import com.subcherry.repository.core.RepositoryException;

public class SKCommandExecutor extends CommandExecutor implements CommandVisitor<SvnOperation<?>, Void> {

	public SKCommandExecutor() {
		super();
	}
	
	@Override
	public CommandContext createContext() {
		return new SVNCommandContext();
	}
	
	@Override
	public void execute(CommandContext context, Command command) throws RepositoryException {
		try {
			SvnOperationFactory operationFactory = operationFactory(command);
			
			TouchCollector touchedFilesHandler = new TouchCollector(operationFactory.getEventHandler());
			operationFactory.setEventHandler(touchedFilesHandler);
			try {
				MergeConflictCollector conclictCollector =
					new MergeConflictCollector(
						context,
						operationFactory.getOperationHandler(),
						touchedFilesHandler.getTouchedFiles());
				operationFactory.setOperationHandler(conclictCollector);
				try {
					try {
						SvnOperation<?> operation = command.visit(this, null);
						operation.run();
					} catch (SVNException ex) {
						SVNErrorCode errorCode = ex.getErrorMessage().getErrorCode();
						boolean missingTarget = errorCode == SVNErrorCode.WC_PATH_NOT_FOUND;
						boolean alreadyExists = errorCode == SVNErrorCode.ENTRY_EXISTS;
						if (missingTarget || alreadyExists) {
							File path = (File) ex.getErrorMessage().getRelatedObjects()[0];
							SVNNodeKind nodeKind = SVNNodeKind.UNKNOWN;
							SVNConflictAction conflictAction;
							SVNConflictReason conflictReason;
							if (missingTarget) {
								conflictAction = SVNConflictAction.EDIT;
								conflictReason = SVNConflictReason.MISSING;
							} else {
								conflictAction = SVNConflictAction.ADD;
								conflictReason = SVNConflictReason.OBSTRUCTED;
							}
							SVNOperation operation = SVNOperation.MERGE;
							SVNConflictVersion sourceLeftVersion = null;
							SVNConflictVersion sourceRightVersion = null;
							List<SVNConflictDescription> conflicts =
								Arrays.<SVNConflictDescription> asList(new SVNTreeConflictDescription(path, nodeKind,
									conflictAction, conflictReason, operation, sourceLeftVersion, sourceRightVersion));
							// Like a tree conflict, where the target of the merge does not exist in the current working copy.
							conclictCollector.addConflict(path, conflicts);
						} else {
							throw ex;
						}
					}
				} finally {
					operationFactory.setOperationHandler(conclictCollector.getDelegate());
				}
			} finally {
				operationFactory.setEventHandler(touchedFilesHandler.getDelegate());
			}
		} catch (SVNException ex) {
			throw wrap(ex);
		}
	}

	private SvnOperationFactory operationFactory(Command op) {
		SvnOperationFactory operationFactory = ((SKOperationFactory) op.getOperationFactory()).impl();
		return operationFactory;
	}

	@Override
	public SvnCopy visitCopy(Copy command, Void arg) {
		SvnCopy impl = operationFactory(command).createCopy();
		
		impl.setSingleTarget(unwrap(command.getTarget()));
		impl.setDepth(unwrap(Depth.INFINITY));
		impl.addCopySource(unwrap2(command.getCopySource()));
		impl.setRevision(unwrap(command.getRevision()));
		impl.setFailWhenDstExists(command.getFailWhenDstExists());
		impl.setMakeParents(command.getMakeParents());
		impl.setMove(command.getMove());
		
		return impl;
	}

	@Override
	public SvnMerge visitMerge(Merge command, Void arg) {
		SvnMerge impl = operationFactory(command).createMerge();
		
		impl.setSource(unwrap(command.getSource()), command.getReintegrate());
		impl.setSingleTarget(unwrap(command.getTarget()));
		impl.setDepth(unwrap(command.getDepth()));
		impl.addRevisionRange(unwrap2(command.getRevisionRange()));
		impl.setAllowMixedRevisions(command.getAllowMixedRevisions());
		impl.setIgnoreAncestry(command.getIgnoreAnchestry());
		impl.setRecordOnly(command.getRecordOnly());
		
		return impl;
	}

	@Override
	public SVNDeleteLocalFile visitLocalDelete(LocalDelete command, Void arg) {
		SVNDeleteLocalFile impl = new SVNDeleteLocalFile(operationFactory(command));
		
		impl.setSingleTarget(unwrap(command.getTarget()));
		
		return impl;
	}

	@Override
	public SVNLocalMkDir visitLocalMkDir(LocalMkDir command, Void arg) {
		SVNLocalMkDir impl = new SVNLocalMkDir(operationFactory(command));
		
		impl.setSingleTarget(unwrap(command.getTarget()));
		
		return impl;
	}

	@Override
	public SVNScheduledTreeConflict visitScheduledTreeConflict(ScheduledTreeConflict command,
			Void arg) {
		SVNScheduledTreeConflict impl = new SVNScheduledTreeConflict(operationFactory(command));
		
		impl.setSingleTarget(unwrap(command.getTarget()));
		impl.setAction(unwrap(command.getAction()));
		impl.setReason(unwrap(command.getReason()));
		
		return impl;
	}

}
