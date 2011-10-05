/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ISetSelectionTarget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SelectionUtil {

  /**
   * Returns the selected element if the selection consists of a single element only.
   * 
   * @param s the selection
   * @return the selected first element or null
   */
  public static Object getSingleElement(ISelection s) {
    if (!(s instanceof IStructuredSelection)) {
      return null;
    }
    IStructuredSelection selection = (IStructuredSelection) s;
    if (selection.size() != 1) {
      return null;
    }

    return selection.getFirstElement();
  }

  /**
   * Attempts to select and reveal the specified resources in all parts within the supplied
   * workbench window's active page.
   * <p>
   * Checks all parts in the active page to see if they implement <code>ISetSelectionTarget</code>,
   * either directly or as an adapter. If so, tells the part to select and reveal the specified
   * resources.
   * </p>
   * 
   * @param resources the resources to be selected and revealed
   * @param window the workbench window to select and reveal the resource
   * @see ISetSelectionTarget
   * @see org.eclipse.ui.wizards.newresource.BasicNewResourceWizard#selectAndReveal(IResource,
   *      IWorkbenchWindow)
   */
  public static void selectAndReveal(IResource[] resources, IWorkbenchWindow window) {
    // validate the input
    if (window == null || resources == null || Arrays.asList(resources).contains(null)) {
      return;
    }
    IWorkbenchPage page = window.getActivePage();
    if (page == null) {
      return;
    }

    // get all the view and editor parts
    List<IWorkbenchPart> parts = new ArrayList<IWorkbenchPart>();
    IWorkbenchPartReference refs[] = page.getViewReferences();
    for (int i = 0; i < refs.length; i++) {
      IWorkbenchPart part = refs[i].getPart(false);
      if (part != null) {
        parts.add(part);
      }
    }
    refs = page.getEditorReferences();
    for (int i = 0; i < refs.length; i++) {
      if (refs[i].getPart(false) != null) {
        parts.add(refs[i].getPart(false));
      }
    }

    final ISelection selection = new StructuredSelection(resources);
    Iterator<IWorkbenchPart> itr = parts.iterator();
    while (itr.hasNext()) {
      IWorkbenchPart part = itr.next();

      // get the part's ISetSelectionTarget implementation
      ISetSelectionTarget target = null;
      if (part instanceof ISetSelectionTarget) {
        target = (ISetSelectionTarget) part;
      } else {
        target = (ISetSelectionTarget) part.getAdapter(ISetSelectionTarget.class);
      }

      if (target != null) {
        // select and reveal resource
        final ISetSelectionTarget finalTarget = target;
        window.getShell().getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            finalTarget.selectReveal(selection);
          }
        });
      }
    }
  }

  @SuppressWarnings("rawtypes")
  public static List toList(ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      return ((IStructuredSelection) selection).toList();
    }
    return null;
  }

}
