/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.server.Element;
import com.google.dart.server.ElementKind;
import com.google.dart.tools.ui.DartElementImageDescriptor;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A {@link LabelProvider} for analysis server {@link Element}.
 */
public class ElementLabelProvider_NEW extends LabelProvider implements
    org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider {
  private static final Point SIZE = new Point(22, 16);
  private static final ImageDescriptorRegistry registry = DartToolsPlugin.getImageDescriptorRegistry();
  private static final String RIGHT_ARROW = " \u2192 "; //$NON-NLS-1$

  public static ImageDescriptor getImageDescriptor(Element element) {
    ElementKind kind = element.getKind();
    ImageDescriptor base = getBaseImageDescriptor(kind, element.isPrivate());
    if (base == null) {
      return null;
    }
    int flags = 0;
    if (kind == ElementKind.CONSTRUCTOR) {
      flags |= DartElementImageDescriptor.CONSTRUCTOR;
    }
    if (kind == ElementKind.GETTER) {
      flags |= DartElementImageDescriptor.GETTER;
    }
    if (kind == ElementKind.SETTER) {
      flags |= DartElementImageDescriptor.SETTER;
    }
    if (element.isAbstract()) {
      flags |= DartElementImageDescriptor.ABSTRACT;
    }
    if (element.isTopLevelOrStatic()) {
      flags |= DartElementImageDescriptor.STATIC;
    }
    if (element.isDeprecated()) {
      flags |= DartElementImageDescriptor.DEPRECATED;
    }
    return new DartElementImageDescriptor(base, flags, SIZE);
  }

  private static ImageDescriptor getBaseImageDescriptor(ElementKind kind, boolean isPrivate) {
    if (kind == ElementKind.CLASS || kind == ElementKind.CLASS_TYPE_ALIAS) {
      return isPrivate ? DartPluginImages.DESC_DART_CLASS_PRIVATE
          : DartPluginImages.DESC_DART_CLASS_PUBLIC;
    }
    if (kind == ElementKind.FUNCTION_TYPE_ALIAS) {
      return isPrivate ? DartPluginImages.DESC_DART_FUNCTIONTYPE_PRIVATE
          : DartPluginImages.DESC_DART_FUNCTIONTYPE_PUBLIC;
    }
    if (kind == ElementKind.FIELD || kind == ElementKind.TOP_LEVEL_VARIABLE) {
      return isPrivate ? DartPluginImages.DESC_DART_FIELD_PRIVATE
          : DartPluginImages.DESC_DART_FIELD_PUBLIC;
    }
    if (kind == ElementKind.CONSTRUCTOR || kind == ElementKind.FUNCTION
        || kind == ElementKind.GETTER || kind == ElementKind.METHOD || kind == ElementKind.SETTER) {
      return isPrivate ? DartPluginImages.DESC_DART_METHOD_PRIVATE
          : DartPluginImages.DESC_DART_METHOD_PUBLIC;
    }
    if (kind == ElementKind.COMPILATION_UNIT) {
      return DartPluginImages.DESC_DART_COMP_UNIT;
    }
    if (kind == ElementKind.LIBRARY) {
      return DartPluginImages.DESC_DART_LIB_FILE;
    }
    if (kind == ElementKind.UNIT_TEST_CASE) {
      return DartPluginImages.DESC_DART_TEST_CASE;
    }
    if (kind == ElementKind.UNIT_TEST_GROUP) {
      return DartPluginImages.DESC_DART_TEST_GROUP;
    }
    return null;
  }

  @Override
  public Image getImage(Object obj) {
    Element element = (Element) obj;
    ImageDescriptor descriptor = getImageDescriptor(element);
    if (descriptor != null) {
      return registry.get(descriptor);
    }
    return null;
  }

  @Override
  public StyledString getStyledText(Object obj) {
    Element element = (Element) obj;
    StyledString styledString = new StyledString(getText(obj));
    // append parameters
    String parameters = element.getParameters();
    if (parameters != null) {
      styledString.append(parameters, StyledString.DECORATIONS_STYLER);
    }
    // append return type
    String returnType = element.getReturnType();
    if (!StringUtils.isEmpty(returnType)) {
      if (element.getKind() == ElementKind.FIELD
          || element.getKind() == ElementKind.TOP_LEVEL_VARIABLE) {
        styledString.append(" : " + returnType, StyledString.QUALIFIER_STYLER);
      } else {
        styledString.append(RIGHT_ARROW + returnType, StyledString.QUALIFIER_STYLER);
      }
    }
    // done
    return styledString;
  }

  @Override
  public String getText(Object obj) {
    return ((Element) obj).getName();
  }
}
