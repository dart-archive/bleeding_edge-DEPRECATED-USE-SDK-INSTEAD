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
package com.google.dart.tools.ui.web.css;

import com.google.dart.tools.ui.web.DartWebPlugin;
import com.google.dart.tools.ui.web.utils.NodeContentProvider;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * The outline page for the CSS editor.
 */
public class CssContentOutlinePage extends ContentOutlinePage {
  private CssEditor editor;

  /**
   * Create a new CssContentOutlinePage.
   * 
   * @param editor
   */
  public CssContentOutlinePage(CssEditor editor) {
    this.editor = editor;
  }

  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);

    getTreeViewer().setLabelProvider(new CssLabelProvider());
    getTreeViewer().setContentProvider(new NodeContentProvider());
    getTreeViewer().setInput(editor.getModel());
  }

  protected void handleEditorReconcilation() {
    if (!getControl().isDisposed()) {
      refreshAsync();
    }
  }

  private void refresh() {
    try {
      if (!getTreeViewer().getControl().isDisposed()) {
        getTreeViewer().setInput(editor.getModel());
      }
    } catch (Throwable exception) {
      DartWebPlugin.logError(exception);

      getTreeViewer().setInput(null);
    }
  }

  private void refreshAsync() {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        refresh();
      }
    });
  }

}
