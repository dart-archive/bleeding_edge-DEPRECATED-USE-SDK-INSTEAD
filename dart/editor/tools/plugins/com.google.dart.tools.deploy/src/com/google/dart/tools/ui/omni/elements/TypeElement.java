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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.ResourceMap;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;

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
    return element.getName();
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
    result.append(element.getName());

    //cache detail offset (used for styling detail area in OmniElement.paint(...))
    detailOffset = result.length();

    LibraryElement library = element.getLibrary();
    if (library != null) {
      result.append(DartElementLabels.CONCAT_STRING);
      result.append(library.getName());
    }
    return result.toString();
  }

  @Override
  protected void doExecute(String text, UIInstrumentationBuilder instrumentation) {
    instrumentation.data("TypeElement.searchResultSelected", element.getName());
    try {
      IFile contextFile = getContextFile();
      DartUI.openInEditor(contextFile, element, true);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }

  private IFile getContextFile() {
    Source source = element.getSource();
    IWorkbenchPage page = DartToolsPlugin.getActivePage();
    if (page != null) {
      // try active editor
      {
        IEditorPart editor = page.getActiveEditor();
        IFile contextFile = getContextFile(source, editor);
        if (contextFile != null) {
          return contextFile;
        }
      }
      // try open editors
      for (IEditorReference editorReference : page.getEditorReferences()) {
        IEditorPart editor = editorReference.getEditor(false);
        IFile contextFile = getContextFile(source, editor);
        if (contextFile != null) {
          return contextFile;
        }
      }
    }
    // not found
    return null;
  }

  private IFile getContextFile(Source source, IEditorPart editor) {
    if (editor instanceof DartEditor) {
      IFile contextFile = ((DartEditor) editor).getInputResourceFile();
      ResourceMap resourceMap = DartCore.getProjectManager().getResourceMap(contextFile);
      if (resourceMap.getResource(source) != null) {
        return contextFile;
      }
    }
    return null;
  }
}
