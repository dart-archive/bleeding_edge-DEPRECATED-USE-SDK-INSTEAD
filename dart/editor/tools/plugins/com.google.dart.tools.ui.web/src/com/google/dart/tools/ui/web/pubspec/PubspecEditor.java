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

package com.google.dart.tools.ui.web.pubspec;

import com.google.dart.tools.core.pub.PubspecModel;
import com.google.dart.tools.core.utilities.io.FileUtilities;
import com.google.dart.tools.ui.web.DartWebPlugin;
import com.google.dart.tools.ui.web.yaml.YamlEditor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;

import java.io.File;
import java.io.IOException;

/**
 * A multi-page forms editor for the pubspec.yaml file
 */
public class PubspecEditor extends FormEditor {

  public static String ID = "com.google.dart.tools.ui.editor.pubspec";

  private PubspecModel model;
  private YamlEditor yamlEditor;
  private OverviewFormPage formPage;

  public PubspecEditor() {

  }

  @Override
  public void doSave(IProgressMonitor monitor) {
    commitPages(true);
    if (getActivePage() == 0 && model.isDirty()) {
      updateDocumentContents();
    }
    if (getActivePage() == 1) {
      model.initialize(yamlEditor.getDocument().get());
    }
    yamlEditor.doSave(monitor);
    editorDirtyStateChanged();
  }

  @Override
  public void doSaveAs() {
  }

  public PubspecModel getModel() {
    return model;
  }

  @Override
  public String getTitle() {
    return "pubspec.yaml";
  }

  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

  @Override
  public void setFocus() {
    super.setFocus();
    IFormPage page = getActivePageInstance();
    if ((page != null) && (page instanceof OverviewFormPage)) {
      ((OverviewFormPage) page).updateFormSelection();
    }
  }

  @Override
  protected void addPages() {
    try {
      yamlEditor = new YamlEditor();
      formPage = new OverviewFormPage(this);
      addPage(formPage);
      addPage(yamlEditor, getEditorInput());
      setPageText(1, "Source");
    } catch (PartInitException e) {

    }
  }

  @Override
  protected FormToolkit createToolkit(Display display) {
    return new FormToolkit(DartWebPlugin.getPlugin().getFormColors(display));
  }

  @Override
  protected void pageChange(int newPageIndex) {
    if (newPageIndex == 0) {
      if (yamlEditor.isDirty()) {
        if (model != null) {
          model.initialize(yamlEditor.getDocument().get());
        }
      }
    }
    if (newPageIndex == 1) {
      if (formPage.isDirty()) {
        updateDocumentContents();
        model.setDirty(false);
      }
    }
    super.pageChange(newPageIndex);
  }

  @Override
  protected void setInput(IEditorInput input) {
    super.setInput(input);

    if (input instanceof IFileEditorInput) {
      File file = ((IFileEditorInput) input).getFile().getLocation().toFile();
      String contents = null;
      try {
        contents = FileUtilities.getContents(file, "UTF-8");
      } catch (IOException e) {
        DartWebPlugin.logError(e);
      }
      // TODO(keerti): more error checking/recovery if contents cannot be parsed
      model = new PubspecModel(contents);
    }

  }

  private void updateDocumentContents() {
    String contents = model.getContents();
    if (contents != null) {
      yamlEditor.getDocument().set(contents);
    }
  }
}
