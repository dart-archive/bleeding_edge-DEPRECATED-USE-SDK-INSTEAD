/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.omni.elements;

import com.google.dart.compiler.resolver.ClassAliasElement;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.ResourceMap;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;

/**
 * {@link OmniElement} for types.
 */
public class TypeElement extends OmniElement {
  private final Element element;

  public TypeElement(OmniProposalProvider provider, Element element) {
    super(provider);
    this.element = element;
  }

  @Override
  public String getId() {
    return element.getDisplayName();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    if (element instanceof ClassAliasElement) {
      return DartPluginImages.DESC_DART_CLASS_TYPE_ALIAS;
    }
    if (element instanceof FunctionTypeAliasElement) {
      return DartPluginImages.DESC_DART_FUNCTIONTYPE_PUBLIC;
    }
    return DartPluginImages.DESC_DART_CLASS_PUBLIC;
  }

  @Override
  public String getLabel() {
    StringBuffer result = new StringBuffer();
    result.append(element.getDisplayName());

    //cache detail offset (used for styling detail area in OmniElement.paint(...))
    detailOffset = result.length();

    LibraryElement library = element.getLibrary();
    if (library != null) {
      result.append(DartElementLabels.CONCAT_STRING);
      result.append(library.getDisplayName());
    }
    return result.toString();
  }

  @Override
  protected void doExecute(String text, UIInstrumentationBuilder instrumentation) {
    instrumentation.data("TypeElement.searchResultSelected", element.getDisplayName());
    try {
      // prepare resource to open
      IFile elementFile = getElementFile();
      if (elementFile == null) {
        return;
      }
      // open editor
      IEditorPart editor = EditorUtility.openInEditor(elementFile, true);
      if (editor == null) {
        return;
      }
      // reveal element
      EditorUtility.revealInEditor(editor, element);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }

  /**
   * @return the {@link IFile} with {@link #element} to open, may be {@code null}.
   */
  private IFile getElementFile() {
    AnalysisContext elementContext = element.getContext();
    Source elementSource = element.getSource();
    ResourceMap map = DartCore.getProjectManager().getResourceMap(elementContext);
    return map.getResource(elementSource);
  }

  // TODO(scheglov) remove this
//  private IFile getContextFile(Source source, IEditorPart editor) {
//    if (editor instanceof DartEditor) {
//      IFile contextFile = ((DartEditor) editor).getInputResourceFile();
//      ResourceMap resourceMap = DartCore.getProjectManager().getResourceMap(contextFile);
//      if (resourceMap.getResource(source) != null) {
//        return contextFile;
//      }
//    }
//    return null;
//  }
//
//  private IResource getContextResource() {
//    Source source = element.getSource();
//    IWorkbenchPage page = DartToolsPlugin.getActivePage();
//    if (page != null) {
//      // try active editor
//      {
//        IEditorPart editor = page.getActiveEditor();
//        IFile contextFile = getContextFile(source, editor);
//        if (contextFile != null) {
//          return contextFile;
//        }
//      }
//      // try open editors
//      for (IEditorReference editorReference : page.getEditorReferences()) {
//        IEditorPart editor = editorReference.getEditor(false);
//        IFile contextFile = getContextFile(source, editor);
//        if (contextFile != null) {
//          return contextFile;
//        }
//      }
//      // try Files view selection
//      {
//        IResource selection = getFilesViewSelection();
//        if (selection != null) {
//          return selection;
//        }
//      }
//    }
//    // not found
//    return null;
//  }
//
//  private IResource getFilesViewSelection() {
//    // prepare IWorkbenchPage
//    IWorkbenchPage activePage = DartToolsPlugin.getActivePage();
//    if (activePage == null) {
//      return null;
//    }
//    // prepare Files view
//    IViewPart filesView = activePage.findView(FilesView.VIEW_ID);
//    if (filesView == null) {
//      return null;
//    }
//    // prepare ISelectionProvider
//    ISelectionProvider selectionProvider = filesView.getViewSite().getSelectionProvider();
//    if (selectionProvider == null) {
//      return null;
//    }
//    // prepare selection
//    ISelection selection = selectionProvider.getSelection();
//    if (!(selection instanceof IStructuredSelection)) {
//      return null;
//    }
//    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
//    // IResource should be selected
//    Object selectedFileObject = structuredSelection.getFirstElement();
//    if (selectedFileObject instanceof IResource) {
//      return (IResource) selectedFileObject;
//    }
//    // wrong selection
//    return null;
//  }
}
