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
import com.google.dart.tools.ui.web.yaml.PubspecYamlEditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.texteditor.IElementStateListener;

import java.io.File;
import java.io.IOException;

/**
 * A multi-page forms editor for the pubspec.yaml file
 */
public class PubspecEditor extends FormEditor {

  class ElementListener implements IElementStateListener {
    @Override
    public void elementContentAboutToBeReplaced(Object element) {

    }

    @Override
    public void elementContentReplaced(Object element) {
      if (element != null && element.equals(getEditorInput())) {
        doRevert();
      }
    }

    @Override
    public void elementDeleted(Object element) {

    }

    @Override
    public void elementDirtyStateChanged(Object element, boolean isDirty) {

    }

    @Override
    public void elementMoved(Object originalElement, Object movedElement) {
      if (originalElement != null && originalElement.equals(getEditorInput())) {
        dispose();
        close(true);
      }
    }
  }

  public static String ID = "com.google.dart.tools.ui.editor.pubspec";

  private IElementStateListener elementStateListener = new ElementListener();

  private QualifiedName PROPERTY_EDITOR_PAGE_KEY = new QualifiedName(
      DartWebPlugin.PLUGIN_ID,
      "editor-page-index"); //$NON-NLS-1$

  private PubspecModel model;
  private PubspecYamlEditor yamlEditor;
  private OverviewFormPage formPage;

  public PubspecEditor() {

  }

  @Override
  public void dispose() {
    IEditorInput input = getEditorInput();
    if (input instanceof IFileEditorInput) {
      setPropertyEditorPageKey((IFileEditorInput) input);
    }
    super.dispose();
  }

  public void doRevert() {
    model.initialize(getContents((IFileEditorInput) getEditorInput()));
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
      yamlEditor = new PubspecYamlEditor();
      formPage = new OverviewFormPage(this);
      addPage(formPage);
      addPage(yamlEditor, getEditorInput());
      setPageText(1, "Source");
    } catch (PartInitException e) {

    }
    IEditorInput input = getEditorInput();
    if (input instanceof IFileEditorInput) {
      String pageIndex = getPropertyEditorPageKey((IFileEditorInput) input);
      if (pageIndex != null) {
        try {
          int i = Integer.parseInt(pageIndex);
          if (i == 0 || i == 1) { // editor has only 2 pages
            setActivePage(i);
          }
        } catch (Exception e) {
          // do nothing
        }
      }
    }
    elementStateListener = new ElementListener();
    yamlEditor.getDocumentProvider().addElementStateListener(elementStateListener);
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
      String contents = getContents((IFileEditorInput) input);
      // TODO(keerti): more error checking/recovery if contents cannot be parsed
      model = new PubspecModel(contents);
    }
  }

  private String getContents(IFileEditorInput input) {
    File file = input.getFile().getLocation().toFile();
    String contents = null;
    try {
      contents = FileUtilities.getContents(file, "UTF-8");
    } catch (IOException e) {
      DartWebPlugin.logError(e);
    }
    return contents;
  }

  private String getPropertyEditorPageKey(IFileEditorInput input) {
    IFile file = input.getFile();
    // Get the persistent editor page key from the file
    try {
      return file.getPersistentProperty(PROPERTY_EDITOR_PAGE_KEY);
    } catch (CoreException e) {
      return null;
    }
  }

  private void setPropertyEditorPageKey(IFileEditorInput input) {
    // We are using the file itself to persist the editor page key property
    // The value persists even after the editor is closed
    IFile file = input.getFile();
    try {
      // Set the editor page index as a persistent property on the file
      file.setPersistentProperty(PROPERTY_EDITOR_PAGE_KEY, Integer.toString(getCurrentPage()));
    } catch (CoreException e) {
      // Ignore
    }
  }

  private void updateDocumentContents() {
    String contents = model.getContents();
    if (contents != null) {
      yamlEditor.getDocument().set(contents);
    }
  }
}
