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

import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.internal.model.DartModelImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartFieldInfo;
import com.google.dart.tools.core.internal.model.info.DartMethodInfo;
import com.google.dart.tools.core.internal.model.info.DartTypeInfo;
import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.ParentElement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Instances of the class <code>DartElementDeltaBuilder</code> create a Dart element delta on a Dart
 * element between the version of the Dart element at the time the comparator was created and the
 * current version of the Dart element.
 * <p>
 * It performs this operation by locally caching the contents of the Dart element when it is
 * created. When the method createDeltas() is called, it creates a delta over the cached contents
 * and the new contents.
 */
public class DartElementDeltaBuilder {
  /**
   * Doubly linked list item
   */
  static class ListItem {
    public DartElement previous;
    public DartElement next;

    public ListItem(DartElement previous, DartElement next) {
      this.previous = previous;
      this.next = next;
    }
  }

  /**
   * The Dart element handle.
   */
  private DartElement dartElement;

  /**
   * The maximum depth in the Dart element children we should look into
   */
  private int maxDepth = Integer.MAX_VALUE;

  /**
   * The old handle to info relationships
   */
  private Map<DartElement, DartElementInfo> infos;

  // Map annotationInfos;

  /**
   * The old position info
   */
  private Map<DartElement, ListItem> oldPositions;

  /**
   * The new position info
   */
  private Map<DartElement, ListItem> newPositions;

  /**
   * Change delta
   */
  public DartElementDeltaImpl delta = null;

  /**
   * List of added elements
   */
  private HashSet<DartElement> added;

  /**
   * List of removed elements
   */
  private HashSet<DartElement> removed;

  /**
   * Creates a Dart element comparator on a Dart element looking as deep as necessary.
   */
  public DartElementDeltaBuilder(DartElement dartElement) {
    this.dartElement = dartElement;
    initialize();
    recordElementInfo(dartElement, (DartModelImpl) dartElement.getDartModel(), 0);
  }

  /**
   * Creates a Dart element comparator on a Dart element looking only 'maxDepth' levels deep.
   */
  public DartElementDeltaBuilder(DartElement dartElement, int maxDepth) {
    this.dartElement = dartElement;
    this.maxDepth = maxDepth;
    initialize();
    recordElementInfo(dartElement, (DartModelImpl) dartElement.getDartModel(), 0);
  }

  /**
   * Builds the Dart element deltas between the old content of the compilation unit and its new
   * content.
   */
  public void buildDeltas() {
    delta = new DartElementDeltaImpl(dartElement);
    // if building a delta on a compilation unit or below,
    // it's a fine grained delta
    if (dartElement instanceof CompilationUnit) {
      delta.fineGrained();
    }
    recordNewPositions(dartElement, 0);
    findAdditions(dartElement, 0);
    findDeletions();
    findChangesInPositioning(dartElement, 0);
    trimDelta(delta);
    if (delta.getAffectedChildren().length == 0) {
      // this is a fine grained but not children affected -> mark as content
      // changed
      delta.contentChanged();
    }
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Built delta:\n"); //$NON-NLS-1$
    buffer.append(delta == null ? "<null>" : delta.toString()); //$NON-NLS-1$
    return buffer.toString();
  }

  /**
   * Repairs the positioning information after an element has been added
   */
  private void added(DartElement element) {
    added.add(element);
    ListItem current = getNewPosition(element);
    ListItem previous = null, next = null;
    if (current.previous != null) {
      previous = getNewPosition(current.previous);
    }
    if (current.next != null) {
      next = getNewPosition(current.next);
    }
    if (previous != null) {
      previous.next = current.next;
    }
    if (next != null) {
      next.previous = current.previous;
    }
  }

  // private boolean equals(char[][][] first, char[][][] second) {
  // if (first == second)
  // return true;
  // if (first == null || second == null)
  // return false;
  // if (first.length != second.length)
  // return false;
  //
  // for (int i = first.length; --i >= 0;)
  // if (!CharOperation.equals(first[i], second[i]))
  // return false;
  // return true;
  // }

  /**
   * Finds elements which have been added or changed.
   */
  private void findAdditions(DartElement newElement, int depth) {
    DartElementInfo oldInfo = getElementInfo(newElement);
    if (oldInfo == null && depth < maxDepth) {
      delta.added(newElement);
      added(newElement);
    } else {
      removeElementInfo(newElement);
    }

    if (depth >= maxDepth) {
      // mark element as changed
      delta.changed(newElement, DartElementDelta.F_CONTENT);
      return;
    }

    DartElementInfo newInfo;
    try {
      newInfo = ((DartElementImpl) newElement).getElementInfo();
    } catch (DartModelException exception) {
      return;
    }

    findContentChange(oldInfo, newInfo, newElement);

    if (oldInfo != null && newElement instanceof ParentElement) {

      DartElement[] children = newInfo.getChildren();
      if (children != null) {
        int length = children.length;
        for (int i = 0; i < length; i++) {
          findAdditions(children[i], depth + 1);
        }
      }
    }
  }

  /**
   * Looks for changed positioning of elements.
   */
  private void findChangesInPositioning(DartElement element, int depth) {
    if (depth >= maxDepth || added.contains(element) || removed.contains(element)) {
      return;
    }

    if (!isPositionedCorrectly(element)) {
      delta.changed(element, DartElementDelta.F_REORDER);
    }

    // if (element instanceof IParent) {
    DartElementInfo info;
    try {
      info = ((DartElementImpl) element).getElementInfo();
    } catch (DartModelException exception) {
      return;
    }

    DartElement[] children = info.getChildren();
    if (children != null) {
      int length = children.length;
      for (int i = 0; i < length; i++) {
        findChangesInPositioning(children[i], depth + 1);
      }
    }
    // }
  }

  // private void findAnnotationChanges(IAnnotation[] oldAnnotations,
  // IAnnotation[] newAnnotations, DartElement parent) {
  // ArrayList annotationDeltas = null;
  // for (int i = 0, length = newAnnotations.length; i < length; i++) {
  // IAnnotation newAnnotation = newAnnotations[i];
  // Object oldInfo = annotationInfos.remove(newAnnotation);
  // if (oldInfo == null) {
  // DartElementDeltaImpl annotationDelta = new
  // DartElementDeltaImpl(newAnnotation);
  // annotationDelta.added();
  // if (annotationDeltas == null) annotationDeltas = new ArrayList();
  // annotationDeltas.add(annotationDelta);
  // continue;
  // } else {
  // AnnotationInfo newInfo = null;
  // try {
  // newInfo = (AnnotationInfo) ((DartElementImpl)
  // newAnnotation).getElementInfo();
  // } catch (DartModelException npe) {
  // return;
  // }
  // if (!Util.equalArraysOrNull(((AnnotationInfo) oldInfo).members,
  // newInfo.members)) {
  // DartElementDeltaImpl annotationDelta = new
  // DartElementDeltaImpl(newAnnotation);
  // annotationDelta.changed(DartElementDelta.F_CONTENT);
  // if (annotationDeltas == null) annotationDeltas = new ArrayList();
  // annotationDeltas.add(annotationDelta);
  // }
  // }
  // }
  // for (int i = 0, length = oldAnnotations.length; i < length; i++) {
  // IAnnotation oldAnnotation = oldAnnotations[i];
  // if (annotationInfos.remove(oldAnnotation) != null) {
  // DartElementDeltaImpl annotationDelta = new
  // DartElementDeltaImpl(oldAnnotation);
  // annotationDelta.removed();
  // if (annotationDeltas == null) annotationDeltas = new ArrayList();
  // annotationDeltas.add(annotationDelta); }
  // }
  // if (annotationDeltas == null)
  // return;
  // int size = annotationDeltas.size();
  // if (size > 0) {
  // DartElementDeltaImpl parentDelta = delta.changed(parent,
  // DartElementDelta.F_ANNOTATIONS);
  // parentDelta.annotationDeltas = (DartElementDelta[])
  // annotationDeltas.toArray(new DartElementDelta[size]);
  // }
  // }
  /**
   * The elements are equivalent, but might have content changes.
   */
  private void findContentChange(DartElementInfo oldInfo, DartElementInfo newInfo,
      DartElement newElement) {
    // if (oldInfo instanceof MemberElementInfo
    // && newInfo instanceof MemberElementInfo) {
    // if (((MemberElementInfo) oldInfo).getModifiers() !=
    // ((MemberElementInfo) newInfo).getModifiers()) {
    // delta.changed(newElement, DartElementDelta.F_MODIFIERS);
    // }
    // if (oldInfo instanceof AnnotatableInfo
    // && newInfo instanceof AnnotatableInfo) {
    // findAnnotationChanges(((AnnotatableInfo) oldInfo).annotations,
    // ((AnnotatableInfo) newInfo).annotations, newElement);
    // }
    if (oldInfo instanceof DartMethodInfo && newInfo instanceof DartMethodInfo) {
      DartMethodInfo oldSourceMethodInfo = (DartMethodInfo) oldInfo;
      DartMethodInfo newSourceMethodInfo = (DartMethodInfo) newInfo;
      if (!CharOperation.equals(
          oldSourceMethodInfo.getReturnTypeName(),
          newSourceMethodInfo.getReturnTypeName())) {
        // || !CharOperation.equals(
        // oldSourceMethodInfo.getTypeParameterNames(),
        // newSourceMethodInfo.getTypeParameterNames())
        // || !equals(oldSourceMethodInfo.getTypeParameterBounds(),
        // newSourceMethodInfo.getTypeParameterBounds())) {
        delta.changed(newElement, DartElementDelta.F_CONTENT);
      }
    } else if (oldInfo instanceof DartFieldInfo && newInfo instanceof DartFieldInfo) {
      if (!CharOperation.equals(
          ((DartFieldInfo) oldInfo).getTypeName(),
          ((DartFieldInfo) newInfo).getTypeName())) {
        delta.changed(newElement, DartElementDelta.F_CONTENT);
      }
    } else if (oldInfo instanceof DartTypeInfo && newInfo instanceof DartTypeInfo) {
      DartTypeInfo oldSourceTypeInfo = (DartTypeInfo) oldInfo;
      DartTypeInfo newSourceTypeInfo = (DartTypeInfo) newInfo;
      if (!CharOperation.equals(
          oldSourceTypeInfo.getSuperclassName(),
          newSourceTypeInfo.getSuperclassName())
          || !CharOperation.equals(
              oldSourceTypeInfo.getInterfaceNames(),
              newSourceTypeInfo.getInterfaceNames())) {
        delta.changed(newElement, DartElementDelta.F_SUPER_TYPES);
      }
      // if (!CharOperation.equals(oldSourceTypeInfo.getTypeParameterNames(),
      // newSourceTypeInfo.getTypeParameterNames())
      // || !equals(oldSourceTypeInfo.getTypeParameterBounds(),
      // newSourceTypeInfo.getTypeParameterBounds())) {
      // delta.changed(newElement, DartElementDelta.F_CONTENT);
      // }
      // HashMap<DartElement, String[]> oldTypeCategories =
      // oldSourceTypeInfo.categories;
      // HashMap<DartElement, String[]> newTypeCategories =
      // newSourceTypeInfo.categories;
      // if (oldTypeCategories != null) {
      // // take the union of old and new categories elements (see
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=125675)
      // Set<DartElement> elements;
      // if (newTypeCategories != null) {
      // elements = new HashSet<DartElement>(oldTypeCategories.keySet());
      // elements.addAll(newTypeCategories.keySet());
      // } else
      // elements = oldTypeCategories.keySet();
      // Iterator<DartElement> iterator = elements.iterator();
      // while (iterator.hasNext()) {
      // DartElement element = iterator.next();
      // String[] oldCategories = oldTypeCategories.get(element);
      // String[] newCategories = newTypeCategories == null ? null :
      // (String[]) newTypeCategories.get(element);
      // if (!Util.equalArraysOrNull(oldCategories, newCategories)) {
      // delta.changed(element, DartElementDelta.F_CATEGORIES);
      // }
      // }
      // } else if (newTypeCategories != null) {
      // Iterator<DartElement> elements =
      // newTypeCategories.keySet().iterator();
      // while (elements.hasNext()) {
      // DartElement element = elements.next();
      // delta.changed(element, DartElementDelta.F_CATEGORIES); // all
      // categories for this element were removed
      // }
      // }
      // }
    }
  }

  /**
   * Adds removed deltas for any handles left in the table
   */
  private void findDeletions() {
    Iterator<DartElement> iter = infos.keySet().iterator();
    while (iter.hasNext()) {
      DartElement element = iter.next();
      delta.removed(element);
      removed(element);
    }
  }

  private DartElementInfo getElementInfo(DartElement element) {
    return infos.get(element);
  }

  private ListItem getNewPosition(DartElement element) {
    return newPositions.get(element);
  }

  private ListItem getOldPosition(DartElement element) {
    return oldPositions.get(element);
  }

  private void initialize() {
    infos = new HashMap<DartElement, DartElementInfo>(20);
    oldPositions = new HashMap<DartElement, ListItem>(20);
    newPositions = new HashMap<DartElement, ListItem>(20);
    oldPositions.put(dartElement, new ListItem(null, null));
    newPositions.put(dartElement, new ListItem(null, null));
    added = new HashSet<DartElement>(5);
    removed = new HashSet<DartElement>(5);
  }

  /**
   * Inserts position information for the elements into the new or old positions table
   */
  private void insertPositions(DartElement[] elements, boolean isNew) {
    int length = elements.length;
    DartElement previous = null, current = null, next = (length > 0) ? elements[0] : null;
    for (int i = 0; i < length; i++) {
      previous = current;
      current = next;
      next = (i + 1 < length) ? elements[i + 1] : null;
      if (isNew) {
        newPositions.put(current, new ListItem(previous, next));
      } else {
        oldPositions.put(current, new ListItem(previous, next));
      }
    }
  }

  /**
   * Returns whether the elements position has not changed.
   */
  private boolean isPositionedCorrectly(DartElement element) {
    ListItem oldListItem = getOldPosition(element);
    if (oldListItem == null) {
      return false;
    }

    ListItem newListItem = getNewPosition(element);
    if (newListItem == null) {
      return false;
    }

    DartElement oldPrevious = oldListItem.previous;
    DartElement newPrevious = newListItem.previous;
    if (oldPrevious == null) {
      return newPrevious == null;
    } else {
      return oldPrevious.equals(newPrevious);
    }
  }

  /**
   * Records this elements info, and attempts to record the info for the children.
   */
  private void recordElementInfo(DartElement element, DartModelImpl model, int depth) {
    if (depth >= maxDepth) {
      return;
    }
    DartElementInfo info = DartModelManager.getInstance().getInfo(element);
    if (info == null) {
      return;
    }
    infos.put(element, info);

    // if (element instanceof IParent) {
    DartElement[] children = info.getChildren();
    if (children != null) {
      insertPositions(children, false);
      for (int i = 0, length = children.length; i < length; i++) {
        recordElementInfo(children[i], model, depth + 1);
      }
    }
    // }
    // IAnnotation[] annotations = null;
    // if (info instanceof AnnotatableInfo)
    // annotations = ((AnnotatableInfo) info).annotations;
    // if (annotations != null) {
    // if (annotationInfos == null)
    // annotationInfos = new HashMap();
    // DartModelManager manager = DartModelManager.getInstance();
    // for (int i = 0, length = annotations.length; i < length; i++) {
    // annotationInfos.put(annotations[i],
    // manager.getInfo(annotations[i]));
    // }
    // }
  }

  /**
   * Fills the newPositions hashtable with the new position information
   */
  private void recordNewPositions(DartElement newElement, int depth) {
    if (depth < maxDepth /* && newElement instanceof IParent */) {
      DartElementInfo info;
      try {
        info = ((DartElementImpl) newElement).getElementInfo();
      } catch (DartModelException exception) {
        return;
      }
      DartElement[] children = info.getChildren();
      if (children != null) {
        insertPositions(children, true);
        for (int i = 0, length = children.length; i < length; i++) {
          recordNewPositions(children[i], depth + 1);
        }
      }
    }
  }

  /**
   * Repairs the positioning information after an element has been removed
   */
  private void removed(DartElement element) {
    removed.add(element);
    ListItem current = getOldPosition(element);
    ListItem previous = null, next = null;
    if (current.previous != null) {
      previous = getOldPosition(current.previous);
    }
    if (current.next != null) {
      next = getOldPosition(current.next);
    }
    if (previous != null) {
      previous.next = current.next;
    }
    if (next != null) {
      next.previous = current.previous;
    }

  }

  private void removeElementInfo(DartElement element) {
    infos.remove(element);
  }

  /**
   * Trims deletion deltas to only report the highest level of deletion
   */
  private void trimDelta(DartElementDeltaImpl elementDelta) {
    if (elementDelta.getKind() == DartElementDelta.REMOVED) {
      DartElementDelta[] children = elementDelta.getAffectedChildren();
      for (int i = 0, length = children.length; i < length; i++) {
        elementDelta.removeAffectedChild((DartElementDeltaImpl) children[i]);
      }
    } else {
      DartElementDelta[] children = elementDelta.getAffectedChildren();
      for (int i = 0, length = children.length; i < length; i++) {
        trimDelta((DartElementDeltaImpl) children[i]);
      }
    }
  }
}
