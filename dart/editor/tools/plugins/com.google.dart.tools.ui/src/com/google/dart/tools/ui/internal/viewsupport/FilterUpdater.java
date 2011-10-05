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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Control;

public class FilterUpdater implements IResourceChangeListener {

  private ProblemTreeViewer fViewer;

  public FilterUpdater(ProblemTreeViewer viewer) {
    Assert.isNotNull(viewer);
    fViewer = viewer;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.
   * eclipse.core.resources.IResourceChangeEvent)
   */
  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (fViewer.getInput() == null) {
      return;
    }
    IResourceDelta delta = event.getDelta();
    if (delta == null) {
      return;
    }
    IResourceDelta[] projDeltas = delta.getAffectedChildren(IResourceDelta.CHANGED);
    for (int i = 0; i < projDeltas.length; i++) {
      IResourceDelta pDelta = projDeltas[i];
      if ((pDelta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
        IProject project = (IProject) pDelta.getResource();
        if (needsRefiltering(project)) {
          final Control ctrl = fViewer.getControl();
          if (ctrl != null && !ctrl.isDisposed()) {
            // async is needed due to bug 33783
            ctrl.getDisplay().asyncExec(new Runnable() {
              @Override
              public void run() {
                if (!ctrl.isDisposed()) {
                  fViewer.refresh(false);
                }
              }
            });
          }
          return; // one refresh is good enough
        }
      }
    }
  }

  private boolean needsRefiltering(IProject project) {
    try {
      Object element = project;
      if (project.hasNature(JavaScriptCore.NATURE_ID)) {
        element = DartCore.create(project);
      }
      boolean inView = fViewer.testFindItem(element) != null;
      boolean afterFilter = !fViewer.isFiltered(element, fViewer.getInput());

      return inView != afterFilter;
    } catch (CoreException e) {
      return true;
    }
  }
}
