/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.model;

import org.eclipse.core.resources.IResourceDelta;

/**
 * The interface <code>DartElementDelta</code> describes changes in Dart elements between two
 * discrete points in time. Given a delta, clients can access the element that has changed, and any
 * children that have changed.
 * <p>
 * Deltas have a different status depending on the kind of change they represent. The list below
 * summarizes each status (as returned by {@link #getKind}) and its meaning (see individual
 * constants for a more detailed description):
 * <ul>
 * <li>{@link #ADDED} - The element described by the delta has been added.</li>
 * <li>{@link #REMOVED} - The element described by the delta has been removed.</li>
 * <li>{@link #CHANGED} - The element described by the delta has been changed in some way.
 * Specification of the type of change is provided by {@link #getFlags} which returns the following
 * values:
 * <ul>
 * <li>{@link #F_CHILDREN} - A child of the element has changed in some way.</li>
 * <li>{@link #F_CLOSED} - The underlying {@link org.eclipse.core.resources.IProject} has been
 * closed. This flag is only valid if the element is an {@link DartProject}.</li>
 * <li>{@link #F_CONTENT} - The contents of the element have been altered. This flag is only valid
 * for elements which correspond to files.</li>
 * <li>{@link #F_FINE_GRAINED} - The delta is a fine-grained delta, that is, an analysis down to the
 * members level was done to determine if there were structural changes to members of the element.</li>
 * <li>{@link #F_OPENED} - The underlying {@link org.eclipse.core.resources.IProject} has been
 * opened. This flag is only valid if the element is an {@link DartProject}.</li>
 * <li>{@link #F_SUPER_TYPES} - One of the supertypes of a {@link Type} has changed</li>.
 * </ul>
 * </li>
 * </ul>
 * </p>
 * <p>
 * Move operations are indicated by other change flags, layered on top of the change flags described
 * above. If element A is moved to become B, the delta for the change in A will have status
 * {@link #REMOVED}, with change flag {@link #F_MOVED_TO}. In this case, {@link #getMovedToElement}
 * on delta A will return the handle for B. The delta for B will have status {@link #ADDED}, with
 * change flag {@link #F_MOVED_FROM}, and {@link #getMovedFromElement} on delta B will return the
 * handle for A. (Note, the handle to A in this case represents an element that no longer exists).
 * </p>
 * <p>
 * Note that the move change flags only describe the changes to a single element, they do not imply
 * anything about the parent or children of the element.
 * </p>
 * <p>
 * No assumptions should be made on whether the java element delta tree is rooted at the
 * {@link IJavaModel} level or not.
 * </p>
 * <p>
 * {@link DartElementDelta} object are not valid outside the dynamic scope of the notification.
 * </p>
 */
public interface DartElementDelta {
  /**
   * Status constant indicating that the element has been added. Note that an added java element
   * delta has no children, as they are all implicitly added.
   */
  public int ADDED = 1;

  /**
   * Status constant indicating that the element has been removed. Note that a removed java element
   * delta has no children, as they are all implicitly removed.
   */
  public int REMOVED = 2;

  /**
   * Status constant indicating that the element has been changed, as described by the change flags.
   * 
   * @see #getFlags()
   */
  public int CHANGED = 4;

  /**
   * Change flag indicating that the content of the element has changed. This flag is only valid for
   * elements which correspond to files.
   */
  public int F_CONTENT = 0x000001;

  /**
   * Change flag indicating that the modifiers of the element have changed. This flag is only valid
   * if the element is an {@link IMember}.
   */
  public int F_MODIFIERS = 0x000002;

  /**
   * Change flag indicating that there are changes to the children of the element. This flag is only
   * valid if the element has children.
   */
  public int F_CHILDREN = 0x000008;

  /**
   * Change flag indicating that the element was moved from another location. The location of the
   * old element can be retrieved using {@link #getMovedFromElement}.
   */
  public int F_MOVED_FROM = 0x000010;

  /**
   * Change flag indicating that the element was moved to another location. The location of the new
   * element can be retrieved using {@link #getMovedToElement}.
   */
  public int F_MOVED_TO = 0x000020;

  /**
   * Change flag indicating that a classpath entry corresponding to the element has been added to
   * the project's classpath. This flag is only valid if the element is an
   * {@link IPackageFragmentRoot}.
   */
  // public int F_ADDED_TO_CLASSPATH = 0x000040;

  /**
   * Change flag indicating that a classpath entry corresponding to the element has been removed
   * from the project's classpath. This flag is only valid if the element is an
   * {@link IPackageFragmentRoot}.
   */
  // public int F_REMOVED_FROM_CLASSPATH = 0x000080;

  /**
   * Change flag indicating that the element has changed position relatively to its siblings.
   */
  public int F_REORDER = 0x000100;

  /**
   * Change flag indicating that the underlying {@link org.eclipse.core.resources.IProject} has been
   * opened. This flag is only valid if the element is a {@link DartProject}.
   */
  public int F_OPENED = 0x000200;

  /**
   * Change flag indicating that the underlying {@link org.eclipse.core.resources.IProject} has been
   * closed. This flag is only valid if the element is a {@link DartProject}.
   */
  public int F_CLOSED = 0x000400;

  /**
   * Change flag indicating that one of the supertypes of a {@link Type} has changed.
   */
  public int F_SUPER_TYPES = 0x000800;

  /**
   * Change flag indicating that the source attachment path or the source attachment root path of a
   * classpath entry corresponding to the element was added. This flag is only valid if the element
   * is an {@link IPackageFragmentRoot}.
   */
  // public int F_SOURCEATTACHED = 0x001000;

  /**
   * Change flag indicating that the source attachment path or the source attachment root path of a
   * classpath entry corresponding to the element was removed. This flag is only valid if the
   * element is an {@link IPackageFragmentRoot}.
   */
  // public int F_SOURCEDETACHED = 0x002000;

  /**
   * Change flag indicating that this is a fine-grained delta, that is, an analysis down to the
   * members level was done to determine if there were structural changes to members.
   * <p>
   * Clients can use this flag to find out if a compilation unit that has a {@link #F_CONTENT}
   * change should assume that there are no finer grained changes ({@link #F_FINE_GRAINED} is set)
   * or if finer grained changes were not considered ({@link #F_FINE_GRAINED} is not set).
   */
  public int F_FINE_GRAINED = 0x004000;

  /**
   * Change flag indicating that the element's archive content on the classpath has changed. This
   * flag is only valid if the element is an {@link IPackageFragmentRoot} which is an archive.
   * 
   * @see IPackageFragmentRoot#isArchive()
   */
  // public int F_ARCHIVE_CONTENT_CHANGED = 0x008000;

  /**
   * Change flag indicating that a compilation unit has become a primary working copy, or that a
   * primary working copy has reverted to a compilation unit. This flag is only valid if the element
   * is an {@link CompilationUnit}.
   */
  public int F_PRIMARY_WORKING_COPY = 0x010000;

  /**
   * Change flag indicating that the {@link IJavaProject#getRawClasspath() raw classpath} (or the
   * {@link IJavaProject#getOutputLocation() output folder}) of a project has changed. This flag is
   * only valid if the element is an {@link IJavaProject}. Also see
   * {@link #F_RESOLVED_CLASSPATH_CHANGED}, which indicates that there is a change to the
   * {@link IJavaProject#getResolvedClasspath(boolean) resolved class path}. The resolved classpath
   * can change without the raw classpath changing (e.g. if a container resolves to a different set
   * of classpath entries). And conversely, it is possible to construct a case where the raw
   * classpath can change without the resolved classpath changing.
   */
  // public int F_CLASSPATH_CHANGED = 0x020000;

  /**
   * Change flag indicating that the resource of a primary compilation unit has changed. This flag
   * is only valid if the element is a primary {@link CompilationUnit}.
   */
  public int F_PRIMARY_RESOURCE = 0x040000;

  /**
   * Change flag indicating that the top-level property of a library has changed. This flag is only
   * valid if the element is a {@link DartLibrary}.
   */
  public int F_TOP_LEVEL = 0x080000;

  /**
   * Change flag indicating that a reconcile operation has affected the compilation unit AST created
   * in a previous reconcile operation. Use {@link #getCompilationUnitAST()} to retrieve the AST (if
   * any is available). This flag is only valid if the element is an {@link ICompilationUnit} in
   * working copy mode.
   */
  public int F_AST_AFFECTED = 0x080000;

  /**
   * Return deltas for the children that have been added.
   * 
   * @return deltas for the children that have been added
   */
  public DartElementDelta[] getAddedChildren();

  /**
   * Return deltas for the affected (added, removed, or changed) children.
   * 
   * @return deltas for the affected (added, removed, or changed) children
   */
  public DartElementDelta[] getAffectedChildren();

  /**
   * Return deltas for the children which have changed.
   * 
   * @return deltas for the children which have changed
   */
  public DartElementDelta[] getChangedChildren();

  /**
   * Return the compilation unit AST created by the last reconcile operation on this delta's
   * element. This returns a non-null value if and only if:
   * <ul>
   * <li>the last reconcile operation on this working copy requested an AST</li>
   * <li>this delta's element is an {@link ICompilationUnit} in working copy mode</li>
   * <li>the delta comes from a {@link ElementChangedEvent#POST_RECONCILE} event
   * </ul>
   * 
   * @return the AST created during the last reconcile operation
   * @see ICompilationUnit#reconcile(int, boolean, WorkingCopyOwner,
   *      org.eclipse.core.runtime.IProgressMonitor)
   * @see #F_AST_AFFECTED
   */
//  public CompilationUnit getCompilationUnitAST();

  /**
   * Return the element that this delta describes a change to.
   * 
   * @return the element that this delta describes a change to
   */
  public DartElement getElement();

  /**
   * Return flags that describe how an element has changed. Such flags should be tested using the
   * <code>&</code> operand. For example:
   * 
   * <pre>
   * if ((delta.getFlags() &amp; IJavaElementDelta.F_CONTENT) != 0) {
   *   // the delta indicates a content change
   * }
   * </pre>
   * 
   * @return flags that describe how an element has changed
   */
  public int getFlags();

  /**
   * Return the kind of this delta - one of {@link #ADDED}, {@link #REMOVED}, or {@link #CHANGED}.
   * 
   * @return the kind of this delta
   */
  public int getKind();

  /**
   * Return an element describing this element before it was moved to its current location, or
   * <code>null</code> if the {@link #F_MOVED_FROM} change flag is not set.
   * 
   * @return an element describing this element before it was moved to its current location
   */
  public DartElement getMovedFromElement();

  /**
   * Return an element describing this element in its new location, or <code>null</code> if the
   * {@link #F_MOVED_TO} change flag is not set.
   * 
   * @return an element describing this element in its new location the {@link #F_MOVED_TO} change
   *         flag is not set
   */
  public DartElement getMovedToElement();

  /**
   * Return deltas for the children which have been removed.
   * 
   * @return deltas for the children which have been removed
   */
  public DartElementDelta[] getRemovedChildren();

  /**
   * Return the collection of resource deltas.
   * <p>
   * Note that resource deltas, like Java element deltas, are generally only valid for the dynamic
   * scope of an event notification. Clients must not hang on to these objects.
   * </p>
   * 
   * @return the underlying resource deltas, or <code>null</code> if none
   */
  public IResourceDelta[] getResourceDeltas();
}
