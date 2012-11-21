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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
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

  private Viewer viewer;

  private DartSdkNode sdkNode;

  private Map<IFileStore, DartLibraryNode> sdkChildMap;

  public ResourceContentProvider() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

    sdkNode = new DartSdkNode();
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
    this.viewer = viewer;
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    // handle deletes in POST_CHANGE to guard against model building failures during refresh
    //due to resources being deleted out from under the model
    if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
      return;
    }
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        if (viewer != null && !viewer.getControl().isDisposed()) {
            viewer.refresh();
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
      if (!(name.startsWith("."))) {
        children.add(child);
      }
    }

    return children;
  }

  private DartLibraryNode getSdkParent(IFileStore fileStore) {
    if (sdkChildMap == null) {
      sdkChildMap = createSdkChildMap();
    }

    return sdkChildMap.get(fileStore);
  }

}
