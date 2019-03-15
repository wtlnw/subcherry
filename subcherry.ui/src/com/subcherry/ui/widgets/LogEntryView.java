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
package com.subcherry.ui.widgets;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ResourceBundle.Control;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

import com.subcherry.repository.core.LogEntry;
import com.subcherry.repository.core.LogEntryPath;
import com.subcherry.repository.core.NodeKind;

/**
 * A {@link SashForm} displaying detailed information for {@link LogEntry} instances.
 * 
 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
 */
public class LogEntryView extends SashForm {

	/**
	 * @see #revision()
	 */
	private Text _revision;
	
	/**
	 * @see #timestamp()
	 */
	private Text _timestamp;
	
	/**
	 * @see #author()
	 */
	private Text _author;
	
	/**
	 * @see #message()
	 */
	private Text _message;
	
	/**
	 * @see #resources()
	 */
	private TreeViewer _resources;

	/**
	 * Create a {@link LogEntryView}.
	 * 
	 * @param parent
	 *            see {@link #getParent()}
	 * @param style
	 *            see {@link #getStyle()}
	 */
	public LogEntryView(final Composite parent, final int style) {
		super(parent, style);
		
		createFields();
		createPaths();
	}
	
	/**
	 * @return the {@link Text} control displaying the {@link #getLogEntry()}'s
	 *         author
	 */
	public Text author() {
		return _author;
	}
	
	/**
	 * @return the {@link Text} control displaying the {@link #getLogEntry()}'s
	 *         commit message
	 */
	public Text message() {
		return _message;
	}
	
	/**
	 * @return the {@link TreeViewer} control displaying the
	 *         {@link #getLogEntry()}'s changed paths
	 */
	public TreeViewer resources() {
		return _resources;
	}
	
	/**
	 * @return the {@link Text} control displaying the {@link #getLogEntry()}'s
	 *         revision number
	 */
	public Text revision() {
		return _revision;
	}
	
	/**
	 * @return the {@link Text} control displaying the {@link #getLogEntry()}'s
	 *         revision timestamp
	 */
	public Text timestamp() {
		return _timestamp;
	}
	
	/**
	 * Create {@link Control}s displaying base data for a {@link LogEntry} such as
	 * revision, author etc.
	 */
	private void createFields() {
		final Composite container = new Composite(this, SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		
		/* revision information */
		final Label labelRev = new Label(container, SWT.NONE);
		labelRev.setLayoutData(new GridData());
		labelRev.setText("Revision:");
		
		_revision = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		_revision.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		/* date information */
		final Label labelTimestamp = new Label(container, SWT.NONE);
		labelTimestamp.setLayoutData(new GridData());
		labelTimestamp.setText("Date:");
		
		_timestamp = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		_timestamp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		/* author information */
		final Label labelAuthor = new Label(container, SWT.NONE);
		labelAuthor.setLayoutData(new GridData());
		labelAuthor.setText("Author:");
		
		_author = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		_author.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		/* message information */
		final Label labelMsg = new Label(container, SWT.NONE);
		labelMsg.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		labelMsg.setText("Message:");
		
		_message = new Text(container, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		_message.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	/**
	 * Create a {@link TreeViewer} displaying the changed resources.
	 */
	private void createPaths() {
		final Composite container = new Composite(this, SWT.NONE);
		container.setLayout(new GridLayout());
		
		_resources = new TreeViewer(container);
		_resources.setContentProvider(new LogEntryPathsContentProvider());
		_resources.setLabelProvider(new LogEntryPathsLabelProvider());
		_resources.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		_resources.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	/**
	 * @return the {@link LogEntry} displayed by this {@link LogEntryView} instance
	 *         or {@code null} if it is empty
	 */
	public LogEntry getLogEntry() {
		return (LogEntry) getData();
	}
	
	/**
	 * Setter for {@link #getLogEntry()}.
	 * 
	 * @param entry see {@link #getLogEntry()}
	 */
	public void setLogEntry(final LogEntry entry) {
		setData(entry);
		
		updateFields();
		updatePaths();
	}
	
	/**
	 * Update {@link Control}s displaying base data for {@link #getLogEntry()} such
	 * as revision, author etc.
	 */
	protected void updateFields() {
		final StringBuilder revision = new StringBuilder();
		final StringBuilder author = new StringBuilder();
		final StringBuilder timestamp = new StringBuilder();
		final StringBuilder message = new StringBuilder();
		
		final LogEntry entry = getLogEntry();
		if (entry != null) {
			revision.append(entry.getRevision());
			author.append(entry.getAuthor());
			timestamp.append(entry.getDate());
			message.append(entry.getMessage());
		}
		
		_revision.setText(revision.toString());
		_author.setText(author.toString());
		_timestamp.setText(timestamp.toString());
		_message.setText(message.toString());
	}
	
	/**
	 * Update the {@link TreeViewer} displaying the changed resource of
	 * {@link #getLogEntry()}.
	 */
	protected void updatePaths() {
		_resources.setInput(getLogEntry());
	}
	
	/**
	 * A {@link LabelProvider} implementation for {@link TreeNode}s displaying
	 * changed resources.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	public static class LogEntryPathsLabelProvider extends LabelProvider {
		
		@Override
		public Image getImage(final Object element) {
			final String imageId = getImageId(element);
			
			// resolve the image for the calculated image identifier
			if(imageId != null) {
				return SVNUIPlugin.getImage(imageId);
			} else {
				return null;
			}
		}

		/**
		 * @param element
		 *            the {@link Object} to compute the image identifier for or
		 *            {@code null}
		 * @return the identifier {@link String} to be used for resolving the
		 *         {@link Image} for the given element or {@code null} indicating that
		 *         no {@link Image} is to be displayed
		 */
		protected String getImageId(final Object element) {
			final TreeNode node = (TreeNode) element;

			if (node.value() instanceof IPath) {
				return ISVNUIConstants.IMG_FOLDER;
			} else {
				final LogEntryPath path = (LogEntryPath) node.value();
				final boolean isFolder = path.getKind() == NodeKind.DIR;

				switch (path.getType()) {
				case ADDED:
					return isFolder ? ISVNUIConstants.IMG_FOLDERADD_PENDING
							: ISVNUIConstants.IMG_FILEADD_PENDING;
				case DELETED:
					return isFolder ? ISVNUIConstants.IMG_FOLDERDELETE_PENDING
							: ISVNUIConstants.IMG_FILEDELETE_PENDING;
				case MODIFIED:
				case REPLACED:
					return isFolder ? ISVNUIConstants.IMG_FOLDERMODIFIED_PENDING
							: ISVNUIConstants.IMG_FILEMODIFIED_PENDING;
				default:
					return null;
				}
			}
		}
		
		@Override
		public String getText(final Object element) {
			final TreeNode node = (TreeNode) element;
			final StringBuilder label = new StringBuilder(getLabel(node));
			
			// append copy path information (if applicable)
			if(node.value() instanceof LogEntryPath) {
				final LogEntryPath path = (LogEntryPath) node.value();
				
				if(path.getCopyPath() != null) {
					label
					.append(" [from ")
					.append(path.getCopyPath())
					.append(":")
					.append(path.getCopyRevision())
					.append("]");
				}
			}
			
			return label.toString();
		}
		
		/**
		 * @param node the {@link TreeNode} to return the label for
		 * @return the node's label
		 */
		private String getLabel(final TreeNode node) {
			final TreeNode parent = node.parent();
			
			// use absolute path for root nodes
			if(parent == null) {
				return getPath(node).toString();
			} else {
				final IPath nodePath = getPath(node);
				final IPath parentPath = getPath(parent);
				
				return nodePath.makeRelativeTo(parentPath).toString();
			}
		}
		
		/**
		 * @param node the {@link TreeNode} to return the path for
		 * @return the node's {@link IPath}
		 */
		private IPath getPath(final TreeNode node) {
			final Object value = node.value();
			final IPath path;
			
			if(value instanceof IPath) {
				path = (IPath) value;
			} else {
				path = Path.forPosix(((LogEntryPath) value).getPath());
			}
			
			return path;
		}
	}

	/**
	 * A {@link ITreeContentProvider} implementation for {@link LogEntry} instances.
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	public static class LogEntryPathsContentProvider implements ITreeContentProvider {

		/**
		 * A {@link Comparator} implementation for {@link TreeNode}s representing
		 * {@link LogEntryPath}s.
		 */
		private static final Comparator<Object> NODE_COMPARATOR = new Comparator<Object>() {
			
			@Override
			public int compare(final Object o1, final Object o2) {
				final TreeNode n1 = (TreeNode) o1;
				final TreeNode n2 = (TreeNode) o2;
				
				final int result = Boolean.compare(isFolder(n1), isFolder(n2));
				if(result != 0) {
					return -result;
				}
				
				final String l1;
				if(n1.value() instanceof IPath) {
					l1 = ((IPath) n1.value()).toString();
				} else {
					l1 = ((LogEntryPath) n1.value()).getPath();
				}
				
				final String l2;
				if(n2.value() instanceof IPath) {
					l2 = ((IPath) n2.value()).toString();
				} else {
					l2 = ((LogEntryPath) n2.value()).getPath();
				}
				
				return l1.compareTo(l2);
			}
			
			/**
			 * @param node the {@link TreeNode} to check
			 * @return {@code true} if the given {@link TreeNode} represents a folder
			 */
			private boolean isFolder(final TreeNode node) {
				// nodes with children are supposed to be folders
				if(node.hasChildren()) {
					return true;
				}
				
				// LogEntryPath nodes being marked with NodeKind.DIR are folders
				if(node.value() instanceof LogEntryPath) {
					return ((LogEntryPath)node.value()).getKind() == NodeKind.DIR;
				}
				
				// all others are considered files
				return false;
			}
		};

		@Override
		public Object[] getElements(final Object input) {
			if(input instanceof LogEntry) {
				final LogEntry entry = (LogEntry) input;
				
				final Map<IPath, TreeNode>  nodes = new HashMap<>();
				entry.getChangedPaths().forEach((key, value) -> {
					final IPath path = Path.forPosix(value.getPath());
					
					// a node for this path has already been created
					// before, so we only need to update its value
					// to display the actual LogEntryPath (changes)
					if(nodes.containsKey(path)) {
						nodes.get(path).value(value);
					} else {
						// create a new node for the LogEntryPath
						final TreeNode node = new TreeNode(value);
						nodes.put(path, node);
						
						// now create TreeNode instances for the path
						// to the structure root (if not done so yet)
						IPath childPath = path;
						TreeNode childNode = node;
						
						while(!childPath.isRoot()) {
							final IPath parentPath = childPath.removeLastSegments(1);
							
							if(nodes.containsKey(parentPath)) {
								nodes.get(parentPath).addChild(childNode);
								break;
							} else {
								final TreeNode parentNode = new TreeNode(parentPath);
								parentNode.addChild(childNode);
								nodes.put(parentPath, parentNode);
								
								childPath = parentPath;
								childNode = parentNode;
							}
						}
					}
				});
				
				// collapse artificial nodes with only one child
				for (final TreeNode root : nodes.values().stream().filter(node -> node.parent() == null).toArray(TreeNode[]::new)) {
					collapse(root, nodes);
				}
				
				// return root nodes
				return nodes.values().stream()
					.filter(node -> node.parent() == null)
					.sorted(NODE_COMPARATOR)
					.toArray();
			}
			
			return new TreeNode[0];
		}
		
		@Override
		public Object[] getChildren(final Object parent) {
			if(parent instanceof TreeNode) {
				return ((TreeNode) parent).children().stream()
					.sorted(NODE_COMPARATOR)
					.toArray();
			}
			
			return new TreeNode[0];
		}
		
		@Override
		public Object getParent(final Object child) {
			if(child instanceof TreeNode) {
				return ((TreeNode) child).parent();
			}
			
			return null;
		}
		
		@Override
		public boolean hasChildren(final Object element) {
			if(element instanceof TreeNode) {
				return ((TreeNode) element).hasChildren();
			}
			
			return false;
		}
		
		/**
		 * Recursively collapse the given {@link TreeNode} so that intermediate path
		 * nodes which are not part of the change set are compressed to one node for
		 * folders having only one child.
		 * 
		 * @param node  the {@link TreeNode} to collapse
		 * @param nodes a (possibly empty) {@link Map} of {@link TreeNode} mapped by
		 *              their {@link IPath} to be modified during recursion
		 */
		private void collapse(final TreeNode node, final Map<IPath, TreeNode> nodes) {
			final Set<TreeNode> children = node.children();
			
			// collapse artificial nodes with only one child
			if(node.value() instanceof IPath && children.size() == 1) {
				final TreeNode child = children.iterator().next();
				if(child.value() instanceof IPath) {
					node.removeChild(child); // disconnect child from node
					
					// collapse node by connecting parent and children directly
					final TreeNode parent = node.parent();
					if(parent != null) {
						parent.removeChild(node);
						parent.addChild(child);
					}
					
					// now remove the collapsed node from the tree
					nodes.remove(node.value());
				}
			}
			
			// recursive descent
			children.forEach(child -> collapse(child, nodes));
		}
	}
	
	/**
	 * Instances of this class represent mutable nodes of a tree.
	 * 
	 * <p>
	 * <b>Warning:</b> this implementation is not thread safe.
	 * </p>
	 * 
	 * @author <a href="mailto:wjatscheslaw.talanow@ascon-systems.de">Wjatscheslaw Talanow</a>
	 */
	public static class TreeNode {
		
		/**
		 * @see #children()
		 */
		private Set<TreeNode> _children;
		
		/**
		 * @see #parent()
		 */
		private TreeNode _parent;
		
		/**
		 * @see #value()
		 */
		private Object _value;
		
		/**
		 * Create a {@link TreeNode}.
		 * 
		 * @param value see {@link #value()}
		 */
		public TreeNode(final Object value) {
			_value = value;
		}
		
		/**
		 * @return the value {@link Object} represented by this {@link TreeNode} or
		 *         {@code null}
		 */
		public Object value() {
			return _value;
		}
		
		/**
		 * Setter for {@link #value()}.
		 * 
		 * @param value see {@link #value()}
		 */
		public void value(final Object value) {
			_value = value;
		}
		
		/**
		 * @return the parent {@link TreeNode} or {@code null}
		 */
		public TreeNode parent() {
			return _parent;
		}
		
		/**
		 * Setter for {@link #parent()}.
		 * 
		 * @param parent see {@link #parent()}
		 */
		private void parent(final TreeNode parent) {
			_parent = parent;
		}
		
		/**
		 * @return a (possibly empty) {@link Set} of child {@link TreeNode}s. Changes to
		 *         the returned set are not reflected in the receiver's structure.
		 */
		public Set<TreeNode> children() {
			if(_children == null) {
				return new LinkedHashSet<>();
			} else {
				return new LinkedHashSet<>(_children);
			}
		}
		
		/**
		 * Add the given node to the collection of the receiver's child nodes and change
		 * its parent accordingly.
		 * 
		 * <p>
		 * <b>Note:</b> has no effect if the given node is already one of the receiver's
		 * children.
		 * </p>
		 * 
		 * @param node the {@link TreeNode} to be added as child node
		 */
		public void addChild(final TreeNode node) {
			// lazy initialization: not thread-safe
			if (_children == null) {
				_children = new LinkedHashSet<>();
			}
			
			// register the new child and adjust its parent to be this instance
			if (_children.add(node)) {
				node.parent(this);
			}
		}
		
		/**
		 * Bulk method for adding multiple child nodes to this receiver.
		 * 
		 * @param nodes a (possibly empty) {@link Set} of nodes to be added as children
		 * @see #addChild(TreeNode)
		 */
		public void addChildren(final Set<TreeNode> nodes) {
			nodes.forEach(this::addChild);
		}
		
		/**
		 * Remove the given node from the collection of the receiver's child nodes and
		 * reset its parent.
		 * 
		 * <p>
		 * <b>Note:</b> has no effect if the given node was not one of the receiver's
		 * children.
		 * </p>
		 * 
		 * @param node the {@link TreeNode} to be removed
		 */
		public void removeChild(final TreeNode node) {
			if (_children != null && _children.remove(node)) {
				node.parent(null);
				
				// release the field when the last child was removed
				if (_children.isEmpty()) {
					_children = null;
				}
			}
		}
		
		/**
		 * Bulk method for removing multiple child nodes from this receiver.
		 * 
		 * @param nodes a (possibly empty) {@link Set} of nodes to be removed
		 * @see #removeChild(TreeNode)
		 */
		public void removeChildren(final Set<TreeNode> nodes) {
			nodes.forEach(this::removeChild);
		}
		
		/**
		 * @return {@code true} if this {@link TreeNode} has at least one child
		 *         {@link TreeNode}, {@code false} indicates that this node is a leaf
		 */
		public boolean hasChildren() {
			return _children != null;
		}
	}
}
