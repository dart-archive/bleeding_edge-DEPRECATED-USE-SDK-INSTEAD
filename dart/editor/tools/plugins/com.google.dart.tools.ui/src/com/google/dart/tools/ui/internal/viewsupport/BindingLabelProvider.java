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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.core.dom.IBinding;
import com.google.dart.core.dom.IFunctionBinding;
import com.google.dart.core.dom.IPackageBinding;
import com.google.dart.core.dom.ITypeBinding;
import com.google.dart.core.dom.IVariableBinding;
import com.google.dart.core.dom.Modifier;
import com.google.dart.tools.ui.DartElementImageDescriptor;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.Messages;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * Label provider to render bindings in viewers.
 */
public class BindingLabelProvider extends LabelProvider {

  public static final long DEFAULT_TEXTFLAGS = DartElementLabels.ALL_DEFAULT;

  public static final int DEFAULT_IMAGEFLAGS = DartElementImageProvider.OVERLAY_ICONS;

  /**
   * Returns the image descriptor for a binding with the flags as defined by
   * {@link DartElementImageProvider}.
   * 
   * @param binding The binding to get the image for.
   * @param imageFlags The image flags as defined in {@link DartElementImageProvider}.
   * @return the image of the binding or null if there is no image
   */
  public static ImageDescriptor getBindingImageDescriptor(IBinding binding, int imageFlags) {
    ImageDescriptor baseImage = getBaseImageDescriptor(binding, imageFlags);
    if (baseImage != null) {
      int adornmentFlags = getAdornmentFlags(binding, imageFlags);
      Point size = ((imageFlags & DartElementImageProvider.SMALL_ICONS) != 0)
          ? DartElementImageProvider.SMALL_SIZE : DartElementImageProvider.BIG_SIZE;
      return new DartElementImageDescriptor(baseImage, adornmentFlags, size);
    }
    return null;
  }

  /**
   * Returns the label for a Java element with the flags as defined by {@link DartElementLabels}.
   * 
   * @param binding The binding to render.
   * @param flags The text flags as defined in {@link DartElementLabels}
   * @return the label of the binding
   */
  public static String getBindingLabel(IBinding binding, long flags) {
    StringBuffer buffer = new StringBuffer(60);
    if (binding instanceof ITypeBinding) {
      getTypeLabel(((ITypeBinding) binding), flags, buffer);
    } else if (binding instanceof IFunctionBinding) {
      getMethodLabel(((IFunctionBinding) binding), flags, buffer);
    } else if (binding instanceof IVariableBinding) {
      final IVariableBinding variable = (IVariableBinding) binding;
      if (variable.isField()) {
        getFieldLabel(variable, flags, buffer);
      } else {
        getLocalVariableLabel(variable, flags, buffer);
      }
    }
    return buffer.toString();
  }

  private static void appendDimensions(int dim, StringBuffer buffer) {
    for (int i = 0; i < dim; i++) {
      buffer.append('[').append(']');
    }
  }

  private static int getAdornmentFlags(IBinding binding, int flags) {
    int adornments = 0;
    if (binding instanceof IFunctionBinding && ((IFunctionBinding) binding).isConstructor()) {
      adornments |= DartElementImageDescriptor.CONSTRUCTOR;
    }
    final int modifiers = binding.getModifiers();
    if (Modifier.isAbstract(modifiers)) {
      adornments |= DartElementImageDescriptor.ABSTRACT;
    }
    if (Modifier.isFinal(modifiers)) {
      adornments |= DartElementImageDescriptor.FINAL;
    }
    if (Modifier.isSynchronized(modifiers)) {
      adornments |= DartElementImageDescriptor.SYNCHRONIZED;
    }
    if (Modifier.isStatic(modifiers)) {
      adornments |= DartElementImageDescriptor.STATIC;
    }
    if (binding instanceof IVariableBinding && ((IVariableBinding) binding).isField()) {
      if (Modifier.isTransient(modifiers)) {
        adornments |= DartElementImageDescriptor.TRANSIENT;
      }
      if (Modifier.isVolatile(modifiers)) {
        adornments |= DartElementImageDescriptor.VOLATILE;
      }
    }
    return adornments;
  }

  private static ImageDescriptor getBaseImageDescriptor(IBinding binding, int flags) {
    if (binding instanceof ITypeBinding) {
      ITypeBinding typeBinding = (ITypeBinding) binding;
      if (typeBinding.isArray()) {
        typeBinding = typeBinding.getElementType();
      }
      return getTypeImageDescriptor(typeBinding.getDeclaringClass() != null, typeBinding, flags);
    } else if (binding instanceof IFunctionBinding) {
      return getMethodImageDescriptor(binding.getModifiers());
    } else if (binding instanceof IVariableBinding) {
      return getFieldImageDescriptor((IVariableBinding) binding);
    }
    return DartPluginImages.DESC_OBJS_UNKNOWN;
  }

//	private static ImageDescriptor getInnerInterfaceImageDescriptor(int modifiers) {
//		if (Modifier.isPublic(modifiers))
//			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PUBLIC;
//		else if (Modifier.isPrivate(modifiers))
//			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PRIVATE;
//		else if (Modifier.isProtected(modifiers))
//			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PROTECTED;
//		else
//			return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
//	}
//
//	private static ImageDescriptor getInterfaceImageDescriptor(int modifiers) {
//		if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers))
//			return JavaPluginImages.DESC_OBJS_INTERFACE;
//		else
//			return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
//	}

  private static ImageDescriptor getClassImageDescriptor(int modifiers) {
    if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)
        || Modifier.isPrivate(modifiers)) {
      return DartPluginImages.DESC_DART_CLASS_PUBLIC;
    } else {
      return DartPluginImages.DESC_OBJS_CLASS_DEFAULT;
    }
  }

  private static ImageDescriptor getFieldImageDescriptor(IVariableBinding binding) {
    final int modifiers = binding.getModifiers();
    if (Modifier.isPublic(modifiers)) {
      return DartPluginImages.DESC_DART_FIELD_PUBLIC;
    }
    if (Modifier.isProtected(modifiers)) {
      return DartPluginImages.DESC_FIELD_PROTECTED;
    }
    if (Modifier.isPrivate(modifiers)) {
      return DartPluginImages.DESC_DART_FIELD_PRIVATE;
    }

    return DartPluginImages.DESC_DART_FIELD_PUBLIC;
  }

  private static void getFieldLabel(IVariableBinding binding, long flags, StringBuffer buffer) {
    if (((flags & DartElementLabels.F_PRE_TYPE_SIGNATURE) != 0)) {
      getTypeLabel(binding.getType(), (flags & DartElementLabels.T_TYPE_PARAMETERS), buffer);
      buffer.append(' ');
    }
    // qualification

    if ((flags & DartElementLabels.F_FULLY_QUALIFIED) != 0) {
      ITypeBinding declaringClass = binding.getDeclaringClass();
      if (declaringClass != null) { // test for array.length
        getTypeLabel(declaringClass, DartElementLabels.T_FULLY_QUALIFIED
            | (flags & DartElementLabels.P_COMPRESSED), buffer);
        buffer.append('.');
      }
    }
    buffer.append(binding.getName());
    if (((flags & DartElementLabels.F_APP_TYPE_SIGNATURE) != 0)) {
      buffer.append(DartElementLabels.DECL_STRING);
      getTypeLabel(binding.getType(), (flags & DartElementLabels.T_TYPE_PARAMETERS), buffer);
    }
    // post qualification
    if ((flags & DartElementLabels.F_POST_QUALIFIED) != 0) {
      ITypeBinding declaringClass = binding.getDeclaringClass();
      if (declaringClass != null) { // test for array.length
        buffer.append(DartElementLabels.CONCAT_STRING);
        getTypeLabel(declaringClass, DartElementLabels.T_FULLY_QUALIFIED
            | (flags & DartElementLabels.P_COMPRESSED), buffer);
      }
    }
  }

  private static ImageDescriptor getInnerClassImageDescriptor(int modifiers) {
    if (Modifier.isPublic(modifiers)) {
      return DartPluginImages.DESC_OBJS_INNER_CLASS_PUBLIC;
    } else if (Modifier.isPrivate(modifiers)) {
      return DartPluginImages.DESC_OBJS_INNER_CLASS_PRIVATE;
    } else if (Modifier.isProtected(modifiers)) {
      return DartPluginImages.DESC_OBJS_INNER_CLASS_PROTECTED;
    } else {
      return DartPluginImages.DESC_OBJS_INNER_CLASS_DEFAULT;
    }
  }

  private static void getLocalVariableLabel(IVariableBinding binding, long flags,
      StringBuffer buffer) {
    if (((flags & DartElementLabels.F_PRE_TYPE_SIGNATURE) != 0)) {
      getTypeLabel(binding.getType(), (flags & DartElementLabels.T_TYPE_PARAMETERS), buffer);
      buffer.append(' ');
    }
    if (((flags & DartElementLabels.F_FULLY_QUALIFIED) != 0)) {
      IFunctionBinding declaringMethod = binding.getDeclaringMethod();
      if (declaringMethod != null) {
        getMethodLabel(declaringMethod, flags, buffer);
        buffer.append('.');
      }
    }
    buffer.append(binding.getName());
    if (((flags & DartElementLabels.F_APP_TYPE_SIGNATURE) != 0)) {
      buffer.append(DartElementLabels.DECL_STRING);
      getTypeLabel(binding.getType(), (flags & DartElementLabels.T_TYPE_PARAMETERS), buffer);
    }
  }

//	private static void getTypeArgumentsLabel(ITypeBinding[] typeArgs, long flags, StringBuffer buf) {
//		if (typeArgs.length > 0) {
//			buf.append('<');
//			for (int i = 0; i < typeArgs.length; i++) {
//				if (i > 0) {
//					buf.append(DartElementLabels.COMMA_STRING);
//				}
//				getTypeLabel(typeArgs[i], flags & DartElementLabels.T_TYPE_PARAMETERS, buf);
//			}
//			buf.append('>');
//		}
//	}

  private static ImageDescriptor getMethodImageDescriptor(int modifiers) {
    if (Modifier.isPublic(modifiers)) {
      return DartPluginImages.DESC_DART_METHOD_PUBLIC;
    }
    if (Modifier.isProtected(modifiers)) {
      return DartPluginImages.DESC_MISC_PROTECTED;
    }
    if (Modifier.isPrivate(modifiers)) {
      return DartPluginImages.DESC_DART_METHOD_PRIVATE;
    }

    return DartPluginImages.DESC_DART_METHOD_PUBLIC;
  }

  private static void getMethodLabel(IFunctionBinding binding, long flags, StringBuffer buffer) {
    // return type
    if (((flags & DartElementLabels.M_PRE_RETURNTYPE) != 0) && !binding.isConstructor()) {
      getTypeLabel(binding.getReturnType(), (flags & DartElementLabels.T_TYPE_PARAMETERS), buffer);
      buffer.append(' ');
    }
    // qualification
    if ((flags & DartElementLabels.M_FULLY_QUALIFIED) != 0) {
      getTypeLabel(binding.getDeclaringClass(), DartElementLabels.T_FULLY_QUALIFIED
          | (flags & DartElementLabels.P_COMPRESSED), buffer);
      buffer.append('.');
    }
    buffer.append(binding.getName());

    // parameters
    buffer.append('(');
    if ((flags & DartElementLabels.M_PARAMETER_TYPES | DartElementLabels.M_PARAMETER_NAMES) != 0) {
      ITypeBinding[] parameters = ((flags & DartElementLabels.M_PARAMETER_TYPES) != 0)
          ? binding.getParameterTypes() : null;
      if (parameters != null) {
        for (int index = 0; index < parameters.length; index++) {
          if (index > 0) {
            buffer.append(DartElementLabels.COMMA_STRING);
          }
          ITypeBinding paramType = parameters[index];
          if (binding.isVarargs() && (index == parameters.length - 1)) {
            getTypeLabel(paramType.getElementType(), (flags & DartElementLabels.T_TYPE_PARAMETERS),
                buffer);
            appendDimensions(paramType.getDimensions() - 1, buffer);
            buffer.append(DartElementLabels.ELLIPSIS_STRING);
          } else {
            getTypeLabel(paramType, (flags & DartElementLabels.T_TYPE_PARAMETERS), buffer);
          }
        }
      }
    } else {
      if (binding.getParameterTypes().length > 0) {
        buffer.append(DartElementLabels.ELLIPSIS_STRING);
      }
    }
    buffer.append(')');

    if (((flags & DartElementLabels.M_APP_RETURNTYPE) != 0) && !binding.isConstructor()) {
      buffer.append(DartElementLabels.DECL_STRING);
      getTypeLabel(binding.getReturnType(), (flags & DartElementLabels.T_TYPE_PARAMETERS), buffer);
    }
    // post qualification
    if ((flags & DartElementLabels.M_POST_QUALIFIED) != 0) {
      buffer.append(DartElementLabels.CONCAT_STRING);
      getTypeLabel(binding.getDeclaringClass(), DartElementLabels.T_FULLY_QUALIFIED
          | (flags & DartElementLabels.P_COMPRESSED), buffer);
    }
  }

  private static ImageDescriptor getTypeImageDescriptor(boolean inner, ITypeBinding binding,
      int flags) {
    if (binding.isClass()) {
      if (inner) {
        return getInnerClassImageDescriptor(binding.getModifiers());
      }
      return getClassImageDescriptor(binding.getModifiers());
    }
    // primitive type, wildcard
    return null;
  }

  private static void getTypeLabel(ITypeBinding binding, long flags, StringBuffer buffer) {
    if ((flags & DartElementLabels.T_FULLY_QUALIFIED) != 0) {
      final IPackageBinding pack = binding.getPackage();
      if (pack != null && !pack.isUnnamed()) {
        buffer.append(pack.getName());
        buffer.append('.');
      }
    }
    if ((flags & (DartElementLabels.T_FULLY_QUALIFIED | DartElementLabels.T_CONTAINER_QUALIFIED)) != 0) {
      final ITypeBinding declaring = binding.getDeclaringClass();
      if (declaring != null) {
        getTypeLabel(declaring, DartElementLabels.T_CONTAINER_QUALIFIED
            | (flags & DartElementLabels.P_COMPRESSED), buffer);
        buffer.append('.');
      }
      final IFunctionBinding declaringMethod = binding.getDeclaringMethod();
      if (declaringMethod != null) {
        getMethodLabel(declaringMethod, 0, buffer);
        buffer.append('.');
      }
    }

    if (binding.isArray()) {
      getTypeLabel(binding.getElementType(), flags & DartElementLabels.T_TYPE_PARAMETERS, buffer);
      appendDimensions(binding.getDimensions(), buffer);
    } else { // type variables, primitive, reftype
      String name = binding.getTypeDeclaration().getName();
      if (name.length() == 0) {
        if (binding.isAnonymous()) {
          ITypeBinding baseType = binding.getSuperclass();

          if (baseType != null) {
            StringBuffer anonymBaseType = new StringBuffer();
            getTypeLabel(baseType, flags & DartElementLabels.T_TYPE_PARAMETERS, anonymBaseType);
            buffer.append(Messages.format(DartUIMessages.JavaElementLabels_anonym_type,
                anonymBaseType.toString()));
          } else {
            buffer.append(DartUIMessages.JavaElementLabels_anonym);
          }
        } else {
          buffer.append("UNKNOWN"); //$NON-NLS-1$
        }
      } else {
        buffer.append(name);
      }
    }

    if ((flags & DartElementLabels.T_POST_QUALIFIED) != 0) {
      final IFunctionBinding declaringMethod = binding.getDeclaringMethod();
      final ITypeBinding declaringType = binding.getDeclaringClass();
      if (declaringMethod != null) {
        buffer.append(DartElementLabels.CONCAT_STRING);
        getMethodLabel(declaringMethod, DartElementLabels.T_FULLY_QUALIFIED
            | (flags & DartElementLabels.P_COMPRESSED), buffer);
      } else if (declaringType != null) {
        buffer.append(DartElementLabels.CONCAT_STRING);
        getTypeLabel(declaringType, DartElementLabels.T_FULLY_QUALIFIED
            | (flags & DartElementLabels.P_COMPRESSED), buffer);
      } else {
        final IPackageBinding pack = binding.getPackage();
        if (pack != null && !pack.isUnnamed()) {
          buffer.append(DartElementLabels.CONCAT_STRING);
          buffer.append(pack.getName());
        }
      }
    }
  }

  final private long fTextFlags;
  final private int fImageFlags;

  private ImageDescriptorRegistry fRegistry;

  /**
   * Creates a new binding label provider with default text and image flags
   */
  public BindingLabelProvider() {
    this(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS);
  }

  /**
   * @param textFlags Flags defined in {@link DartElementLabels}.
   * @param imageFlags Flags defined in {@link DartElementImageProvider}.
   */
  public BindingLabelProvider(final long textFlags, final int imageFlags) {
    fImageFlags = imageFlags;
    fTextFlags = textFlags;
    fRegistry = null;
  }

  /*
   * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
   */
  @Override
  public Image getImage(Object element) {
    if (element instanceof IBinding) {
      ImageDescriptor baseImage = getBindingImageDescriptor((IBinding) element, fImageFlags);
      if (baseImage != null) {
        return getRegistry().get(baseImage);
      }
    }
    return super.getImage(element);
  }

  /*
   * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
   */
  @Override
  public String getText(Object element) {
    if (element instanceof IBinding) {
      return getBindingLabel((IBinding) element, fTextFlags);
    }
    return super.getText(element);
  }

  private ImageDescriptorRegistry getRegistry() {
    if (fRegistry == null) {
      fRegistry = DartToolsPlugin.getImageDescriptorRegistry();
    }
    return fRegistry;
  }
}
