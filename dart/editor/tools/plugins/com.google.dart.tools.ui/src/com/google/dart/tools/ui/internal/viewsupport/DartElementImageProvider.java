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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;

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

  private static ImageDescriptor DESC_OBJ_PROJECT_CLOSED;
  private static ImageDescriptor DESC_OBJ_PROJECT;

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

  private static ImageDescriptor getFuntionTypeDescriptor(DartFunctionTypeAlias element) {
    if (element.isPrivate()) {
      return DartPluginImages.DESC_DART_FUNCTIONTYPE_PRIVATE;
    } else {
      return DartPluginImages.DESC_DART_FUNCTIONTYPE_PUBLIC;
    }
  }

  private static ImageDescriptor getInterfaceImageDescriptor(boolean isPrivate) {
    if (isPrivate) {
      return DartPluginImages.DESC_DART_INNER_INTERFACE_PRIVATE;
    } else {
      return DartPluginImages.DESC_DART_INTERFACE;
    }
  }

  {
    ISharedImages images = DartToolsPlugin.getDefault().getWorkbench().getSharedImages();
    DESC_OBJ_PROJECT_CLOSED = images.getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED);
    DESC_OBJ_PROJECT = images.getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT);
  }

  public DartElementImageProvider() {
  }

  public void dispose() {
  }

  /**
   * Returns an image descriptor for a java element. This is the base image, no overlays.
   */
  @SuppressWarnings("unused")
  public ImageDescriptor getBaseImageDescriptor(DartElement element, int renderFlags) {
    switch (element.getElementType()) {
      case DartElement.FUNCTION:
        if (((DartFunction) element).isPrivate()) {
          return DartPluginImages.DESC_DART_METHOD_PRIVATE;
        }
        if (element.getParent().getElementType() == DartElement.COMPILATION_UNIT) {
          return DartPluginImages.DESC_DART_METHOD_PUBLIC;
        } else {
          // If functions defined within methods are displayed in the outline (or elsewhere) then a
          // new icon should be added, since they are private but not defined by the user as such.
          return DartPluginImages.DESC_MISC_DEFAULT;
        }
      case DartElement.FUNCTION_TYPE_ALIAS:
        return getFuntionTypeDescriptor((DartFunctionTypeAlias) element);
      case DartElement.METHOD: {
        Method method = (Method) element;
        return getMethodImageDescriptor(false, method.isPrivate());
      }
      case DartElement.FIELD: {
        TypeMember member = (TypeMember) element;
        Type declType = member.getDeclaringType();
        return getFieldImageDescriptor(false, member.isPrivate());
      }
//        case DartElement.LOCAL_VARIABLE:
//          return JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE;

      case DartElement.IMPORT_CONTAINER:
        return DartPluginImages.DESC_OBJS_IMPCONT;

      case DartElement.TYPE: {
        Type type = (Type) element;

        boolean isInterface = false;
        return getTypeImageDescriptor(isInterface, type.isPrivate());
      }

      case DartElement.COMPILATION_UNIT:
        return getCompilationUnitDescriptor((CompilationUnit) element);

      case DartElement.LIBRARY:
        return getLibraryImageDescriptor(renderFlags);

      case DartElement.DART_PROJECT:
        DartProject project = (DartProject) element;
        if (project.getProject().isOpen()) {
          IProject project2 = project.getProject();
          IWorkbenchAdapter adapter = (IWorkbenchAdapter) project2.getAdapter(IWorkbenchAdapter.class);
          if (adapter != null) {
            ImageDescriptor result = adapter.getImageDescriptor(project2);
            if (result != null) {
              return result;
            }
          }
          return DESC_OBJ_PROJECT;
        }
        return DESC_OBJ_PROJECT_CLOSED;

      case DartElement.DART_MODEL:
        return DartPluginImages.DESC_OBJS_JAVA_MODEL;

      case DartElement.VARIABLE: {
        DartVariableDeclaration var = (DartVariableDeclaration) element;
        //TODO (pquitslund): top-level vars should have their own descriptors
        return getFieldImageDescriptor(false, var.isPrivate());
      }
      case DartElement.CLASS_TYPE_ALIAS: {
        return DartPluginImages.DESC_DART_CLASS_TYPE_ALIAS;
      }
      default:
        return DartPluginImages.DESC_OBJS_GHOST;

    }

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

  private ImageDescriptor getCompilationUnitDescriptor(CompilationUnit element) {
    return DartPluginImages.DESC_DART_COMP_UNIT;
  }
}
