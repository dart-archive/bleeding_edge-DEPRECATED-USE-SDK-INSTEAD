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

import com.google.dart.engine.element.LibraryElement;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Element for classes.
 */
public class ClassElement extends OmniElement {

  private static final ImageDescriptor CLASS_ICON = DartPluginImages.DESC_DART_CLASS_PUBLIC;

  private final com.google.dart.engine.element.ClassElement cls;

  public ClassElement(OmniProposalProvider provider,
      com.google.dart.engine.element.ClassElement type) {
    super(provider);
    this.cls = type;
  }

  @Override
  public String getId() {
    return cls.getName();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return CLASS_ICON;
  }

  @Override
  public String getLabel() {
    StringBuffer result = new StringBuffer();
    result.append(cls.getName());

    //cache detail offset (used for styling detail area in OmniElement.paint(...))
    detailOffset = result.length();

    LibraryElement library = cls.getLibrary();
    if (library != null) {
      result.append(DartElementLabels.CONCAT_STRING);
      result.append(library.getName());
    }
    return result.toString();
  }

  @Override
  protected void doExecute(String text, UIInstrumentationBuilder instrumentation) {

    instrumentation.data("ClassElement.searchResultSelected", cls.getName());

// try {

    //TODO (pquitslund): add support for opening Elements to DartUI
    //DartUI.openInEditor(cls, true, true);

//    } catch (PartInitException e) {
//      DartToolsPlugin.log(e);
//    } catch (DartModelException e) {
//      DartToolsPlugin.log(e);
//    }
  }
}
