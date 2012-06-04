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
package com.google.dart.tools.ui.internal.appsview;

import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartProject;

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
import java.util.List;

public class AppsViewContentProvider implements ITreeContentProvider, IResourceChangeListener {

  private static final IResource[] NO_CHILDREN = new IResource[0];
  private Viewer viewer;

  public AppsViewContentProvider() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  @Override
  public void dispose() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

  @Override
  public Object[] getChildren(Object element) {
    try {
      if (element instanceof IWorkspaceRoot) {
        List<DartLibrary> allLibs = findAllLibraries((IWorkspaceRoot) element);
        List<DartLibrary> topLibs = selectTopLevelLibraries(allLibs);
        return topLibs.toArray();
      } else if (element instanceof DartLibrary) {
        DartLibrary lib = (DartLibrary) element;
        Object[] children = getChildren(lib);
        return children;
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
    if (element instanceof CompilationUnit) {
      // TODO Fix up to handle DAG
      return ((CompilationUnit) element).getLibrary();
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
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        if (viewer != null && !viewer.getControl().isDisposed()) {
          viewer.refresh();
        }
      }
    });
  }

  private List<DartLibrary> findAllLibraries(IWorkspaceRoot root) throws CoreException {
    DartModel model = DartModelManager.getInstance().getDartModel();
    List<DartLibrary> children = new ArrayList<DartLibrary>();
    for (IResource res : root.members()) {
      DartProject proj = model.getDartProject(res);
      for (DartLibrary lib : proj.getDartLibraries()) {
        children.add(lib);
      }
    }
    return children;
  }

  private Object[] getChildren(DartLibrary lib) throws CoreException {
    List<Object> children = new ArrayList<Object>();
    for (DartLibrary imp : lib.getImportedLibraries()) {
      children.add(imp);
    }
    for (CompilationUnit cu : lib.getCompilationUnits()) {
      children.add(cu);
    }
    return children.toArray();
  }

  private List<DartLibrary> selectTopLevelLibraries(List<DartLibrary> libs) throws CoreException {
    List<DartLibrary> topLevel = new ArrayList<DartLibrary>();
    nextLib : for (DartLibrary possibleTopLevelLib : libs) {
      for (DartLibrary libWithImports : libs) {
        for (DartImport imp : libWithImports.getImports()) {
          if (imp.getLibrary().equals(possibleTopLevelLib)) {
            // not top level
            continue nextLib;
          }
        }
      }
      topLevel.add(possibleTopLevelLib);
    }
    return topLevel;
  }
}
