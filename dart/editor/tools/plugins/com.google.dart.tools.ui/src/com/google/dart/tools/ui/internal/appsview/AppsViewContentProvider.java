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
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartToolsPlugin;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppsViewContentProvider implements ITreeContentProvider, IResourceChangeListener {

  private static final IResource[] NO_CHILDREN = new IResource[0];
  private Viewer viewer;
  private Map<DartElement, ElementTreeNode> map;

  public AppsViewContentProvider() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  @Override
  public void dispose() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

  public ElementTreeNode findNode(Object resource) {
    ElementTreeNode elementNode = null;
    for (DartElement key : map.keySet()) {
      try {
        if (resource.equals(key.getCorrespondingResource())) {
          elementNode = map.get(key);
          if (elementNode.getModelElement() instanceof CompilationUnit) {
            return elementNode; // quickly return the most specific match
          }
        }
      } catch (DartModelException e) {
        DartToolsPlugin.log(e);
      }
    }
    return elementNode; // return the library or null or not found
  }

  @Override
  public Object[] getChildren(Object element) {
    if (element instanceof ElementTreeNode) {
      ElementTreeNode node = (ElementTreeNode) element;
      if (node.getChildNodes() != null) {
        return node.getChildNodes().toArray();
      }
      element = node.getModelElement();
    }
    try {
      if (element instanceof IWorkspaceRoot) {
        List<DartLibrary> allLibs = findAllLibraries((IWorkspaceRoot) element);
        List<DartLibrary> topLibs = selectTopLevelLibraries(allLibs);
        return makeTreeNodes(topLibs, element).toArray();
      } else if (element instanceof DartLibrary) {
        List<DartElement> children = getChildren((DartLibrary) element);
        return makeTreeNodes(children, element).toArray();
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
    ElementTreeNode x = map.get(element);
    if (x != null && x.getParentNode() != null) {
      return x.getParentNode();
    }
    if (element instanceof ElementTreeNode) {
      return ((ElementTreeNode) element).getParentNode();
    }
    return findNode(element);
  }

  @Override
  public boolean hasChildren(Object element) {
    if (element instanceof ElementTreeNode) {
      ElementTreeNode node = (ElementTreeNode) element;
      if (node.getChildNodes() != null) {
        return !node.getChildNodes().isEmpty();
      }
    }
    return getChildren(element).length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    this.viewer = viewer;
    this.map = new HashMap<DartElement, ElementTreeNode>();
  }

  @Override
  public void resourceChanged(final IResourceChangeEvent event) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        if (viewer != null && !viewer.getControl().isDisposed()) {
          removeCachedElement(event.getResource());
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

  private List<DartElement> getChildren(DartLibrary lib) throws CoreException {
    List<DartElement> children = new ArrayList<DartElement>();
    for (DartLibrary imp : lib.getImportedLibraries()) {
      children.add(imp);
    }
    for (CompilationUnit cu : lib.getCompilationUnits()) {
      children.add(cu);
    }
    return children;
  }

  private List<? extends Object> makeTreeNodes(List<? extends DartElement> children, Object parent) {
    ElementTreeNode parentNode = map.get(parent);
    List<ElementTreeNode> result = new ArrayList<ElementTreeNode>();
    for (DartElement child : children) {
      ElementTreeNode node = map.get(child);
      if (node == null) {
        node = new ElementTreeNode(child, parentNode);
        map.put(child, node);
      }
      result.add(node);
    }
    if (parentNode != null) {
      parentNode.setChildNodes(result);
    }
    return result;
  }

  private void removeCachedElement(IResource resource) {
    if (resource == null) {
      return;
    }
    ElementTreeNode nodeToRemove = findNode(resource);
    if (nodeToRemove == null) {
      return;
    }
    removeCachedSubTree(nodeToRemove);
  }

  private void removeCachedSubTree(ElementTreeNode node) {
    if (node.getChildNodes() != null) {
      for (ElementTreeNode child : node.getChildNodes()) {
        removeCachedSubTree(child);
      }
    }
    map.remove(node);
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
