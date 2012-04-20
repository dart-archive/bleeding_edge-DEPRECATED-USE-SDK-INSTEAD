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
package com.google.dart.tools.internal.corext.refactoring.reorg;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ISetSelectionTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public class RenameSelectionState {
  private final Display fDisplay;
  private final Object fElement;
  private final List<IWorkbenchPart> fParts;
  private final List<IStructuredSelection> fSelections;

  public RenameSelectionState(Object element) {
    fElement = element;
    fParts = new ArrayList<IWorkbenchPart>();
    fSelections = new ArrayList<IStructuredSelection>();

    IWorkbenchWindow dw = DartToolsPlugin.getActiveWorkbenchWindow();
    if (dw == null) {
      fDisplay = null;
      return;
    }
    fDisplay = dw.getShell().getDisplay();
    IWorkbenchPage page = dw.getActivePage();
    if (page == null) {
      return;
    }
    IViewReference vrefs[] = page.getViewReferences();
    for (int i = 0; i < vrefs.length; i++) {
      consider(vrefs[i].getPart(false));
    }
    IEditorReference refs[] = page.getEditorReferences();
    for (int i = 0; i < refs.length; i++) {
      consider(refs[i].getPart(false));
    }
  }

  public void restore(Object newElement) {
    if (fDisplay == null) {
      return;
    }
    for (int i = 0; i < fParts.size(); i++) {
      IStructuredSelection currentSelection = fSelections.get(i);
      boolean changed = false;
      final ISetSelectionTarget target = (ISetSelectionTarget) fParts.get(i);
      final IStructuredSelection[] newSelection = new IStructuredSelection[1];
      newSelection[0] = currentSelection;
      if (currentSelection instanceof TreeSelection) {
        TreeSelection treeSelection = (TreeSelection) currentSelection;
        TreePath[] paths = treeSelection.getPaths();
        for (int p = 0; p < paths.length; p++) {
          TreePath path = paths[p];
          if (path.getSegmentCount() > 0 && path.getLastSegment().equals(fElement)) {
            paths[p] = createTreePath(path, newElement);
            changed = true;
          }
        }
        if (changed) {
          newSelection[0] = new TreeSelection(paths, treeSelection.getElementComparer());
        }
      } else {
        Object[] elements = currentSelection.toArray();
        for (int e = 0; e < elements.length; e++) {
          if (elements[e].equals(fElement)) {
            elements[e] = newElement;
            changed = true;
          }
        }
        if (changed) {
          newSelection[0] = new StructuredSelection(elements);
        }
      }
      if (changed) {
        fDisplay.asyncExec(new Runnable() {
          @Override
          public void run() {
            target.selectReveal(newSelection[0]);
          }
        });
      }
    }
  }

  private void consider(IWorkbenchPart part) {
    if (part == null) {
      return;
    }
    ISetSelectionTarget target = null;
    if (!(part instanceof ISetSelectionTarget)) {
      target = (ISetSelectionTarget) part.getAdapter(ISetSelectionTarget.class);
      if (target == null) {
        return;
      }
    } else {
      target = (ISetSelectionTarget) part;
    }
    ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
    if (selectionProvider == null) {
      return;
    }
    ISelection s = selectionProvider.getSelection();
    if (!(s instanceof IStructuredSelection)) {
      return;
    }
    IStructuredSelection selection = (IStructuredSelection) s;
    if (!selection.toList().contains(fElement)) {
      return;
    }
    fParts.add(part);
    fSelections.add(selection);
  }

  // Method assumes that segment count of path > 0.
  private TreePath createTreePath(TreePath old, Object newElement) {
    int count = old.getSegmentCount();
    Object[] newObjects = new Object[count];
    for (int i = 0; i < count - 1; i++) {
      newObjects[i] = old.getSegment(i);
    }
    newObjects[count - 1] = newElement;
    return new TreePath(newObjects);
  }
}