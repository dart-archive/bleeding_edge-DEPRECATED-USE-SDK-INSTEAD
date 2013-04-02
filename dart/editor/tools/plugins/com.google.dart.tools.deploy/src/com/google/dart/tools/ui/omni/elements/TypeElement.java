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
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.jface.resource.ImageDescriptor;

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
      DartUI.openInEditor(element);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
  }
}
