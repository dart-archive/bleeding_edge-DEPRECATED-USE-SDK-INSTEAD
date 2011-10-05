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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;

import org.eclipse.jface.resource.ImageDescriptor;
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

  private static final Image CLASS_ICON = DartPluginImages.get(DartPluginImages.IMG_OBJS_CLASS);
  private static final Image INTERFACE_ICON = DartPluginImages.get(DartPluginImages.IMG_OBJS_INTERFACE);

  private int fFlags;

  public TypeNameMatchLabelProvider(int flags) {
    fFlags = flags;
  }

  /*
   * non java-doc
   * 
   * @see ILabelProvider#getImage
   */
  @Override
  public Image getImage(Object element) {
    if (element instanceof Type) {
//    if (isSet(SHOW_TYPE_CONTAINER_ONLY)) {
//      Type typeRef = (Type) element;
//      if (typeRef.getPackageName().equals(typeRef.getTypeContainerName()))
//        return PKG_ICON;
//
//      // XXX cannot check outer type for interface efficiently (5887)
//      return CLASS_ICON;
//
//    } else if (isSet(SHOW_PACKAGE_ONLY)) {
//      return PKG_ICON;
//    } else {
      Type type = (Type) element;
      try {
        return type.isInterface() ? INTERFACE_ICON : CLASS_ICON;
      } catch (DartModelException e) {
        DartToolsPlugin.log(e);
      }
      return null;
//    }
    } else if (element instanceof DartFunctionTypeAlias) {
      ImageDescriptor descriptor = new DartElementImageProvider().getBaseImageDescriptor(
          (DartFunctionTypeAlias) element, 0);
      return descriptor.createImage();
    }
    return super.getImage(element);
  }

  /*
   * non java-doc
   * 
   * @see ILabelProvider#getText
   */
  @Override
  public String getText(Object element) {
    if (!(element instanceof Type)) {
      return super.getText(element);
    }

    Type typeRef = (Type) element;
    StringBuffer buf = new StringBuffer();
    buf.append(typeRef.getElementName());
//    if (isSet(SHOW_TYPE_ONLY)) {
//      buf.append(typeRef.getSimpleTypeName());
//    } else if (isSet(SHOW_TYPE_CONTAINER_ONLY)) {
//      String containerName = typeRef.getTypeContainerName();
//      buf.append(getPackageName(containerName));
//    } else if (isSet(SHOW_PACKAGE_ONLY)) {
//      String packName = typeRef.getPackageName();
//      buf.append(getPackageName(packName));
//    } else {
//      if (isSet(SHOW_FULLYQUALIFIED)) {
//        buf.append(typeRef.getFullyQualifiedName());
//      } else if (isSet(SHOW_POST_QUALIFIED)) {
//        buf.append(typeRef.getSimpleTypeName());
//        String containerName = typeRef.getTypeContainerName();
//        if (containerName != null && containerName.length() > 0) {
//          buf.append(DartElementLabels.CONCAT_STRING);
//          buf.append(containerName);
//        }
//      } else {
//        buf.append(typeRef.getTypeQualifiedName());
//      }

//      if (isSet(SHOW_PACKAGE_POSTFIX)) {
//        buf.append(DartElementLabels.CONCAT_STRING);
//        String packName = typeRef.getPackageName();
//        buf.append(getPackageName(packName));
//      }
//    }
//    if (isSet(SHOW_ROOT_POSTFIX)) {
//      buf.append(DartElementLabels.CONCAT_STRING);
//      IPackageFragmentRoot root = typeRef.getPackageFragmentRoot();
//      DartElementLabels.getPackageFragmentRootLabel(root,
//          DartElementLabels.ROOT_QUALIFIED, buf);
//    }
    return buf.toString();
  }

//  private String getPackageName(String packName) {
//    if (packName.length() == 0)
//      return DartUIMessages.TypeInfoLabelProvider_default_package;
//    else
//      return packName;
//  }

  @SuppressWarnings("unused")
  private boolean isSet(int flag) {
    return (fFlags & flag) != 0;
  }
}
