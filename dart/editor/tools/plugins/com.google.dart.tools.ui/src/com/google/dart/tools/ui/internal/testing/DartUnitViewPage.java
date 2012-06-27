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

package com.google.dart.tools.ui.internal.testing;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.Page;

/**
 * A page for the Tests view. A page represents all the content for a project, and persists (along
 * with UI state like selection and tree node expansion) for as long as an editor on a file in that
 * project is open.
 */
public class DartUnitViewPage extends Page {
  private IProject project;
  private TreeViewer treeViewer;

  public DartUnitViewPage() {
    this(null);
  }

  public DartUnitViewPage(IProject project) {
    this.project = project;
  }

  @Override
  public void createControl(Composite parent) {
    treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    treeViewer.setAutoExpandLevel(2);

    if (project != null) {
      treeViewer.setLabelProvider(new WorkbenchLabelProvider());
      treeViewer.setContentProvider(new TestsContentProvider(project));
      treeViewer.setComparator(new ViewerComparator(String.CASE_INSENSITIVE_ORDER));
      treeViewer.setInput(project);
    }
  }

  @Override
  public Control getControl() {
    return treeViewer.getControl();
  }

  public IProject getProject() {
    return project;
  }

  @Override
  public void setFocus() {
    treeViewer.getControl().setFocus();
  }

  protected void activated(DartUnitView view) {
    updateContentDescription(view);
  }

  private void updateContentDescription(DartUnitView view) {
    String desc = (project == null ? "" : "Tests for " + project.getName());

    view.setContentDescription(desc);
  }

}
