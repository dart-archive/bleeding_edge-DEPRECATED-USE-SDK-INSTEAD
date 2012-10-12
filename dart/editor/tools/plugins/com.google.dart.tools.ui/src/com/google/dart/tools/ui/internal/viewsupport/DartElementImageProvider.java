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
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartElementImageDescriptor;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.ImportedDartLibrary;
import com.google.dart.tools.ui.ImportedDartLibraryContainer;
import com.google.dart.tools.ui.internal.DartWorkbenchAdapter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;

import java.io.File;

/**
 * Default strategy of the Java plugin for the construction of Java element icons.
 */
public class DartElementImageProvider {

  /**
   * Flags for the JavaImageLabelProvider: Generate images with overlays.
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

  private static ImageDescriptor DESC_READ_ONLY;

  private static ImageDescriptor DESC_LAUNCHABLE;
  private static ImageDescriptor DESC_MAIN_TYPE;

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

  public static ImageDescriptor getTypeImageDescriptor(boolean isInterface, boolean isPrivate) {
    if (isInterface) {
      return getInterfaceImageDescriptor(isPrivate);
    } else {
      return getClassImageDescriptor(isPrivate);
    }
  }

  private static boolean confirmAbstract(TypeMember element) throws DartModelException {
    // never show the abstract symbol on interfaces or members in interfaces
    if (element.getElementType() == DartElement.TYPE) {
      return true;
    }
    return true;
  }

  private static ImageDescriptor decorate(ImageDescriptor main, ImageDescriptor badge) {
    return new DecorationOverlayIcon(
        DartToolsPlugin.getImageDescriptorRegistry().get(main),
        badge,
        IDecoration.BOTTOM_RIGHT);
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

  private static boolean showOverlayIcons(int flags) {
    return (flags & OVERLAY_ICONS) != 0;
  }

  private static boolean useSmallSize(int flags) {
    return (flags & SMALL_ICONS) != 0;
  }

  {
    ISharedImages images = DartToolsPlugin.getDefault().getWorkbench().getSharedImages();
    DESC_OBJ_PROJECT_CLOSED = images.getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED);
    DESC_OBJ_PROJECT = images.getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT);
    DESC_READ_ONLY = DartToolsPlugin.getImageDescriptor("icons/full/ovr16/lock_ovr.png");
    DESC_LAUNCHABLE = DartToolsPlugin.getImageDescriptor("icons/full/ovr16/run_co.gif");
    DESC_MAIN_TYPE = DartToolsPlugin.getImageDescriptor("icons/full/ovr16/owned_ovr.gif");
  }

  private ImageDescriptorRegistry fRegistry;

  public DartElementImageProvider() {
    fRegistry = null; // lazy initialization
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
      case DartElement.HTML_FILE: {
        return decorate(DartPluginImages.DESC_DART_HTML_FILE, DESC_LAUNCHABLE);
      }
//        case DartElement.LOCAL_VARIABLE:
//          return JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE;

      case DartElement.IMPORT_CONTAINER:
        return DartPluginImages.DESC_OBJS_IMPCONT;

      case DartElement.TYPE: {
        Type type = (Type) element;

        boolean isInterface;
        try {
          isInterface = type.isInterface();
        } catch (DartModelException e) {
          isInterface = false;
        }
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
      default:
        // ignore. Must be a new, yet unknown element
        // give an advanced IWorkbenchAdapter the chance
        IWorkbenchAdapter wbAdapter = (IWorkbenchAdapter) element.getAdapter(IWorkbenchAdapter.class);
        if (wbAdapter != null && !(wbAdapter instanceof DartWorkbenchAdapter)) { // avoid
                                                                                 // recursion
          ImageDescriptor imageDescriptor = wbAdapter.getImageDescriptor(element);
          if (imageDescriptor != null) {
            return imageDescriptor;
          }
        } else {
          return DartPluginImages.DESC_OBJS_GHOST;
        }
    }

    return DartPluginImages.DESC_OBJS_GHOST;
  }

  /**
   * Returns an image descriptor for a compilation unit not on the class path. The descriptor
   * includes overlays, if specified.
   */
  public ImageDescriptor getCUResourceImageDescriptor(IFile file, int flags) {
    Point size = useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;
    return new DartElementImageDescriptor(DartPluginImages.DESC_OBJS_CUNIT_RESOURCE, 0, size);
  }

  /**
   * Returns an image descriptor for a Dart element. The descriptor includes overlays, if specified.
   */
  public ImageDescriptor getDartImageDescriptor(DartElement element, int flags) {
    Point size = useSmallSize(flags) ? SMALL_SIZE : BIG_SIZE;
    ImageDescriptor baseDesc = getBaseImageDescriptor(element, flags);
    if (baseDesc != null) {
      if (element instanceof CompilationUnit) {
        CompilationUnit cu = (CompilationUnit) element;

        if (cu.isReadOnly()) {
          baseDesc = decorateReadOnly(baseDesc);
        }
      }

      int adornmentFlags = computeJavaAdornmentFlags(element, flags);
      return new DartElementImageDescriptor(baseDesc, adornmentFlags, size);
    }

    return new DartElementImageDescriptor(DartPluginImages.DESC_OBJS_GHOST, 0, size);
  }

  /**
   * Returns the icon for a given element. The icon depends on the element type and element
   * properties. If configured, overlay icons are constructed for <code>SourceReference</code>s.
   * 
   * @param flags Flags as defined by the JavaImageLabelProvider
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

  // private static boolean isDefaultFlag(int flags) {
  // return !Flags.isPublic(flags) && !Flags.isProtected(flags) &&
  // !Flags.isPrivate(flags);
  // }
  //
  protected ImageDescriptor getPackageFragmentIcon(DartElement element, int renderFlags)
      throws DartModelException {
    // IPackageFragment fragment= (IPackageFragment)element;
    // boolean containsJavaElements= false;
    // try {
    // containsJavaElements= fragment.hasChildren();
    // } catch(DartModelException e) {
    // // assuming no children;
    // }
    // if(!containsJavaElements && (fragment.getNonJavaResources().length > 0))
    // return JavaPluginImages.DESC_OBJS_EMPTY_PACKAGE_RESOURCES;
    // else if (!containsJavaElements)
    // return JavaPluginImages.DESC_OBJS_EMPTY_PACKAGE;
    return DartPluginImages.DESC_OBJS_PACKAGE;
  }

  private ImageDescriptor computeDescriptor(Object element, int flags) {
    if (element instanceof DartElement) {
      return getDartImageDescriptor((DartElement) element, flags);
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

  private int computeJavaAdornmentFlags(DartElement element, int renderFlags) {
    int flags = 0;
    if (showOverlayIcons(renderFlags) && element instanceof TypeMember) {
      try {
        TypeMember member = (TypeMember) element;

        if (element.getElementType() == DartElement.METHOD && ((Method) element).isConstructor()) {
          flags |= DartElementImageDescriptor.CONSTRUCTOR;
        }

        if (element.getElementType() == DartElement.METHOD && ((Method) element).isGetter()) {
          flags |= DartElementImageDescriptor.GETTER;
        }

        if (element.getElementType() == DartElement.METHOD && ((Method) element).isSetter()) {
          flags |= DartElementImageDescriptor.SETTER;
        }

        if (element.getElementType() == DartElement.METHOD && ((Method) member).isAbstract()
            && confirmAbstract(member)) {
          flags |= DartElementImageDescriptor.ABSTRACT;
        }

        if (member.isStatic()) {
          flags |= DartElementImageDescriptor.STATIC;
        }

        if (element.getElementType() == DartElement.FIELD && ((Field) member).isConstant()) {
          flags |= DartElementImageDescriptor.CONST;
        }
      } catch (DartModelException e) {
        // do nothing. Can't compute runnable adornment or get flags
      }
    }
    return flags;
  }

  private ImageDescriptor decorateReadOnly(ImageDescriptor imageDescriptor) {
    return decorate(imageDescriptor, DESC_READ_ONLY);
  }

  private ImageDescriptor getCompilationUnitDescriptor(CompilationUnit element) {
    boolean hasMain;

    try {
      hasMain = element.hasMain();
    } catch (DartModelException e) {
      hasMain = false;
    }

    if (hasMain) {
      return decorate(DartPluginImages.DESC_DART_COMP_UNIT, DESC_MAIN_TYPE);
    } else {
      return DartPluginImages.DESC_DART_COMP_UNIT;
    }
  }

  private Image getImageLabel(ImageDescriptor descriptor) {
    if (descriptor == null) {
      return null;
    }
    return getRegistry().get(descriptor);
  }

  private ImageDescriptorRegistry getRegistry() {
    if (fRegistry == null) {
      fRegistry = DartToolsPlugin.getImageDescriptorRegistry();
    }
    return fRegistry;
  }
}
