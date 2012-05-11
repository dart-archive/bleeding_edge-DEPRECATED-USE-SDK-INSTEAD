/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
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
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PartInitException;

/**
 * Element for types.
 */
public class TypeElement extends OmniElement {

  private static final ImageDescriptor CLASS_ICON = DartPluginImages.DESC_DART_CLASS_PUBLIC;
  private static final ImageDescriptor INTERFACE_ICON = DartPluginImages.DESC_DART_INTERFACE;

  private final Type type;

  public TypeElement(OmniProposalProvider provider, Type type) {
    super(provider);
    this.type = type;
  }

  @Override
  public void execute(String text) {
    try {
      DartUI.openInEditor(type, true, true);
    } catch (PartInitException e) {
      DartToolsPlugin.log(e);
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }
  }

  @Override
  public String getId() {
    return type.getElementName();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    try {
      return type.isInterface() ? INTERFACE_ICON : CLASS_ICON;
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }
    return null;
  }

  @Override
  public String getLabel() {
    StringBuffer result = new StringBuffer();
    result.append(type.getElementName());

    DartLibrary library = type.getLibrary();
    if (library != null) {
      result.append(DartElementLabels.CONCAT_STRING);
      result.append(library.getDisplayName());
    }
    return result.toString();
  }

}
