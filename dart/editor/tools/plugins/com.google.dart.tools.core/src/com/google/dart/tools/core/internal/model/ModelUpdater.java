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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Instances of the class <code>ModelUpdater</code> are used by <code>DartModelManager</code> to
 * update the Dart model based on some <code>DartElementDelta</code>s.
 */
public class ModelUpdater {
  /**
   * Closes the given element, which removes it from the cache of open elements.
   */
  protected static void close(OpenableElementImpl element) {
    try {
      element.close();
    } catch (DartModelException e) {
      // do nothing
    }
  }

  HashSet<DartProjectImpl> projectsToUpdate = new HashSet<DartProjectImpl>();

  /**
   * Converts a <code>IResourceDelta</code> rooted in a <code>Workspace</code> into the
   * corresponding set of <code>DartElementDelta</code>, rooted in the relevant
   * <code>DartModel</code>s.
   */
  public void processDartDelta(DartElementDelta delta) {
    try {
      traverseDelta(delta, null, null); // traverse delta

      // reset project caches of projects that were affected
      Iterator<DartProjectImpl> iterator = projectsToUpdate.iterator();
      while (iterator.hasNext()) {
        DartProjectImpl project = iterator.next();
        project.resetCaches();
      }
    } finally {
      projectsToUpdate = new HashSet<DartProjectImpl>();
    }
  }

  /**
   * Adds the given child handle to its parent's cache of children.
   */
  protected void addToParentInfo(OpenableElementImpl child) {
    OpenableElementImpl parent = (OpenableElementImpl) child.getParent();
    if (parent != null && parent.isOpen()) {
      try {
        OpenableElementInfo info = (OpenableElementInfo) parent.getElementInfo();
        info.addChild(child);
      } catch (DartModelException exception) {
        // do nothing - we already checked if open
      }
    }
  }

  /**
   * Processing for an element that has been added:
   * <ul>
   * <li>If the element is a project, do nothing, and do not process children, as when a project is
   * created it does not yet have any natures - specifically a Dart nature.
   * <li>If the element is not a project, process it as added (see <code>basicElementAdded</code>.
   * </ul>
   */
  protected void elementAdded(OpenableElementImpl element) {
    int elementType = element.getElementType();
    if (elementType == DartElement.DART_PROJECT) {
      // project add is handled by DartProjectImpl.configure() because
      // when a project is created, it does not yet have a Dart nature
      addToParentInfo(element);
      projectsToUpdate.add((DartProjectImpl) element);
    } else {
      addToParentInfo(element);

      // Force the element to be closed as it might have been opened
      // before the resource modification came in and it might have a new child
      // For example, in an IWorkspaceRunnable:
      // 1. create a package fragment p using a Dart model operation
      // 2. open package p
      // 3. add file X.dart in folder p
      // When the resource delta comes in, only the addition of p is notified,
      // but the package p is already opened, thus its children are not
      // recomputed
      // and it appears empty.
      close(element);
    }

    switch (elementType) {
      case DartElement.LIBRARY:
        // when a root is added, the project must be updated
        projectsToUpdate.add((DartProjectImpl) element.getDartProject());
        break;
    }
  }

  /**
   * Generic processing for elements with changed contents:
   * <ul>
   * <li>The element is closed such that any subsequent accesses will re-open the element reflecting
   * its new structure.
   * </ul>
   */
  protected void elementChanged(OpenableElementImpl element) {
    close(element);
  }

  /**
   * Generic processing for a removed element:
   * <ul>
   * <li>Close the element, removing its structure from the cache
   * <li>Remove the element from its parent's cache of children
   * <li>Add a REMOVED entry in the delta
   * </ul>
   */
  protected void elementRemoved(OpenableElementImpl element) {
    if (element.isOpen()) {
      close(element);
    }
    removeFromParentInfo(element);
    int elementType = element.getElementType();

    DartCore.notYetImplemented();
    switch (elementType) {
      case DartElement.DART_MODEL:
        // DartModelManager.getIndexManager().reset();
        break;
      case DartElement.DART_PROJECT:
        // DartModelManager manager = DartModelManager.getInstance();
        // DartProjectImpl dartProject = (DartProjectImpl) element;
        // manager.removePerProjectInfo(dartProject, true /* remove external jar
        // files indexes and timestamps */);
        // manager.containerRemove(dartProject);
        break;
      case DartElement.LIBRARY:
        projectsToUpdate.add((DartProjectImpl) element.getDartProject());
        break;
    }
  }

  /**
   * Removes the given element from its parents cache of children. If the element does not have a
   * parent, or the parent is not currently open, this has no effect.
   */
  protected void removeFromParentInfo(OpenableElementImpl child) {
    OpenableElementImpl parent = (OpenableElementImpl) child.getParent();
    if (parent != null && parent.isOpen()) {
      try {
        OpenableElementInfo info = (OpenableElementInfo) parent.getElementInfo();
        info.removeChild(child);
      } catch (DartModelException exception) {
        // do nothing - we already checked if open
      }
    }
  }

  /**
   * Converts an <code>IResourceDelta</code> and its children into the corresponding
   * <code>DartElementDelta</code>s. Return whether the delta corresponds to a resource on the
   * classpath. If it is not a resource on the classpath, it will be added as a non-Dart resource by
   * the sender of this method.
   */
  protected void traverseDelta(DartElementDelta delta, DartLibrary root, DartProject project) {

    boolean processChildren = true;

    OpenableElementImpl element = (OpenableElementImpl) delta.getElement();
    switch (element.getElementType()) {
      case DartElement.DART_PROJECT:
        project = (DartProject) element;
        break;
      case DartElement.LIBRARY:
        root = (DartLibrary) element;
        break;
      case DartElement.COMPILATION_UNIT:
        // filter out working copies that are not primary (we don't want to
        // add/remove them to/from the package fragment
        CompilationUnitImpl cu = (CompilationUnitImpl) element;
        if (cu.isWorkingCopy() && !cu.isPrimary()) {
          return;
        }
        // $FALL-THROUGH$
        // case DartElement.CLASS_FILE :
        processChildren = false;
        break;
    }

    switch (delta.getKind()) {
      case DartElementDelta.ADDED:
        elementAdded(element);
        break;
      case DartElementDelta.REMOVED:
        elementRemoved(element);
        break;
      case DartElementDelta.CHANGED:
        if ((delta.getFlags() & DartElementDelta.F_CONTENT) != 0) {
          elementChanged(element);
        }
        break;
    }
    if (processChildren) {
      DartElementDelta[] children = delta.getAffectedChildren();
      for (int i = 0; i < children.length; i++) {
        DartElementDelta childDelta = children[i];
        traverseDelta(childDelta, root, project);
      }
    }
  }
}
