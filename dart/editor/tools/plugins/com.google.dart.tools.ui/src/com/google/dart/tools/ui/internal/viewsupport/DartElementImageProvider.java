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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.ui.DartPluginImages;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * Default strategy of the Dart plugin for the construction of Dart element icons.
 */
public class DartElementImageProvider {

  /**
   * Flags for the DartElementImageProvider: Generate images with overlays.
   */
  public final static int OVERLAY_ICONS = 0x1;

  /**
   * Generate small sized images.
   */
  public final static int SMALL_ICONS = 0x2;

  public static final Point SMALL_SIZE = new Point(16, 16);
  public static final Point BIG_SIZE = new Point(22, 16);

  private static final NewDartElementImageProvider newImageProvider = new NewDartElementImageProvider();

  public static ImageDescriptor getFieldImageDescriptor(boolean isInInterfaceOrAnnotation,
      boolean isPrivate) {
    if (isInInterfaceOrAnnotation) {
      return DartPluginImages.DESC_DART_FIELD_PUBLIC;
    }
    if (isPrivate) {
      return DartPluginImages.DESC_DART_FIELD_PRIVATE;
    }
    return DartPluginImages.DESC_DART_FIELD_PUBLIC;
  }

  public static ImageDescriptor getLibraryImageDescriptor(int flags) {
    return DartPluginImages.DESC_DART_LIB_FILE;
  }

  public static ImageDescriptor getMethodImageDescriptor(boolean isInInterfaceOrAnnotation,
      boolean isPrivate) {
    if (isInInterfaceOrAnnotation) {
      return DartPluginImages.DESC_DART_METHOD_PUBLIC;
    }
    if (isPrivate) {
      return DartPluginImages.DESC_DART_METHOD_PRIVATE;
    }
    return DartPluginImages.DESC_DART_METHOD_PUBLIC;
  }

  public static ImageDescriptor getTypeImageDescriptor(boolean isInterface, boolean isPrivate) {
    if (isInterface) {
      return getInterfaceImageDescriptor(isPrivate);
    } else {
      return getClassImageDescriptor(isPrivate);
    }
  }

  private static ImageDescriptor getClassImageDescriptor(boolean isPrivate) {
    if (isPrivate) {
      return DartPluginImages.DESC_DART_CLASS_PRIVATE;
    } else {
      return DartPluginImages.DESC_DART_CLASS_PUBLIC;
    }
  }

  private static ImageDescriptor getInterfaceImageDescriptor(boolean isPrivate) {
    if (isPrivate) {
      return DartPluginImages.DESC_DART_INNER_INTERFACE_PRIVATE;
    } else {
      return DartPluginImages.DESC_DART_INTERFACE;
    }
  }

  public DartElementImageProvider() {
  }

  public void dispose() {
  }

  /**
   * Returns the icon for a given element. The icon depends on the element type and element
   * properties. If configured, overlay icons are constructed for <code>SourceReference</code>s.
   * 
   * @param flags Flags as defined by the JavaImageLabelProvider
   */
  public Image getImageLabel(Object element, int flags) {

    return newImageProvider.getImageLabel(element, flags);

  }
}
