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
package com.google.dart.tools.core.internal.model.search;

import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.DartProjectImpl;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;

import org.eclipse.core.runtime.IPath;

import java.util.HashSet;
import java.util.Set;

/**
 * Instances of the class <code>DartWorkspaceScope</code> implement a Dart-specific scope for
 * searching the entire workspace. The scope can be configured to not search binaries. By default,
 * binaries are included.
 */
@Deprecated
public class DartWorkspaceScope extends AbstractSearchScope {
  private IPath[] enclosingPaths = null;

  public DartWorkspaceScope() {
    super();
  }

  @Override
  public boolean encloses(DartElement element) {
    /*
     * A workspace scope encloses all java elements (this assumes that the index selector and thus
     * enclosingProjectAndJars() returns indexes on the classpath only and that these indexes are
     * consistent.) NOTE: Returning true gains 20% of a hierarchy build on Object
     */
    return true;
  }

  public boolean encloses(String resourcePathString) {
    /*
     * A workspace scope encloses all resources (this assumes that the index selector and thus
     * enclosingProjectAndJars() returns indexes on the classpath only and that these indexes are
     * consistent.) NOTE: Returning true gains 20% of a hierarchy build on Object
     */
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jdt.core.search.IJavaSearchScope#enclosingProjectsAndJars()
   */
  public IPath[] enclosingProjectsAndJars() {
    IPath[] result = this.enclosingPaths;
    if (result != null) {
      return result;
    }
    // long start = BasicSearchEngine.VERBOSE ? System.currentTimeMillis() : -1;
    try {
      DartProject[] projects = DartModelManager.getInstance().getDartModel().getDartProjects();
      Set<IPath> paths = new HashSet<IPath>(projects.length * 2);
      for (int i = 0, length = projects.length; i < length; i++) {
        DartProjectImpl project = (DartProjectImpl) projects[i];

        // Add project full path
        IPath projectPath = project.getProject().getFullPath();
        paths.add(projectPath);

        // Add project libraries paths
        // IClasspathEntry[] entries = project.getResolvedClasspath();
        // for (int j = 0, eLength = entries.length; j < eLength; j++) {
        // IClasspathEntry entry = entries[j];
        // if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
        // IPath path = entry.getPath();
        // Object target = DartModelImpl.getTarget(path, false/*don't check
        // existence*/);
        // if (target instanceof IFolder) // case of an external folder
        // path = ((IFolder) target).getFullPath();
        // paths.add(entry.getPath());
        // }
        // }
      }
      result = new IPath[paths.size()];
      paths.toArray(result);
      return this.enclosingPaths = result;
    } catch (DartModelException e) {
      Util.log(e, "Exception while computing workspace scope's enclosing projects and jars"); //$NON-NLS-1$
      return new IPath[0];
      // } finally {
      // if (BasicSearchEngine.VERBOSE) {
      // long time = System.currentTimeMillis() - start;
      // int length = result == null ? 0 : result.length;
      //      Util.verbose("DartWorkspaceScope.enclosingProjectsAndJars: "+length+" paths computed in "+time+"ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      // }
    }
  }

  @Override
  public boolean equals(Object o) {
    return o == this; // use the singleton pattern
  }

  // public AccessRuleSet getAccessRuleSet(String relativePath, String
  // containerPath) {
  // // Do not consider access rules on workspace scope
  // return null;
  // }

  @Override
  public int hashCode() {
    return DartWorkspaceScope.class.hashCode();
  }

  /**
   * @see AbstractJavaSearchScope#packageFragmentRoot(String, int, String)
   */
  // public IPackageFragmentRoot packageFragmentRoot(String resourcePathString,
  // int jarSeparatorIndex, String jarPath) {
  // HashMap rootInfos = DartModelManager.getDeltaState().roots;
  // DeltaProcessor.RootInfo rootInfo = null;
  // if (jarPath != null) {
  // IPath path = new Path(jarPath);
  // rootInfo = (DeltaProcessor.RootInfo) rootInfos.get(path);
  // } else {
  // IPath path = new Path(resourcePathString);
  // if (ExternalFoldersManager.isInternalPathForExternalFolder(path)) {
  // IResource resource =
  // DartModel.getWorkspaceTarget(path.uptoSegment(2/*linked folders for
  // external folders are always of size 2*/));
  // if (resource != null)
  // rootInfo = (DeltaProcessor.RootInfo) rootInfos.get(resource.getLocation());
  // } else {
  // rootInfo = (DeltaProcessor.RootInfo) rootInfos.get(path);
  // while (rootInfo == null && path.segmentCount() > 0) {
  // path = path.removeLastSegments(1);
  // rootInfo = (DeltaProcessor.RootInfo) rootInfos.get(path);
  // }
  // }
  // }
  // if (rootInfo == null)
  // return null;
  // return rootInfo.getPackageFragmentRoot(null/*no resource hint*/);
  // }

  @Override
  public void processDelta(DartElementDelta delta, int eventType) {
    if (this.enclosingPaths == null) {
      return;
    }
    DartElement element = delta.getElement();
    switch (element.getElementType()) {
      case DartElement.DART_MODEL:
        DartElementDelta[] children = delta.getAffectedChildren();
        for (int i = 0, length = children.length; i < length; i++) {
          DartElementDelta child = children[i];
          processDelta(child, eventType);
        }
        break;
      case DartElement.DART_PROJECT:
        int kind = delta.getKind();
        switch (kind) {
          case DartElementDelta.ADDED:
          case DartElementDelta.REMOVED:
            this.enclosingPaths = null;
            break;
          case DartElementDelta.CHANGED:
            int flags = delta.getFlags();
            if ((flags & DartElementDelta.F_CLOSED) != 0
                || (flags & DartElementDelta.F_OPENED) != 0) {
              this.enclosingPaths = null;
            } else {
              children = delta.getAffectedChildren();
              for (int i = 0, length = children.length; i < length; i++) {
                DartElementDelta child = children[i];
                processDelta(child, eventType);
              }
            }
            break;
        }
        break;
      // case DartElement.PACKAGE_FRAGMENT_ROOT:
      // kind = delta.getKind();
      // switch (kind) {
      // case DartElementDelta.ADDED:
      // case DartElementDelta.REMOVED:
      // this.enclosingPaths = null;
      // break;
      // case DartElementDelta.CHANGED:
      // int flags = delta.getFlags();
      // if ((flags & DartElementDelta.F_ADDED_TO_CLASSPATH) > 0
      // || (flags & DartElementDelta.F_REMOVED_FROM_CLASSPATH) > 0) {
      // this.enclosingPaths = null;
      // }
      // break;
      // }
      // break;
      case DartElement.LIBRARY:
        kind = delta.getKind();
        switch (kind) {
          case DartElementDelta.ADDED:
          case DartElementDelta.REMOVED:
            this.enclosingPaths = null;
            break;
          case DartElementDelta.CHANGED:
            break;
        }
        break;
    }
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("DartWorkspaceScope on "); //$NON-NLS-1$
    IPath[] paths = enclosingProjectsAndJars();
    int length = paths == null ? 0 : paths.length;
    if (length == 0) {
      result.append("[empty scope]"); //$NON-NLS-1$
    } else {
      result.append("["); //$NON-NLS-1$
      for (int i = 0; i < length; i++) {
        result.append("\n\t"); //$NON-NLS-1$
        result.append(paths[i]);
      }
      result.append("\n]"); //$NON-NLS-1$
    }
    return result.toString();
  }
}
