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
package com.google.dart.tools.ui.internal.filesview;

import com.google.common.collect.Lists;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Files view content provider.
 */
public class ResourceContentProvider implements ITreeContentProvider, IResourceChangeListener {
  private static final IResource[] NO_CHILDREN = new IResource[0];

  private StructuredViewer viewer;

  private DartSdkNode sdkNode;

  private InstalledPackagesNode packagesNode;

  private Map<IFileStore, DartLibraryNode> sdkChildMap;

  public ResourceContentProvider() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);

    sdkNode = DartSdkNode.createInstance();

    packagesNode = InstalledPackagesNode.createInstance();
  }

  @Override
  public void dispose() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

  @Override
  public Object[] getChildren(Object element) {
    try {
      if (element instanceof IWorkspaceRoot) {
        IWorkspaceRoot root = (IWorkspaceRoot) element;

        List<Object> children = new ArrayList<Object>();

        children.addAll(Arrays.asList(root.members()));
        children.add(sdkNode);
        children.add(packagesNode);
        return children.toArray();
      } else if (element instanceof IContainer) {
        IContainer container = (IContainer) element;
        return filteredMembers(container).toArray();
      } else if (element instanceof IFileStore) {
        IFileStore fileStore = (IFileStore) element;
        return fileStore.childStores(EFS.NONE, null);
      } else if (element instanceof DartSdkNode) {
        return ((DartSdkNode) element).getLibraries();
      } else if (element instanceof DartLibraryNode) {
        return ((DartLibraryNode) element).getFiles();
      } else if (element instanceof InstalledPackagesNode) {
        return ((InstalledPackagesNode) element).getPackages();
      } else if (element instanceof DartPackageNode) {
        return ((DartPackageNode) element).getFiles();
      }
    } catch (CoreException ce) {
      //fall through
    }

    return NO_CHILDREN;
  }

  @Override
  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  public InstalledPackagesNode getPackagesNode() {
    return packagesNode;
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof IResource) {
      return ((IResource) element).getParent();
    } else if (element instanceof IFileStore) {
      IFileStore fileStore = (IFileStore) element;

      if (getSdkParent(fileStore) != null) {
        return getSdkParent(fileStore);
      }

      return fileStore.getParent();
    } else if (element instanceof IDartNode) {
      return ((IDartNode) element).getParent();
    } else {
      return null;
    }
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    this.viewer = (StructuredViewer) viewer;
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    final List<IResource> changedResources = getChangedResources(event);
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        if (viewer != null && !viewer.getControl().isDisposed()) {
          viewer.refresh(false);
          for (IResource resource : changedResources) {
            viewer.update(resource, null);
          }
        }
      }
    });
  }

  private Map<IFileStore, DartLibraryNode> createSdkChildMap() {
    Map<IFileStore, DartLibraryNode> map = new HashMap<IFileStore, DartLibraryNode>();

    for (DartLibraryNode library : sdkNode.getLibraries()) {
      for (IFileStore child : library.getFiles()) {
        map.put(child, library);
      }
    }
    return map;
  }

  private List<IResource> filteredMembers(IContainer container) throws CoreException {
    List<IResource> children = new ArrayList<IResource>();

    for (IResource child : container.members()) {
      String name = child.getName();
      if (name.equals(DartCore.PACKAGES_DIRECTORY_NAME)) {
        // Only show packages directories at top level and as sibling to pubspec
        if (container.getProject() == container
            || container.findMember(DartCore.PUBSPEC_FILE_NAME) != null) {
          children.add(child);
        }
      } else if (!name.startsWith(".")) {
        children.add(child);
      }
    }

    return children;
  }

  /**
   * Returns {@link IResource} changed by this {@link IResourceChangeEvent}.
   */
  private List<IResource> getChangedResources(IResourceChangeEvent event) {
    final List<IResource> updatedResources = Lists.newArrayList();
    IResourceDelta delta = event.getDelta();
    if (delta != null) {
      try {
        delta.accept(new IResourceDeltaVisitor() {
          @Override
          public boolean visit(IResourceDelta delta) throws CoreException {
            IResource resource = delta.getResource();
            if (resource != null) {
              updatedResources.add(resource);
            }
            return true;
          }
        });
      } catch (Throwable e) {
        DartToolsPlugin.log(e);
      }
    }
    return updatedResources;
  }

  private DartLibraryNode getSdkParent(IFileStore fileStore) {
    if (sdkChildMap == null) {
      sdkChildMap = createSdkChildMap();
    }

    return sdkChildMap.get(fileStore);
  }

}
