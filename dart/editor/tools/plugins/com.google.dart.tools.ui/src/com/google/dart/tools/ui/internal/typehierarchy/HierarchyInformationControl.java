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
package com.google.dart.tools.ui.internal.typehierarchy;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.text.functions.AbstractInformationControl;
import com.google.dart.tools.ui.internal.viewsupport.ColoringLabelProvider;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

/**
 * Show hierarchy in light-weight control.
 */
public class HierarchyInformationControl extends AbstractInformationControl {

  private TypeHierarchyContentProvider contentProvider;
  private HierarchyLabelProvider labelProvider;

  public HierarchyInformationControl(Shell parent, int shellStyle, int treeStyle) {
    super(parent, shellStyle, treeStyle, DartEditorActionDefinitionIds.OPEN_HIERARCHY, true);
  }

  @Override
  public void setInput(Object _input) {
    DartElement input = (DartElement) _input;
    Object selection = input instanceof TypeMember ? input.getAncestor(Type.class) : input;
    inputChanged(new Object[] {input}, selection);
  }

  @Override
  protected TreeViewer createTreeViewer(Composite parent, int style) {
    Tree tree = new Tree(parent, SWT.SINGLE | (style & ~SWT.MULTI));
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = tree.getItemHeight() * 12;
    tree.setLayoutData(gd);

    TreeViewer treeViewer = new TreeViewer(tree);
    treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
    treeViewer.addFilter(new ViewerFilter() {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        return element instanceof Type;
      }
    });

    contentProvider = new TypeHierarchyContentProvider();
    treeViewer.setContentProvider(contentProvider);

    labelProvider = new HierarchyLabelProvider(contentProvider.getLightPredicate());
    ColoringLabelProvider coloringLabelProvider = new ColoringLabelProvider(labelProvider);
    treeViewer.setLabelProvider(coloringLabelProvider);
    coloringLabelProvider.setOwnerDrawEnabled(true);

    return treeViewer;
  }

  @Override
  protected String getId() {
    return "com.google.dart.tools.ui.internal.typehierarchy.HierarchyInformationControl";
  }

  @Override
  protected Object getSelectedElement() {
    Object selectedElement = super.getSelectedElement();
    return contentProvider.convertSelectedElement(selectedElement);
  }
}
