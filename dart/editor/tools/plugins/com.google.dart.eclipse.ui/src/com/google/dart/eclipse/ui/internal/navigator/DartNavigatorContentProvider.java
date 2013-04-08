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
package com.google.dart.eclipse.ui.internal.navigator;

import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.DartProjectNature;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.model.DartIgnoreEvent;
import com.google.dart.tools.core.model.DartIgnoreListener;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.eclipse.ui.navigator.INavigatorContentService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CNF navigator content provider for dart elements.
 */
public class DartNavigatorContentProvider implements ICommonContentProvider,
    IResourceChangeListener {

  private static final Object[] NONE = new Object[0];

  private Map<IFileStore, DartLibrary> fileStoreMap;

  /**
   * Used to refresh navigator content when ignores are updated.
   */
  private DartIgnoreListener dartIgnoreListener;

  private Viewer viewer;

  public DartNavigatorContentProvider() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  @Override
  public void dispose() {

    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

    if (dartIgnoreListener != null) {
      DartCore.removeIgnoreListener(dartIgnoreListener);
    }
  }

  @Override
  public Object[] getChildren(Object element) {
    try {
      if (element instanceof IProject) {
        return getProjectChildren((IProject) element);
      } else if (element instanceof IContainer) {
        IContainer container = (IContainer) element;
        return filteredMembers(container).toArray();
      } else if (element instanceof IFileStore) {
        IFileStore fileStore = (IFileStore) element;
        return fileStore.childStores(EFS.NONE, null);
      } else if (element instanceof DartLibraryImpl) {
        return getLibraryChildren((DartLibraryImpl) element);
      } else if (element instanceof LibraryNode) {
        return ((LibraryNode) element).getFiles();
      }
    } catch (CoreException e) {
      //fall through
    }

    return NONE;
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
      IFileStore parent = fileStore.getParent();
      if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
        // TODO(keertip): Add support for new model 
      } else {
        if (getFileStoreMap().containsKey(parent)) {
          return getFileStoreMap().get(parent);
        }
      }
      return parent;
    }

    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @Override
  public void init(ICommonContentExtensionSite config) {

    final INavigatorContentService contentService = config.getService();
    dartIgnoreListener = new DartIgnoreListener() {
      @Override
      public void ignoresChanged(DartIgnoreEvent event) {
        contentService.update();
      }
    };

    DartCore.addIgnoreListener(dartIgnoreListener);
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    this.viewer = viewer;
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        viewer.refresh();
      }
    });
  }

  @Override
  public void restoreState(IMemento aMemento) {
  }

  @Override
  public void saveState(IMemento aMemento) {
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

  private Map<IFileStore, DartLibrary> getFileStoreMap() {
    if (fileStoreMap == null) {
      fileStoreMap = new HashMap<IFileStore, DartLibrary>();

      try {
        for (DartLibrary lib : DartModelManager.getInstance().getDartModel().getBundledLibraries()) {
          if (lib instanceof DartLibraryImpl) {
            DartLibraryImpl library = (DartLibraryImpl) lib;

            IFileStore libraryFileStore = getLibraryFileStore(library);

            fileStoreMap.put(libraryFileStore, library);
          }
        }
      } catch (CoreException exception) {
        DartToolsPlugin.log(exception);
      }
    }

    return fileStoreMap;
  }

  private Set<LibraryElement> getImportedSystemLibraries(LibraryElement[] libraries) {
    Set<LibraryElement> results = new HashSet<LibraryElement>();
    for (LibraryElement element : libraries) {
      for (LibraryElement importedLibrary : element.getImportedLibraries()) {
        if (importedLibrary.getSource().isInSystemLibrary()) {
          results.add(importedLibrary);
        }
      }
    }
    return results;
  }

  private Object[] getLibraryChildren(DartLibraryImpl library) throws CoreException {
    IFileStore libraryFileStore = getLibraryFileStore(library);

    if (libraryFileStore != null) {
      return libraryFileStore.childStores(EFS.NONE, null);
    } else {
      return NONE;
    }
  }

  private IFileStore getLibraryFileStore(DartLibraryImpl library) throws CoreException {
    URI uri = library.getLibrarySourceFile().getUri();

    if (PackageLibraryManager.isDartUri(uri)) {
      PackageLibraryManager libraryManager = PackageLibraryManagerProvider.getPackageLibraryManager();

      uri = libraryManager.translateDartUri(uri);
    }

    if ("file".equals(uri.getScheme())) {
      IFileStore fileStore = EFS.getStore(uri);

      return fileStore.getParent();
    } else {
      return null;
    }
  }

  private Object[] getProjectChildren(IProject project) throws CoreException {
    List<Object> children = new ArrayList<Object>();

    if (DartProjectNature.hasDartNature(project)) {
      if (DartCoreDebug.ENABLE_NEW_ANALYSIS) {
        // TODO(keertip): change this to get library sources instead and check if sources are
        // in workspace. This will also catch external packages.
        // TODO(devoncarew): temporarily disable calls to the model - this blocks the UI
        // thread for 5-10 seconds
//        LibraryElement[] libraries = manager.getLibraries(project);
//        if (DartSdkManager.getManager().hasSdk()) {
//          for (LibraryElement element : getImportedSystemLibraries(libraries)) {
//            children.add(new LibraryNode(project, getSourceFileStore(element.getSource()),
//                element.getName()));
//          }
//        }
      } else {
        DartProject dartProject = DartCore.create(project);
        if (DartSdkManager.getManager().hasSdk()) {
          for (DartLibrary library : getSystemLibraries(dartProject.getDartLibraries())) {
            children.add(library);
          }
        }
      }
    }

    return children.toArray();
  }

  private IFileStore getSourceFileStore(Source element) throws CoreException {
    if (element instanceof FileBasedSource) {
      URI uri;
      try {
        uri = new URI(element.getEncoding());
        if ("file".equals(uri.getScheme())) {
          IFileStore fileStore = EFS.getStore(uri);
          return fileStore.getParent();
        }
      } catch (URISyntaxException e) {
        DartCore.logError(e);
      }
    }

    return null;
  }

  private Set<DartLibrary> getSystemLibraries(DartLibrary[] libraries) throws CoreException {
    Set<DartLibrary> results = new HashSet<DartLibrary>();

    for (DartLibrary library : libraries) {
      for (DartLibrary lib : library.getImportedLibraries()) {
        if (!lib.isLocal()) {
          results.add(lib);
        }
      }
    }

    DartLibrary coreLibrary = DartModelManager.getInstance().getDartModel().getCoreLibrary();
    if (!coreLibrary.isLocal()) {
      results.add(coreLibrary);
    }

    return results;
  }

}
