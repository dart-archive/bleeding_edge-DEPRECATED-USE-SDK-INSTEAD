/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class TypeNameMatchLabelProvider extends LabelProvider {

  public static final int SHOW_FULLYQUALIFIED = 0x01;
  public static final int SHOW_PACKAGE_POSTFIX = 0x02;
  public static final int SHOW_PACKAGE_ONLY = 0x04;
  public static final int SHOW_ROOT_POSTFIX = 0x08;
  public static final int SHOW_TYPE_ONLY = 0x10;
  public static final int SHOW_TYPE_CONTAINER_ONLY = 0x20;
  public static final int SHOW_POST_QUALIFIED = 0x40;

  private static final Image CLASS_ICON = DartToolsPlugin.getImage(DartPluginImages.DESC_DART_CLASS_PUBLIC);
  private static final Image FUNCTION_TYPE_ALIAS_ICON = DartToolsPlugin.getImage(DartPluginImages.DESC_DART_FUNCTIONTYPE_PUBLIC);

  @SuppressWarnings("unused")
  private int fFlags;

  public TypeNameMatchLabelProvider(int flags) {
    fFlags = flags;
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof ClassElement) {
      return CLASS_ICON;
    } else if (element instanceof FunctionTypeAliasElement) {
      return FUNCTION_TYPE_ALIAS_ICON;
    }
    return super.getImage(element);
  }

  @Override
  public String getText(Object element) {
    if (element instanceof Element) {
      return ((Element) element).getName();
    }
    return super.getText(element);
  }

}
