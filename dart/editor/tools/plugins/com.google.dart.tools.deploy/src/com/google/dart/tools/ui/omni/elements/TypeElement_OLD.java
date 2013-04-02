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
package com.google.dart.tools.ui.omni.elements;

import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PartInitException;

/**
 * Element for types.
 */
public class TypeElement_OLD extends OmniElement {

  private static final ImageDescriptor CLASS_ICON = DartPluginImages.DESC_DART_CLASS_PUBLIC;

  private final Type type;

  public TypeElement_OLD(OmniProposalProvider provider, Type type) {
    super(provider);
    this.type = type;
  }

  @Override
  public String getId() {
    return type.getElementName();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return CLASS_ICON;
  }

  @Override
  public String getLabel() {
    StringBuffer result = new StringBuffer();
    result.append(type.getElementName());

    //cache detail offset (used for styling detail area in OmniElement.paint(...))
    detailOffset = result.length();

    DartLibrary library = type.getLibrary();
    if (library != null) {
      result.append(DartElementLabels.CONCAT_STRING);
      result.append(library.getDisplayName());
    }
    return result.toString();
  }

  @Override
  protected void doExecute(String text, UIInstrumentationBuilder instrumentation) {
    try {

      instrumentation.data("typeName", type.getElementName());

      DartUI.openInEditor(type, true, true);
    } catch (PartInitException e) {
      DartToolsPlugin.log(e);
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }
  }

}
