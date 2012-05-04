/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.model.delta;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;

import org.eclipse.core.resources.IResourceDelta;

import java.util.ArrayList;

/**
 * Instances of the class <code>DartElementDeltaImpl</code>
 */
public class DartElementDeltaImpl extends SimpleDelta implements DartElementDelta {
  /**
   * @see #getAffectedChildren()
   */
  private DartElementDelta[] affectedChildren = EMPTY_DELTA;

  /**
   * The AST created during the last reconcile operation. Non-null only iff: - in a POST_RECONCILE
   * event - an AST was requested during the last reconcile operation - the changed element is a
   * CompilationUnit in working copy mode
   */
  private DartUnit ast = null;

  /*
   * The element that this delta describes the change to.
   */
  private DartElement changedElement;

  /**
   * Collection of resource deltas that correspond to non dart resources deltas.
   */
  private IResourceDelta[] resourceDeltas = null;

  /**
   * Counter of resource deltas
   */
  private int resourceDeltasCounter;

  /**
   * @see #getMovedFromElement()
   */
  private DartElement movedFromHandle = null;

  /**
   * @see #getMovedToElement()
   */
  private DartElement movedToHandle = null;

  private DartElementDelta[] annotationDeltas = EMPTY_DELTA;

  /**
   * Empty array of DartElementDelta
   */
  private static DartElementDelta[] EMPTY_DELTA = new DartElementDelta[0];

  /**
   * Create the root delta. To create the nested delta hierarchies use the following convenience
   * methods. The root delta can be created at any level (for example: project, package root,
   * package fragment...).
   * <ul>
   * <li><code>added(DartElement)</code>
   * <li><code>changed(DartElement)</code>
   * <li><code>moved(DartElement, DartElement)</code>
   * <li><code>removed(DartElement)</code>
   * <li><code>renamed(DartElement, DartElement)</code>
   * </ul>
   */
  public DartElementDeltaImpl(DartElement element) {
    changedElement = element;
  }

  /**
   * Create the nested deltas resulting from an add operation. Convenience method for creating add
   * deltas. The constructor should be used to create the root delta and then an add operation
   * should call this method.
   */
  public void added(DartElement element) {
    added(element, 0);
  }

  public void added(DartElement element, int flags) {
    DartElementDeltaImpl addedDelta = new DartElementDeltaImpl(element);
    addedDelta.added();
    addedDelta.changeFlags |= flags;
    insertDeltaTree(element, addedDelta);
  }

  /**
   * Create the nested deltas resulting from a change operation. Convenience method for creating
   * change deltas. The constructor should be used to create the root delta and then a change
   * operation should call this method.
   */
  public DartElementDeltaImpl changed(DartElement element, int changeFlag) {
    DartElementDeltaImpl changedDelta = new DartElementDeltaImpl(element);
    changedDelta.changed(changeFlag);
    insertDeltaTree(element, changedDelta);
    return changedDelta;
  }

  /**
   * Records the last changed AST.
   */
  public void changedAST(DartUnit changedAST) {
    ast = changedAST;
    changed(F_AST_AFFECTED);
  }

  /**
   * Create the nested deltas for a closed element.
   */
  public void closed(DartElement element) {
    DartElementDeltaImpl delta = new DartElementDeltaImpl(element);
    delta.changed(F_CLOSED);
    insertDeltaTree(element, delta);
  }

  /**
   * Mark this delta as a content changed delta.
   */
  public void contentChanged() {
    changeFlags |= F_CONTENT;
  }

  /**
   * Mark this delta as a fine-grained delta.
   */
  public void fineGrained() {
    changed(F_FINE_GRAINED);
  }

  @Override
  public DartElementDelta[] getAddedChildren() {
    return getChildrenOfType(ADDED);
  }

  @Override
  public DartElementDelta[] getAffectedChildren() {
    return affectedChildren;
  }

  public DartElementDelta[] getAnnotationDeltas() {
    return annotationDeltas;
  }

  @Override
  public DartElementDelta[] getChangedChildren() {
    return getChildrenOfType(CHANGED);
  }

  public DartUnit getCompilationUnitAST() {
    return ast;
  }

  @Override
  public DartElement getElement() {
    return changedElement;
  }

  @Override
  public DartElement getMovedFromElement() {
    return movedFromHandle;
  }

  @Override
  public DartElement getMovedToElement() {
    return movedToHandle;
  }

  @Override
  public DartElementDelta[] getRemovedChildren() {
    return getChildrenOfType(REMOVED);
  }

  @Override
  public IResourceDelta[] getResourceDeltas() {
    if (resourceDeltas == null) {
      return null;
    }
    if (resourceDeltas.length != resourceDeltasCounter) {
      System.arraycopy(
          resourceDeltas,
          0,
          resourceDeltas = new IResourceDelta[resourceDeltasCounter],
          0,
          resourceDeltasCounter);
    }
    return resourceDeltas;
  }

  /**
   * Create the delta tree for the given element and delta, and then inserts the tree as an affected
   * child of this node.
   */
  public void insertDeltaTree(DartElement element, DartElementDeltaImpl delta) {
    DartElementDeltaImpl childDelta = createDeltaTree(element, delta);
    if (!equalsAndSameParent(element, getElement())) {
      // handle case of two jars that can be equals but not in the same project
      addAffectedChild(childDelta);
    }
  }

  /**
   * Create the nested deltas resulting from an move operation. Convenience method for creating the
   * "move from" delta. The constructor should be used to create the root delta and then the move
   * operation should call this method.
   */
  public void movedFrom(DartElement movedFromElement, DartElement movedToElement) {
    DartElementDeltaImpl removedDelta = new DartElementDeltaImpl(movedFromElement);
    removedDelta.kind = REMOVED;
    removedDelta.changeFlags |= F_MOVED_TO;
    removedDelta.movedToHandle = movedToElement;
    insertDeltaTree(movedFromElement, removedDelta);
  }

  /**
   * Create the nested deltas resulting from an move operation. Convenience method for creating the
   * "move to" delta. The constructor should be used to create the root delta and then the move
   * operation should call this method.
   */
  public void movedTo(DartElement movedToElement, DartElement movedFromElement) {
    DartElementDeltaImpl addedDelta = new DartElementDeltaImpl(movedToElement);
    addedDelta.kind = ADDED;
    addedDelta.changeFlags |= F_MOVED_FROM;
    addedDelta.movedFromHandle = movedFromElement;
    insertDeltaTree(movedToElement, addedDelta);
  }

  /**
   * Create the nested deltas for an opened element.
   */
  public void opened(DartElement element) {
    DartElementDeltaImpl delta = new DartElementDeltaImpl(element);
    delta.changed(F_OPENED);
    insertDeltaTree(element, delta);
  }

  /**
   * Create the nested deltas resulting from an delete operation. Convenience method for creating
   * removed deltas. The constructor should be used to create the root delta and then the delete
   * operation should call this method.
   */
  public void removed(DartElement element) {
    removed(element, 0);
  }

  public void removed(DartElement element, int flags) {
    DartElementDeltaImpl removedDelta = new DartElementDeltaImpl(element);
    insertDeltaTree(element, removedDelta);
    DartElementDeltaImpl actualDelta = getDeltaFor(element);
    if (actualDelta != null) {
      actualDelta.removed();
      actualDelta.changeFlags |= flags;
      actualDelta.affectedChildren = EMPTY_DELTA;
    }
  }

  /**
   * Create the nested deltas resulting from a change operation. Convenience method for creating
   * change deltas. The constructor should be used to create the root delta and then a change
   * operation should call this method.
   */
  public void sourceAttached(DartElement element) {
    DartCore.notYetImplemented();
    // DartElementDeltaImpl attachedDelta = new DartElementDeltaImpl(element);
    // attachedDelta.changed(F_SOURCEATTACHED);
    // insertDeltaTree(element, attachedDelta);
  }

  /**
   * Create the nested deltas resulting from a change operation. Convenience method for creating
   * change deltas. The constructor should be used to create the root delta and then a change
   * operation should call this method.
   */
  public void sourceDetached(DartElement element) {
    DartCore.notYetImplemented();
    // DartElementDeltaImpl detachedDelta = new DartElementDeltaImpl(element);
    // detachedDelta.changed(F_SOURCEDETACHED);
    // insertDeltaTree(element, detachedDelta);
  }

  /**
   * Return a string representation of this delta's structure suitable for debug purposes.
   * 
   * @see #toString()
   */
  public String toDebugString(int depth) {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < depth; i++) {
      buffer.append('\t');
    }
    buffer.append(((DartElementImpl) getElement()).toString()); // toDebugString());
    toDebugString(buffer);
    DartElementDelta[] children = getAffectedChildren();
    if (children != null) {
      for (int i = 0; i < children.length; ++i) {
        buffer.append("\n"); //$NON-NLS-1$
        buffer.append(((DartElementDeltaImpl) children[i]).toDebugString(depth + 1));
      }
    }
    for (int i = 0; i < resourceDeltasCounter; i++) {
      buffer.append("\n");//$NON-NLS-1$
      for (int j = 0; j < depth + 1; j++) {
        buffer.append('\t');
      }
      IResourceDelta resourceDelta = resourceDeltas[i];
      buffer.append(resourceDelta.toString());
      buffer.append("["); //$NON-NLS-1$
      switch (resourceDelta.getKind()) {
        case IResourceDelta.ADDED:
          buffer.append('+');
          break;
        case IResourceDelta.REMOVED:
          buffer.append('-');
          break;
        case IResourceDelta.CHANGED:
          buffer.append('*');
          break;
        default:
          buffer.append('?');
          break;
      }
      buffer.append("]"); //$NON-NLS-1$
    }
    DartElementDelta[] annotations = getAnnotationDeltas();
    if (annotations != null) {
      for (int i = 0; i < annotations.length; ++i) {
        buffer.append("\n"); //$NON-NLS-1$
        buffer.append(((DartElementDeltaImpl) annotations[i]).toDebugString(depth + 1));
      }
    }
    return buffer.toString();
  }

  /**
   * Return a string representation of this delta's structure suitable for debug purposes.
   */
  @Override
  public String toString() {
    return toDebugString(0);
  }

  /**
   * Add the child delta to the collection of affected children. If the child is already in the
   * collection, walk down the hierarchy.
   */
  protected void addAffectedChild(DartElementDeltaImpl child) {
    switch (kind) {
      case ADDED:
      case REMOVED:
        // no need to add a child if this parent is added or removed
        return;
      case CHANGED:
        changeFlags |= F_CHILDREN;
        break;
      default:
        kind = CHANGED;
        changeFlags |= F_CHILDREN;
    }

    // if a child delta is added to a compilation unit delta or below,
    // it's a fine grained delta
    if (changedElement.getElementType() >= DartElement.COMPILATION_UNIT) {
      fineGrained();
    }

    if (affectedChildren == null || affectedChildren.length == 0) {
      affectedChildren = new DartElementDelta[] {child};
      return;
    }
    DartElementDeltaImpl existingChild = null;
    int existingChildIndex = -1;
    for (int i = 0; i < affectedChildren.length; i++) {
      if (equalsAndSameParent(affectedChildren[i].getElement(), child.getElement())) {
        // handle case of two jars that can be equals but not in the same project
        existingChild = (DartElementDeltaImpl) affectedChildren[i];
        existingChildIndex = i;
        break;
      }
    }
    if (existingChild == null) {
      // new affected child
      affectedChildren = growAndAddToArray(affectedChildren, child);
    } else {
      switch (existingChild.getKind()) {
        case ADDED:
          switch (child.getKind()) {
            case ADDED: // child was added then added -> it is added
            case CHANGED: // child was added then changed -> it is added
              return;
            case REMOVED: // child was added then removed -> noop
              affectedChildren = removeAndShrinkArray(affectedChildren, existingChildIndex);
              return;
          }
          break;
        case REMOVED:
          switch (child.getKind()) {
            case ADDED: // child was removed then added -> it is changed
              child.kind = CHANGED;
              affectedChildren[existingChildIndex] = child;
              return;
            case CHANGED: // child was removed then changed -> it is removed
            case REMOVED: // child was removed then removed -> it is removed
              return;
          }
          break;
        case CHANGED:
          switch (child.getKind()) {
            case ADDED: // child was changed then added -> it is added
            case REMOVED: // child was changed then removed -> it is removed
              affectedChildren[existingChildIndex] = child;
              return;
            case CHANGED: // child was changed then changed -> it is changed
              DartElementDelta[] children = child.getAffectedChildren();
              for (int i = 0; i < children.length; i++) {
                DartElementDeltaImpl childsChild = (DartElementDeltaImpl) children[i];
                existingChild.addAffectedChild(childsChild);
              }

              // update flags
              boolean childHadContentFlag = (child.changeFlags & F_CONTENT) != 0;
              boolean existingChildHadChildrenFlag = (existingChild.changeFlags & F_CHILDREN) != 0;
              existingChild.changeFlags |= child.changeFlags;

              // remove F_CONTENT flag if existing child had F_CHILDREN flag set
              // (case of fine grained delta (existing child) and delta coming
              // from DeltaProcessor (child))
              if (childHadContentFlag && existingChildHadChildrenFlag) {
                existingChild.changeFlags &= ~F_CONTENT;
              }

              // add the non-java resource deltas if needed note that the child
              // delta always takes precedence over this existing child delta
              // as non-java resource deltas are always created last (by the
              // DeltaProcessor)
              IResourceDelta[] resDeltas = child.getResourceDeltas();
              if (resDeltas != null) {
                existingChild.resourceDeltas = resDeltas;
                existingChild.resourceDeltasCounter = child.resourceDeltasCounter;
              }
              return;
          }
          break;
        default:
          // unknown -> existing child becomes the child with the existing
          // child's flags
          int flags = existingChild.getFlags();
          affectedChildren[existingChildIndex] = child;
          child.changeFlags |= flags;
      }
    }
  }

  /**
   * Add the child delta to the collection of affected children. If the child is already in the
   * collection, walk down the hierarchy.
   */
  protected void addResourceDelta(IResourceDelta child) {
    switch (kind) {
      case ADDED:
      case REMOVED:
        // no need to add a child if this parent is added or removed
        return;
      case CHANGED:
        changeFlags |= F_CONTENT;
        break;
      default:
        kind = CHANGED;
        changeFlags |= F_CONTENT;
    }
    if (resourceDeltas == null) {
      resourceDeltas = new IResourceDelta[5];
      resourceDeltas[resourceDeltasCounter++] = child;
      return;
    }
    if (resourceDeltas.length == resourceDeltasCounter) {
      // need a resize
      System.arraycopy(
          resourceDeltas,
          0,
          (resourceDeltas = new IResourceDelta[resourceDeltasCounter * 2]),
          0,
          resourceDeltasCounter);
    }
    resourceDeltas[resourceDeltasCounter++] = child;
  }

  /**
   * Create the nested delta deltas based on the affected element its delta, and the root of this
   * delta tree. Returns the root of the created delta tree.
   */
  protected DartElementDeltaImpl createDeltaTree(DartElement element, DartElementDeltaImpl delta) {
    DartElementDeltaImpl childDelta = delta;
    ArrayList<DartElement> ancestors = getAncestors(element);
    if (ancestors == null) {
      if (equalsAndSameParent(delta.getElement(), getElement())) {
        // handle case of two jars that can be equals but not in the same project
        // the element being changed is the root element
        kind = delta.kind;
        changeFlags = delta.changeFlags;
        movedToHandle = delta.movedToHandle;
        movedFromHandle = delta.movedFromHandle;
      }
    } else {
      for (int i = 0, size = ancestors.size(); i < size; i++) {
        DartElement ancestor = ancestors.get(i);
        DartElementDeltaImpl ancestorDelta = new DartElementDeltaImpl(ancestor);
        ancestorDelta.addAffectedChild(childDelta);
        childDelta = ancestorDelta;
      }
    }
    return childDelta;
  }

  /**
   * Return whether the two java elements are equals and have the same parent.
   */
  protected boolean equalsAndSameParent(DartElement e1, DartElement e2) {
    DartElement parent1;
    return e1.equals(e2) && ((parent1 = e1.getParent()) != null) && parent1.equals(e2.getParent());
  }

  /**
   * Return the <code>DartElementDeltaImpl</code> for the given element in the delta tree, or null,
   * if no delta for the given element is found.
   */
  protected DartElementDeltaImpl find(DartElement e) {
    if (equalsAndSameParent(changedElement, e)) {
      // handle case of two jars that can be equals but not in the same project
      return this;
    } else {
      for (int i = 0; i < affectedChildren.length; i++) {
        DartElementDeltaImpl delta = ((DartElementDeltaImpl) affectedChildren[i]).find(e);
        if (delta != null) {
          return delta;
        }
      }
    }
    return null;
  }

  protected DartElementDelta[] getChildrenOfType(int type) {
    int length = affectedChildren.length;
    if (length == 0) {
      return new DartElementDelta[] {};
    }
    ArrayList<DartElementDelta> children = new ArrayList<DartElementDelta>(length);
    for (int i = 0; i < length; i++) {
      if (affectedChildren[i].getKind() == type) {
        children.add(affectedChildren[i]);
      }
    }

    DartElementDelta[] childrenOfType = new DartElementDelta[children.size()];
    children.toArray(childrenOfType);

    return childrenOfType;
  }

  /**
   * Return the delta for a given element. Only looks below this delta.
   */
  protected DartElementDeltaImpl getDeltaFor(DartElement element) {
    if (equalsAndSameParent(getElement(), element)) {
      // handle case of two jars that can be equals but not in the same project
      return this;
    }
    if (affectedChildren.length == 0) {
      return null;
    }
    int childrenCount = affectedChildren.length;
    for (int i = 0; i < childrenCount; i++) {
      DartElementDeltaImpl delta = (DartElementDeltaImpl) affectedChildren[i];
      if (equalsAndSameParent(delta.getElement(), element)) {
        // handle case of two jars that can be equals but not in the same project
        return delta;
      } else {
        delta = delta.getDeltaFor(element);
        if (delta != null) {
          return delta;
        }
      }
    }
    return null;
  }

  /**
   * Add the new element to a new array that contains all of the elements of the old array. Returns
   * the new array.
   */
  protected DartElementDelta[] growAndAddToArray(DartElementDelta[] array, DartElementDelta addition) {
    DartElementDelta[] old = array;
    array = new DartElementDelta[old.length + 1];
    System.arraycopy(old, 0, array, 0, old.length);
    array[old.length] = addition;
    return array;
  }

  /**
   * Remove the child delta from the collection of affected children.
   */
  protected void removeAffectedChild(DartElementDeltaImpl child) {
    int index = -1;
    if (affectedChildren != null) {
      for (int i = 0; i < affectedChildren.length; i++) {
        if (equalsAndSameParent(affectedChildren[i].getElement(), child.getElement())) {
          // handle case of two jars that can be equals but not in the same project
          index = i;
          break;
        }
      }
    }
    if (index >= 0) {
      affectedChildren = removeAndShrinkArray(affectedChildren, index);
    }
  }

  /**
   * Remove the element from the array. Returns the a new array which has shrunk.
   */
  protected DartElementDelta[] removeAndShrinkArray(DartElementDelta[] old, int index) {
    DartElementDelta[] array = new DartElementDelta[old.length - 1];
    if (index > 0) {
      System.arraycopy(old, 0, array, 0, index);
    }
    int rest = old.length - index - 1;
    if (rest > 0) {
      System.arraycopy(old, index + 1, array, index, rest);
    }
    return array;
  }

  @Override
  protected boolean toDebugString(StringBuilder buffer, int flags) {
    boolean prev = super.toDebugString(buffer, flags);

    if ((flags & DartElementDelta.F_CHILDREN) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("CHILDREN"); //$NON-NLS-1$
      prev = true;
    }
    if ((flags & DartElementDelta.F_CONTENT) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("CONTENT"); //$NON-NLS-1$
      prev = true;
    }
    if ((flags & DartElementDelta.F_MOVED_FROM) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("MOVED_FROM(" + ((DartElementImpl) getMovedFromElement()).toStringWithAncestors() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
      prev = true;
    }
    if ((flags & DartElementDelta.F_MOVED_TO) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("MOVED_TO(" + ((DartElementImpl) getMovedToElement()).toStringWithAncestors() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
      prev = true;
    }
    // if ((flags & DartElementDelta.F_ADDED_TO_CLASSPATH) != 0) {
    // if (prev)
    //      buffer.append(" | "); //$NON-NLS-1$
    //    buffer.append("ADDED TO CLASSPATH"); //$NON-NLS-1$
    // prev = true;
    // }
    // if ((flags & DartElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) {
    // if (prev)
    //      buffer.append(" | "); //$NON-NLS-1$
    //    buffer.append("REMOVED FROM CLASSPATH"); //$NON-NLS-1$
    // prev = true;
    // }
    if ((flags & DartElementDelta.F_REORDER) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("REORDERED"); //$NON-NLS-1$
      prev = true;
    }
    // if ((flags & DartElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0) {
    // if (prev)
    //      buffer.append(" | "); //$NON-NLS-1$
    //    buffer.append("ARCHIVE CONTENT CHANGED"); //$NON-NLS-1$
    // prev = true;
    // }
    // if ((flags & DartElementDelta.F_SOURCEATTACHED) != 0) {
    // if (prev)
    //      buffer.append(" | "); //$NON-NLS-1$
    //    buffer.append("SOURCE ATTACHED"); //$NON-NLS-1$
    // prev = true;
    // }
    // if ((flags & DartElementDelta.F_SOURCEDETACHED) != 0) {
    // if (prev)
    //      buffer.append(" | "); //$NON-NLS-1$
    //    buffer.append("SOURCE DETACHED"); //$NON-NLS-1$
    // prev = true;
    // }
    if ((flags & DartElementDelta.F_FINE_GRAINED) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("FINE GRAINED"); //$NON-NLS-1$
      prev = true;
    }
    if ((flags & DartElementDelta.F_PRIMARY_WORKING_COPY) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("PRIMARY WORKING COPY"); //$NON-NLS-1$
      prev = true;
    }
    // if ((flags & DartElementDelta.F_CLASSPATH_CHANGED) != 0) {
    // if (prev)
    //      buffer.append(" | "); //$NON-NLS-1$
    //    buffer.append("RAW CLASSPATH CHANGED"); //$NON-NLS-1$
    // prev = true;
    // }
    // if ((flags & DartElementDelta.F_RESOLVED_CLASSPATH_CHANGED) != 0) {
    // if (prev)
    //      buffer.append(" | "); //$NON-NLS-1$
    //    buffer.append("RESOLVED CLASSPATH CHANGED"); //$NON-NLS-1$
    // prev = true;
    // }
    if ((flags & DartElementDelta.F_PRIMARY_RESOURCE) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("PRIMARY RESOURCE"); //$NON-NLS-1$
      prev = true;
    }
    if ((flags & DartElementDelta.F_OPENED) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("OPENED"); //$NON-NLS-1$
      prev = true;
    }
    if ((flags & DartElementDelta.F_CLOSED) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("CLOSED"); //$NON-NLS-1$
      prev = true;
    }
    if ((flags & DartElementDelta.F_AST_AFFECTED) != 0) {
      if (prev) {
        buffer.append(" | "); //$NON-NLS-1$
      }
      buffer.append("AST AFFECTED"); //$NON-NLS-1$
      prev = true;
    }
    // if ((flags & DartElementDelta.F_CATEGORIES) != 0) {
    // if (prev)
    //      buffer.append(" | "); //$NON-NLS-1$
    //    buffer.append("CATEGORIES"); //$NON-NLS-1$
    // prev = true;
    // }
    // if ((flags & DartElementDelta.F_ANNOTATIONS) != 0) {
    // if (prev)
    //      buffer.append(" | "); //$NON-NLS-1$
    //    buffer.append("ANNOTATIONS"); //$NON-NLS-1$
    // prev = true;
    // }
    return prev;
  }

  /**
   * Return a collection of all the parents of this element up to (but not including) the root of
   * this tree in bottom-up order. If the given element is not a descendant of the root of this
   * tree, <code>null</code> is returned.
   */
  private ArrayList<DartElement> getAncestors(DartElement element) {
    DartElement parent = element.getParent();
    if (parent == null) {
      return null;
    }
    ArrayList<DartElement> parents = new ArrayList<DartElement>();
    while (!parent.equals(changedElement)) {
      parents.add(parent);
      parent = parent.getParent();
      if (parent == null) {
        return null;
      }
    }
    parents.trimToSize();
    return parents;
  }
}
