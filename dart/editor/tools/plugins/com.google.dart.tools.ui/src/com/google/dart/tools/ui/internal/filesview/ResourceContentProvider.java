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
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.pub.PubCacheManager_NEW;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.filesview.nodes.old.IDartNode_OLD;
import com.google.dart.tools.ui.internal.filesview.nodes.old.packages.DartPackageNode_OLD;
import com.google.dart.tools.ui.internal.filesview.nodes.old.packages.InstalledPackagesNode_OLD;
import com.google.dart.tools.ui.internal.filesview.nodes.old.sdk.DartLibraryNode_OLD;
import com.google.dart.tools.ui.internal.filesview.nodes.old.sdk.DartSdkNode_OLD;
import com.google.dart.tools.ui.internal.filesview.nodes.server.IDartNode_NEW;
import com.google.dart.tools.ui.internal.filesview.nodes.server.packages.DartPackageNode_NEW;
import com.google.dart.tools.ui.internal.filesview.nodes.server.packages.InstalledPackagesNode_NEW;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Files view content provider.
 */
public class ResourceContentProvider implements ITreeContentProvider, IResourceChangeListener {
  private static final IResource[] NO_CHILDREN = new IResource[0];

  private StructuredViewer viewer;

  private DartSdkNode_OLD sdkNode;

  private InstalledPackagesNode_OLD packagesNode_OLD;
  private InstalledPackagesNode_NEW packagesNode_NEW;

  private Map<IFileStore, DartLibraryNode_OLD> sdkChildMap;

  public ResourceContentProvider() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);

    sdkNode = DartSdkNode_OLD.createInstance();

    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      packagesNode_NEW = InstalledPackagesNode_NEW.createInstance();
    } else {
      packagesNode_OLD = InstalledPackagesNode_OLD.createInstance();
    }
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
        // add projects
        {
          IProject[] projects = root.getProjects();
          for (IProject project : projects) {
            if (PubCacheManager_NEW.isPubCacheProject(project)) {
              continue;
            }
            children.add(project);
          }
        }
        // add Dart nodes
        children.add(sdkNode);
        if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
          children.add(packagesNode_NEW);
        } else {
          children.add(packagesNode_OLD);
        }
        return children.toArray();
      } else if (element instanceof IContainer) {
        IContainer container = (IContainer) element;
        return filteredMembers(container).toArray();
      } else if (element instanceof IFileStore) {
        IFileStore fileStore = (IFileStore) element;
        return fileStore.childStores(EFS.NONE, null);
      } else if (element instanceof DartSdkNode_OLD) {
        return ((DartSdkNode_OLD) element).getLibraries();
      } else if (element instanceof DartLibraryNode_OLD) {
        return ((DartLibraryNode_OLD) element).getFiles();
      } else if (element instanceof InstalledPackagesNode_NEW) {
        return ((InstalledPackagesNode_NEW) element).getPackages();
      } else if (element instanceof InstalledPackagesNode_OLD) {
        return ((InstalledPackagesNode_OLD) element).getPackages();
      } else if (element instanceof DartPackageNode_NEW) {
        DartPackageNode_NEW node = (DartPackageNode_NEW) element;
        return filteredMembers(node.getProject()).toArray();
      } else if (element instanceof DartPackageNode_OLD) {
        return ((DartPackageNode_OLD) element).getFiles();
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

  public Object getPackagesNode() {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      return packagesNode_NEW;
    } else {
      return packagesNode_OLD;
    }
  }

  @Override
  public Object getParent(Object element) {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      if (element instanceof IProject) {
        IProject project = (IProject) element;
        if (PubCacheManager_NEW.isPubCacheProject(project)) {
          return packagesNode_NEW;
        }
      }
      if (element instanceof IResource) {
        IContainer parent = ((IResource) element).getParent();
        if (parent instanceof IProject) {
          IProject project = (IProject) parent;
          DartPackageNode_NEW node = packagesNode_NEW.getPackage(project);
          if (node != null) {
            return node;
          }
        }
      }
    }
    if (element instanceof IResource) {
      return ((IResource) element).getParent();
    } else if (element instanceof IFileStore) {
      IFileStore fileStore = (IFileStore) element;

      if (getSdkParent(fileStore) != null) {
        return getSdkParent(fileStore);
      }

      return fileStore.getParent();
    } else if (element instanceof IDartNode_NEW) {
      return ((IDartNode_NEW) element).getParent();
    } else if (element instanceof IDartNode_OLD) {
      return ((IDartNode_OLD) element).getParent();
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

  public void updatePackages(Map<String, Object> added) {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      // TODO(scheglov) I'm not sure we want to do anything here.
    } else {
      packagesNode_OLD.updatePackages(added);
    }
  }

  private Map<IFileStore, DartLibraryNode_OLD> createSdkChildMap() {
    Map<IFileStore, DartLibraryNode_OLD> map = new HashMap<IFileStore, DartLibraryNode_OLD>();

    for (DartLibraryNode_OLD library : sdkNode.getLibraries()) {
      for (IFileStore child : library.getFiles()) {
        map.put(child, library);
      }
    }
    return map;
  }

  private List<IResource> filteredMembers(IContainer container) throws CoreException {
    // TODO(scheglov) remove this method when remove "packages" folder
    List<IResource> children = new ArrayList<IResource>();

    if (container == null) {
      return children;
    }

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

  private DartLibraryNode_OLD getSdkParent(IFileStore fileStore) {
    if (sdkChildMap == null) {
      sdkChildMap = createSdkChildMap();
    }

    return sdkChildMap.get(fileStore);
  }
}
