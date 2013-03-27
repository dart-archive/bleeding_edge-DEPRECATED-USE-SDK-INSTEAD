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

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.tools.ui.DartElementImageDescriptor;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.ImportedDartLibrary;
import com.google.dart.tools.ui.ImportedDartLibraryContainer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.model.IWorkbenchAdapter;

import java.io.File;

/**
 * Dart element image provider.
 */
public class NewDartElementImageProvider {

  /**
   * Flag to generate images with overlays.
   */
  public final static int OVERLAY_ICONS = 0x1;

  /**
   * Flag to generate small sized images.
   */
  public final static int SMALL_ICONS = 0x2;

  public static final Point SMALL_SIZE = new Point(16, 16);
  public static final Point BIG_SIZE = new Point(22, 16);

  private static ImageDescriptor DESC_READ_ONLY;

  static {
    DESC_READ_ONLY = DartToolsPlugin.getImageDescriptor("icons/full/ovr16/lock_ovr.png"); //$NON-NLS-1$
  }

  public static Image getDecoratedImage(ImageDescriptor baseImage, int adornments, Point size) {
    return DartToolsPlugin.getImageDescriptorRegistry().get(
        new DartElementImageDescriptor(baseImage, adornments, size));
  }

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

  private static ImageDescriptor decorate(ImageDescriptor main, ImageDescriptor badge) {
    return new DecorationOverlayIcon(
        DartToolsPlugin.getImageDescriptorRegistry().get(main),
        badge,
        IDecoration.BOTTOM_RIGHT);
  }

//  @SuppressWarnings("unused")
//  private static ImageDescriptor getFunctionTypeDescriptor(DartFunctionTypeAlias element) {
//    if (element.isPrivate()) {
//      return DartPluginImages.DESC_DART_FUNCTIONTYPE_PRIVATE;
//    } else {
//      return DartPluginImages.DESC_DART_FUNCTIONTYPE_PUBLIC;
//    }
//  }

  private static ImageDescriptor getClassImageDescriptor(boolean isPrivate) {
    return isPrivate ? DartPluginImages.DESC_DART_CLASS_PRIVATE
        : DartPluginImages.DESC_DART_CLASS_PUBLIC;
  }

  private static boolean showOverlayIcons(int flags) {
    return (flags & OVERLAY_ICONS) != 0;
  }

  private static boolean useSmallSize(int flags) {
    return (flags & SMALL_ICONS) != 0;
  }

  private ImageDescriptorRegistry descriptorRegistry;

  public void dispose() {
  }

  /**
   * Returns an image descriptor for a Dart element. This is the base image, no overlays.
   */
  public ImageDescriptor getBaseImageDescriptor(Element element, int renderFlags) {

    switch (element.getKind()) {

      case FUNCTION:
        if (isPrivate(element)) {
          return DartPluginImages.DESC_DART_METHOD_PRIVATE;
        }
        if (element.getEnclosingElement().getKind() == ElementKind.COMPILATION_UNIT) {
          return DartPluginImages.DESC_DART_METHOD_PUBLIC;
        } else {
          // If functions defined within methods are displayed in the outline (or elsewhere) then a
          // new icon should be added, since they are private but not defined by the user as such.
          return DartPluginImages.DESC_MISC_DEFAULT;
        }
//      case ???:
//        return getFunctionTypeDescriptor((DartFunctionTypeAlias) element);

      case CONSTRUCTOR:
      case GETTER:
      case SETTER:
      case METHOD:
        return getMethodImageDescriptor(false, isPrivate(element));

      case FIELD:
        return getFieldImageDescriptor(false, isPrivate(element));

      case IMPORT:
        return DartPluginImages.DESC_OBJS_IMPCONT;

      case CLASS:
        return getClassImageDescriptor(isPrivate(element));

      case COMPILATION_UNIT:

        return getCompilationUnitDescriptor((CompilationUnitElement) element);

      case LIBRARY:
        return getLibraryImageDescriptor(renderFlags);

//TODO (pquitslund): projects are not in the new model
//      case DartElement.DART_PROJECT:
//        DartProject project = (DartProject) element;
//        if (project.getProject().isOpen()) {
//          IProject project2 = project.getProject();
//          IWorkbenchAdapter adapter = (IWorkbenchAdapter) project2.getAdapter(IWorkbenchAdapter.class);
//          if (adapter != null) {
//            ImageDescriptor result = adapter.getImageDescriptor(project2);
//            if (result != null) {
//              return result;
//            }
//          }
//          return DESC_OBJ_PROJECT;
//        }
//        return DESC_OBJ_PROJECT_CLOSED;
//
//      case DartElement.DART_MODEL:
//        return DartPluginImages.DESC_OBJS_JAVA_MODEL;

      case LOCAL_VARIABLE:
      case TOP_LEVEL_VARIABLE:
        //TODO (pquitslund): top-level vars should have their own descriptors
        return getFieldImageDescriptor(false, isPrivate(element));

      case FUNCTION_TYPE_ALIAS:
        return DartPluginImages.DESC_DART_CLASS_TYPE_ALIAS;

    }

    return DartPluginImages.DESC_OBJS_GHOST;
  }

  /**
   * Returns an image descriptor for a compilation unit resource. The descriptor includes overlays,
   * if specified.
   */
  public ImageDescriptor getCUResourceImageDescriptor(IFile file, int flags) {
    Point size = useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;
    return new DartElementImageDescriptor(DartPluginImages.DESC_OBJS_CUNIT_RESOURCE, 0, size);
  }

  /**
   * Returns an image descriptor for a Dart element. The descriptor includes overlays, if specified.
   */
  public ImageDescriptor getDartImageDescriptor(Element element, int flags) {
    Point size = useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;
    ImageDescriptor baseDesc = getBaseImageDescriptor(element, flags);
    if (baseDesc != null) {
      if (element instanceof CompilationUnitElement) {

        //TODO (pquitslund): test for read-only-ness
//        CompilationUnitElement cu = (CompilationUnitElement) element;
//        if (cu.isReadOnly()) {
//          baseDesc = decorateReadOnly(baseDesc);
//        }
      }

      int adornmentFlags = computeDecorators(element, flags);
      return new DartElementImageDescriptor(baseDesc, adornmentFlags, size);
    }

    return new DartElementImageDescriptor(DartPluginImages.DESC_OBJS_GHOST, 0, size);
  }

  /**
   * Returns the icon for a given element. The icon depends on the element type and element
   * properties. If configured, overlay icons are constructed for <code>SourceReference</code>s.
   * 
   * @param flags Flags as defined by the DartElementImageProvider
   */
  public Image getImageLabel(Object element, int flags) {
    return getImageLabel(computeDescriptor(element, flags));
  }

  /**
   * Returns an image descriptor for a IAdaptable. The descriptor includes overlays, if specified
   * (only error ticks apply). Returns <code>null</code> if no image could be found.
   */
  public ImageDescriptor getWorkbenchImageDescriptor(IAdaptable adaptable, int flags) {
    IWorkbenchAdapter wbAdapter = (IWorkbenchAdapter) adaptable.getAdapter(IWorkbenchAdapter.class);
    if (wbAdapter == null) {
      return null;
    }
    ImageDescriptor descriptor = wbAdapter.getImageDescriptor(adaptable);
    if (descriptor == null) {
      return null;
    }

    Point size = useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;
    return new DartElementImageDescriptor(descriptor, 0, size);
  }

  private int computeDecorators(Element element, int renderFlags) {

    int flags = 0;

    if (showOverlayIcons(renderFlags)) {

      switch (element.getKind()) {

        case CONSTRUCTOR:
          flags |= DartElementImageDescriptor.CONSTRUCTOR;
          break;

        case GETTER:
          flags |= DartElementImageDescriptor.GETTER;
          break;

        case SETTER:
          flags |= DartElementImageDescriptor.SETTER;
          break;

        case METHOD:
          MethodElement method = (MethodElement) element;
          if (method.isAbstract()) {
            flags |= DartElementImageDescriptor.ABSTRACT;
          }
          if (method.isStatic()) {
            flags |= DartElementImageDescriptor.STATIC;
          }
          break;

        case FIELD:
          FieldElement field = (FieldElement) element;
          if (field.isStatic()) {
            flags |= DartElementImageDescriptor.STATIC;
          }
          if (field.isConst()) {
            flags |= DartElementImageDescriptor.CONST;
          }
          break;
      }

    }

    return flags;
  }

  private ImageDescriptor computeDescriptor(Object element, int flags) {

    if (element instanceof Element) {

      return getDartImageDescriptor((Element) element, flags);

    } else if (element instanceof IFile) {

      IFile file = (IFile) element;
      ImageDescriptor imageDescriptor = getWorkbenchImageDescriptor(file, flags);

      if (file.isReadOnly() || !file.exists()) {
        return decorateReadOnly(imageDescriptor);
      } else {
        return imageDescriptor;
      }

    } else if (element instanceof FileStoreEditorInput) {

      ImageDescriptor imageDescriptor = DartPluginImages.DESC_DART_COMP_UNIT;
      File file = new File(((FileStoreEditorInput) element).getURI());
      imageDescriptor = DartPluginImages.DESC_DART_COMP_UNIT;
      if (!file.canWrite()) {
        return decorateReadOnly(imageDescriptor);
      }
      return imageDescriptor;

    } else if (element instanceof IAdaptable) {

      return getWorkbenchImageDescriptor((IAdaptable) element, flags);

    } else if (element instanceof ImportedDartLibraryContainer
        || element instanceof ImportedDartLibrary) {

      Point size = useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;
      ImageDescriptor baseDesc = getLibraryImageDescriptor(flags);
      if (baseDesc != null) {
        return new DartElementImageDescriptor(baseDesc, 0, size);
      }
      return new DartElementImageDescriptor(DartPluginImages.DESC_OBJS_GHOST, 0, size);

    }

    return null;
  }

  private ImageDescriptor decorateReadOnly(ImageDescriptor imageDescriptor) {
    return decorate(imageDescriptor, DESC_READ_ONLY);
  }

  private ImageDescriptor getCompilationUnitDescriptor(CompilationUnitElement element) {
    return DartPluginImages.DESC_DART_COMP_UNIT;
  }

  private Image getImageLabel(ImageDescriptor descriptor) {
    if (descriptor == null) {
      return null;
    }
    return getRegistry().get(descriptor);
  }

  private ImageDescriptorRegistry getRegistry() {

    if (descriptorRegistry == null) {
      descriptorRegistry = DartToolsPlugin.getImageDescriptorRegistry();
    }

    return descriptorRegistry;
  }

  private boolean isPrivate(Element elem) {
    return Identifier.isPrivateName(elem.getName());
  }

}
