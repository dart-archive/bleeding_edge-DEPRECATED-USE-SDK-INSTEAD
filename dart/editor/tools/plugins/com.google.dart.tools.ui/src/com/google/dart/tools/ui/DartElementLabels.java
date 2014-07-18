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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.internal.util.Strings;
import com.google.dart.tools.ui.internal.viewsupport.DartElementLabelComposer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * <code>DartElementLabels</code> provides helper methods to render names of JavaScript elements.
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class DartElementLabels {

  /**
   * Method names contain parameter types. e.g. <code>foo(int)</code>
   */
  public final static long M_PARAMETER_TYPES = 1L << 0;

  /**
   * Method names contain parameter names. e.g. <code>foo(index)</code>
   */
  public final static long M_PARAMETER_NAMES = 1L << 1;

  /**
   * Method names contain type parameters prepended. e.g. <code>&lt;A&gt; foo(A index)</code>
   */
  public final static long M_PRE_TYPE_PARAMETERS = 1L << 2;

  /**
   * Method names contain type parameters appended. e.g. <code>foo(A index) &lt;A&gt;</code>
   */
  public final static long M_APP_TYPE_PARAMETERS = 1L << 3;

  /**
   * Method names contain thrown exceptions. e.g. <code>foo throws IOException</code>
   */
  public final static long M_EXCEPTIONS = 1L << 4;

  /**
   * Method names contain return type (appended) e.g. <code>foo : int</code>
   */
  public final static long M_APP_RETURNTYPE = 1L << 5;

  /**
   * Method names contain return type (appended) e.g. <code>int foo</code>
   */
  public final static long M_PRE_RETURNTYPE = 1L << 6;

  /**
   * Method names are fully qualified. e.g. <code>java.util.Vector.size</code>
   */
  public final static long M_FULLY_QUALIFIED = 1L << 7;

  /**
   * Method names are post qualified. e.g. <code>size - java.util.Vector</code>
   */
  public final static long M_POST_QUALIFIED = 1L << 8;

  /**
   * Initializer names are fully qualified. e.g. <code>java.util.Vector.{ ... }</code>
   */
  public final static long I_FULLY_QUALIFIED = 1L << 10;

  /**
   * Type names are post qualified. e.g. <code>{ ... } - java.util.Map</code>
   */
  public final static long I_POST_QUALIFIED = 1L << 11;

  /**
   * Field names contain the declared type (appended) e.g. <code>fHello : int</code>
   */
  public final static long F_APP_TYPE_SIGNATURE = 1L << 14;

  /**
   * Field names contain the declared type (prepended) e.g. <code>int fHello</code>
   */
  public final static long F_PRE_TYPE_SIGNATURE = 1L << 15;

  /**
   * Fields names are fully qualified. e.g. <code>java.lang.System.out</code>
   */
  public final static long F_FULLY_QUALIFIED = 1L << 16;

  /**
   * Fields names are post qualified. e.g. <code>out - java.lang.System</code>
   */
  public final static long F_POST_QUALIFIED = 1L << 17;

  /**
   * Type names are fully qualified. e.g. <code>java.util.Map.MapEntry</code>
   */
  public final static long T_FULLY_QUALIFIED = 1L << 18;

  /**
   * Type names are type container qualified. e.g. <code>Map.MapEntry</code>
   */
  public final static long T_CONTAINER_QUALIFIED = 1L << 19;

  /**
   * Type names are post qualified. e.g. <code>MapEntry - java.util.Map</code>
   */
  public final static long T_POST_QUALIFIED = 1L << 20;

  /**
   * Type names contain type parameters. e.g. <code>Map&lt;S, T&gt;</code>
   */
  public final static long T_TYPE_PARAMETERS = 1L << 21;

  /**
   * Declarations (import container / declaration, package declaration) are qualified. e.g.
   * <code>java.util.Vector.class/import container</code>
   */
  public final static long D_QUALIFIED = 1L << 24;

  /**
   * Declarations (import container / declaration, package declaration) are post qualified. e.g.
   * <code>import container - java.util.Vector.class</code>
   */
  public final static long D_POST_QUALIFIED = 1L << 25;

  /**
   * Class file names are fully qualified. e.g. <code>java.util.Vector.class</code>
   */
  public final static long CF_QUALIFIED = 1L << 27;

  /**
   * Class file names are post qualified. e.g. <code>Vector.class - java.util</code>
   */
  public final static long CF_POST_QUALIFIED = 1L << 28;

  /**
   * Compilation unit names are fully qualified. e.g. <code>java.util.Vector.java</code>
   */
  public final static long CU_QUALIFIED = 1L << 31;

  /**
   * Compilation unit names are post qualified. e.g. <code>Vector.JavaScript - java.util</code>
   */
  public final static long CU_POST_QUALIFIED = 1L << 32;

  /**
   * Package names are qualified. e.g. <code>MyProject/src/java.util</code>
   */
  public final static long P_QUALIFIED = 1L << 35;

  /**
   * Package names are post qualified. e.g. <code>java.util - MyProject/src</code>
   */
  public final static long P_POST_QUALIFIED = 1L << 36;

  /**
   * Package names are compressed. e.g. <code>o*.e*.search</code>
   */
  public final static long P_COMPRESSED = 1L << 37;

  /**
   * Package Fragment Roots contain variable name if from a variable. e.g.
   * <code>JRE_LIB - c:\java\lib\rt.jar</code>
   */
  public final static long ROOT_VARIABLE = 1L << 40;

  /**
   * Package Fragment Roots contain the project name if not an archive (prepended). e.g.
   * <code>MyProject/src</code>
   */
  public final static long ROOT_QUALIFIED = 1L << 41;

  /**
   * Package Fragment Roots contain the project name if not an archive (appended). e.g.
   * <code>src - MyProject</code>
   */
  public final static long ROOT_POST_QUALIFIED = 1L << 42;

  /**
   * Add root path to all elements except Package Fragment Roots and JavaScript projects. e.g.
   * <code>java.lang.Vector - c:\java\lib\rt.jar</code> Option only applies to getElementLabel
   */
  public final static long APPEND_ROOT_PATH = 1L << 43;

  /**
   * Add root path to all elements except Package Fragment Roots and JavaScript projects. e.g.
   * <code>java.lang.Vector - c:\java\lib\rt.jar</code> Option only applies to getElementLabel
   */
  public final static long PREPEND_ROOT_PATH = 1L << 44;

  /**
   * Post qualify referenced package fragment roots. For example
   * <code>jdt.jar - com.google.dart.tools.ui</code> if the jar is referenced from another project.
   */
  public final static long REFERENCED_ROOT_POST_QUALIFIED = 1L << 45;

  /**
   * Specified to use the resolved information of a Type, IFunction or IField. See
   * {@link Type#isResolved()}. If resolved information is available, types will be rendered with
   * type parameters of the instantiated type. Resolved method render with the parameter types of
   * the method instance. <code>Vector&lt;String&gt;.get(String)</code>
   */
  public final static long USE_RESOLVED = 1L << 48;

  /**
   * Prepend first category (if any) to field.
   */
  public final static long F_CATEGORY = 1L << 49;
  /**
   * Prepend first category (if any) to method.
   */
  public final static long M_CATEGORY = 1L << 50;
  /**
   * Prepend first category (if any) to type.
   */
  public final static long T_CATEGORY = 1L << 51;

  /**
   * @deprecated no longer used
   */
  @Deprecated
  public final static long SHOW_TYPE = 1L << 52;

  /**
   * Show category for all elements.
   */
  public final static long ALL_CATEGORY = new Long(DartElementLabels.F_CATEGORY
      | DartElementLabels.M_CATEGORY | DartElementLabels.T_CATEGORY).longValue();

  /**
   * Qualify all elements
   */
  public final static long ALL_FULLY_QUALIFIED = new Long(F_FULLY_QUALIFIED | M_FULLY_QUALIFIED
      | I_FULLY_QUALIFIED | T_FULLY_QUALIFIED | D_QUALIFIED | CF_QUALIFIED | CU_QUALIFIED
      | P_QUALIFIED | ROOT_QUALIFIED).longValue();

  /**
   * Post qualify all elements
   */
  public final static long ALL_POST_QUALIFIED = new Long(F_POST_QUALIFIED | M_POST_QUALIFIED
      | I_POST_QUALIFIED | T_POST_QUALIFIED | D_POST_QUALIFIED | CF_POST_QUALIFIED
      | CU_POST_QUALIFIED | P_POST_QUALIFIED | ROOT_POST_QUALIFIED).longValue();

  /**
   * Default options (M_PARAMETER_TYPES, M_APP_TYPE_PARAMETERS & T_TYPE_PARAMETERS enabled)
   */
  public final static long ALL_DEFAULT = new Long(M_PARAMETER_TYPES | M_APP_TYPE_PARAMETERS
      | T_TYPE_PARAMETERS).longValue();

  /**
   * Default qualify options (All except Root and Package)
   */
  public final static long DEFAULT_QUALIFIED = new Long(F_FULLY_QUALIFIED | M_FULLY_QUALIFIED
      | I_FULLY_QUALIFIED | T_FULLY_QUALIFIED | D_QUALIFIED | CF_QUALIFIED | CU_QUALIFIED).longValue();

  /**
   * Default post qualify options (All except Root and Package)
   */
  public final static long DEFAULT_POST_QUALIFIED = new Long(F_POST_QUALIFIED | M_POST_QUALIFIED
      | I_POST_QUALIFIED | T_POST_QUALIFIED | D_POST_QUALIFIED | CF_POST_QUALIFIED
      | CU_POST_QUALIFIED).longValue();

  /**
   * Specifies to apply color styles to labels. This flag only applies to methods taking or
   * returning a {@link StyledString}.
   */
  public final static long COLORIZE = 1L << 55;

  /**
   * User-readable string for separating post qualified names (e.g. " - ").
   */
  public final static String CONCAT_STRING = DartUIMessages.JavaElementLabels_concat_string;
  /**
   * User-readable string for separating list items (e.g. ", ").
   */
  public final static String COMMA_STRING = DartUIMessages.JavaElementLabels_comma_string;
  /**
   * User-readable string for separating the return type (e.g. " : ").
   */
  public final static String DECL_STRING = DartUIMessages.JavaElementLabels_declseparator_string;
  /**
   * User-readable string for concatenating categories (e.g. " "). XXX: to be made API post 3.2
   */
  @SuppressWarnings("unused")
  private final static String CATEGORY_SEPARATOR_STRING = DartUIMessages.JavaElementLabels_category_separator_string;
  /**
   * User-readable string for ellipsis ("...").
   */
  public final static String ELLIPSIS_STRING = "..."; //$NON-NLS-1$
  /**
   * User-readable string for the default package name (e.g. "(default package)").
   */
  public final static String DEFAULT_PACKAGE = DartUIMessages.JavaElementLabels_default_package;

  private final static long QUALIFIER_FLAGS = P_COMPRESSED | USE_RESOLVED;

//  /**
//   * Appends the label for a class file to a {@link StringBuffer}. Considers the
//   * CF_* flags.
//   * 
//   * @param classFile The element to render.
//   * @param flags The rendering flags. Flags with names starting with 'CF_' are
//   *          considered.
//   * @param buf The buffer to append the resulting label to.
//   */
//  public static void getClassFileLabel(IClassFile classFile, long flags,
//      StringBuffer buf) {
//    if (getFlag(flags, CF_QUALIFIED)) {
//      IPackageFragment pack = (IPackageFragment) classFile.getParent();
//      if (!pack.isDefaultPackage()) {
//        getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), buf);
//        buf.append('.');
//      }
//    }
//    buf.append(classFile.getDisplayName());
//
//    if (getFlag(flags, CF_POST_QUALIFIED)) {
//      buf.append(CONCAT_STRING);
//      getPackageFragmentLabel((IPackageFragment) classFile.getParent(), flags
//          & QUALIFIER_FLAGS, buf);
//    }
//  }

  /**
   * Appends the label for a compilation unit to a {@link StringBuffer}. Considers the CU_* flags.
   * 
   * @param cu The element to render.
   * @param flags The rendering flags. Flags with names starting with 'CU_' are considered.
   * @param buf The buffer to append the resulting label to.
   */
  public static void getCompilationUnitLabel(CompilationUnit cu, long flags, StringBuffer buf) {
//    if (getFlag(flags, CU_QUALIFIED)) {
//      IPackageFragment pack = (IPackageFragment) cu.getParent();
//      if (!pack.isDefaultPackage()) {
//        getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), buf);
//        buf.append('.');
//      }
//    }
    buf.append(cu.getElementName());

//    if (getFlag(flags, CU_POST_QUALIFIED)) {
//      buf.append(CONCAT_STRING);
//      getPackageFragmentLabel((IPackageFragment) cu.getParent(), flags
//          & QUALIFIER_FLAGS, buf);
//    }
  }

  /**
   * Appends the label for a import container, import or package declaration to a
   * {@link StringBuffer}. Considers the D_* flags.
   * 
   * @param declaration The element to render.
   * @param flags The rendering flags. Flags with names starting with 'D_' are considered.
   * @param buf The buffer to append the resulting label to.
   */
  public static void getDeclarationLabel(DartElement declaration, long flags, StringBuffer buf) {
    if (getFlag(flags, D_QUALIFIED)) {
      DartElement openable = declaration.getOpenable();
      if (openable != null) {
        buf.append(getElementLabel(openable, CF_QUALIFIED | CU_QUALIFIED | flags & QUALIFIER_FLAGS));
        buf.append('/');
      }
    }
    if (declaration.getElementType() == DartElement.IMPORT_CONTAINER) {
      buf.append(DartUIMessages.JavaElementLabels_import_container);
    } else {
      buf.append(declaration.getElementName());
    }
    // post qualification
    if (getFlag(flags, D_POST_QUALIFIED)) {
      DartElement openable = declaration.getOpenable();
      if (openable != null) {
        buf.append(CONCAT_STRING);
        buf.append(getElementLabel(openable, CF_QUALIFIED | CU_QUALIFIED | flags & QUALIFIER_FLAGS));
      }
    }
  }

  /**
   * Returns the label for a JavaScript element with the flags as defined by this class.
   * 
   * @param element The element to render.
   * @param flags The rendering flags.
   * @return the label of the JavaScript element
   */
  public static String getElementLabel(DartElement element, long flags) {
    StringBuffer buf = new StringBuffer(60);
    getElementLabel(element, flags, buf);
    return buf.toString();
  }

  /**
   * Returns the label for a JavaScript element with the flags as defined by this class.
   * 
   * @param element The element to render.
   * @param flags The rendering flags.
   * @param buf The buffer to append the resulting label to.
   */
  public static void getElementLabel(DartElement element, long flags, StringBuffer buf) {
    int type = element.getElementType();
    DartX.todo();
//    IPackageFragmentRoot root = null;
//    if (type != DartElement.JAVASCRIPT_MODEL
//        && type != DartElement.JAVASCRIPT_PROJECT
//        && type != DartElement.PACKAGE_FRAGMENT_ROOT)
//      root = DartModelUtil.getPackageFragmentRoot(element);
//    if (root != null && getFlag(flags, PREPEND_ROOT_PATH)) {
//      getPackageFragmentRootLabel(root, ROOT_QUALIFIED, buf);
//      buf.append(CONCAT_STRING);
//    }

    switch (type) {
      case DartElement.METHOD:
        getMethodLabel((Method) element, flags, buf);
        break;
      case DartElement.FIELD:
        getFieldLabel((Field) element, flags, buf);
        break;
      case DartElement.FUNCTION:
        getFunctionLabel((DartFunction) element, flags, buf);
        break;
//      case DartElement.LOCAL_VARIABLE:
//        getLocalVariableLabel((ILocalVariable) element, flags, buf);
//        break;
//      case DartElement.INITIALIZER:
//        getInitializerLabel((IInitializer) element, flags, buf);
//        break;
      case DartElement.TYPE:
        getTypeLabel((Type) element, flags, buf);
        break;
//      case DartElement.CLASS_FILE:
//        getClassFileLabel((IClassFile) element, flags, buf);
//        break;
      case DartElement.COMPILATION_UNIT:
        getCompilationUnitLabel((CompilationUnit) element, flags, buf);
        break;
//      case DartElement.PACKAGE_FRAGMENT:
//        getPackageFragmentLabel((IPackageFragment) element, flags, buf);
//        break;
//      case DartElement.PACKAGE_FRAGMENT_ROOT:
//        getPackageFragmentRootLabel((IPackageFragmentRoot) element, flags, buf);
//        break;
      case DartElement.IMPORT_CONTAINER:
//      case DartElement.IMPORT_DECLARATION:
        getDeclarationLabel(element, flags, buf);
        break;
      case DartElement.DART_PROJECT:
      case DartElement.DART_MODEL:
        buf.append(element.getElementName());
        break;
      case DartElement.LIBRARY:
        buf.append(((DartLibrary) element).getDisplayName());
        break;
      default:
        buf.append(element.getElementName());
    }

//    if (root != null && getFlag(flags, APPEND_ROOT_PATH)) {
//      buf.append(CONCAT_STRING);
//      getPackageFragmentRootLabel(root, ROOT_QUALIFIED, buf);
//    }
  }

  /**
   * Returns the styled label for a Java element with the flags as defined by this class.
   * 
   * @param element the element to render
   * @param flags the rendering flags
   * @param result the buffer to append the resulting label to
   */
  public static void getElementLabel(DartElement element, long flags, StyledString result) {
    new DartElementLabelComposer(result).appendElementLabel(element, flags);
  }

  /**
   * Appends the label for a field to a {@link StringBuffer}. Considers the F_* flags.
   * 
   * @param field The element to render.
   * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
   * @param buf The buffer to append the resulting label to.
   */
  public static void getFieldLabel(Field field, long flags, StringBuffer buf) {
    try {
      if (getFlag(flags, F_PRE_TYPE_SIGNATURE) && field.exists()) {
//        if (getFlag(flags, USE_RESOLVED) && field.isResolved()) {
//          getTypeSignatureLabel(new BindingKey(field.getKey()).toSignature(),
//              flags, buf);
//        } else {
        buf.append(field.getTypeName());
//        }
        buf.append(' ');
      }

      // qualification
      Type declaringType = field.getDeclaringType();
      if (getFlag(flags, F_FULLY_QUALIFIED)) {
        if (declaringType != null) {
          getTypeLabel(declaringType, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
        } else {
          getFileLabel(field, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
        }
        buf.append('.');
      }
      buf.append(field.getElementName()); // getDisplayName()

      DartX.todo();
      if (getFlag(flags, F_APP_TYPE_SIGNATURE) && field.exists()) {
        if (field.getTypeName() != null) {
          buf.append(DECL_STRING);
//          if (getFlag(flags, USE_RESOLVED) && field.isResolved()) {
//            getTypeSignatureLabel(new BindingKey(field.getKey()).toSignature(),
//                flags, buf);
//          } else {
          buf.append(field.getTypeName());
//          }
        }
      }

      // category
//      if (getFlag(flags, F_CATEGORY) && field.exists())
//        getCategoryLabel(field, buf);

      // post qualification
      if (getFlag(flags, F_POST_QUALIFIED)) {
        buf.append(CONCAT_STRING);
        if (declaringType != null) {
          getTypeLabel(declaringType, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
        } else {
          getFileLabel(field, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
        }
      }
    } catch (DartModelException ex) {
      DartToolsPlugin.log(ex);
    }
  }

  public static void getFileLabel(TypeMember member, long flags, StringBuffer buf) {
    CompilationUnit compUnit = member.getCompilationUnit();
    if (compUnit != null) {
      getCompilationUnitLabel(compUnit, flags, buf);
    }
  }

//  /**
//   * Appends the label for a initializer to a {@link StringBuffer}. Considers
//   * the I_* flags.
//   * 
//   * @param initializer The element to render.
//   * @param flags The rendering flags. Flags with names starting with 'I_' are
//   *          considered.
//   * @param buf The buffer to append the resulting label to.
//   */
//  public static void getInitializerLabel(IInitializer initializer, long flags,
//      StringBuffer buf) {
//    // qualification
//    if (getFlag(flags, I_FULLY_QUALIFIED)) {
//      getTypeLabel(initializer.getDeclaringType(), T_FULLY_QUALIFIED
//          | (flags & QUALIFIER_FLAGS), buf);
//      buf.append('.');
//    }
//    buf.append(DartUIMessages.JavaElementLabels_initializer);
//
//    // post qualification
//    if (getFlag(flags, I_POST_QUALIFIED)) {
//      buf.append(CONCAT_STRING);
//      getTypeLabel(initializer.getDeclaringType(), T_FULLY_QUALIFIED
//          | (flags & QUALIFIER_FLAGS), buf);
//    }
//  }

  public static void getFunctionLabel(DartFunction function, long flags, StringBuffer buf) {
    DartX.todo();
    try {
//    BindingKey resolvedKey = getFlag(flags, USE_RESOLVED)
//        && method.isResolved() ? new BindingKey(method.getKey()) : null;
//    String resolvedSig = (resolvedKey != null) ? resolvedKey.toSignature()
//        : null;

      // return type
      if (getFlag(flags, M_PRE_RETURNTYPE) && function.exists()) {
        buf.append(function.getReturnTypeName());
        buf.append(' ');
      }
      // TODO(devoncarew): function.getElementName() should return null for empty names.
      if (function.getElementName() == null || function.getElementName().length() == 0) {
        buf.append("function");
      } else {
        buf.append(function.getElementName());
      }
      getCommonFunctionLabelElements(function, flags, buf);
    } catch (DartModelException ex) {
      DartToolsPlugin.log(ex);
    }
  }

  /**
   * Appends the label for a method to a {@link StringBuffer}. Considers the M_* flags.
   * 
   * @param method The element to render.
   * @param flags The rendering flags. Flags with names starting with 'M_' are considered.
   * @param buf The buffer to append the resulting label to.
   */
  public static void getMethodLabel(Method method, long flags, StringBuffer buf) {
    DartX.todo();
    try {
//      BindingKey resolvedKey = getFlag(flags, USE_RESOLVED)
//          && method.isResolved() ? new BindingKey(method.getKey()) : null;
//      String resolvedSig = (resolvedKey != null) ? resolvedKey.toSignature()
//          : null;
//
      // return type
      if (getFlag(flags, M_PRE_RETURNTYPE) && method.exists() && !method.isConstructor()) {
        buf.append(method.getReturnTypeName());
        buf.append(' ');
      }

      // qualification
      Type declaringType = method.getDeclaringType();
      if (getFlag(flags, M_FULLY_QUALIFIED)) {
        if (declaringType != null && !method.isConstructor()) {
          getTypeLabel(method.getDeclaringType(), T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
          buf.append('.');
        }
        // else {
        // buf.append('[');
        // getFileLabel(method, T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS),
        // buf);
        // buf.append(']');
        // }
      }

      // if(buf.length() != 0)
      // buf.append(' ');
      buf.append(method.getElementName());
      getCommonFunctionLabelElements(method, flags, buf);

      if (getFlag(flags, M_EXCEPTIONS)) {
        String[] types;
        DartX.todo();
//        if (resolvedKey != null) {
//          types= resolvedKey.getThrownExceptions();
//          types = new String[0];
//        } else {
        types = new String[0];
//        }
        if (types.length > 0) {
          buf.append(" throws "); //$NON-NLS-1$
          for (int i = 0; i < types.length; i++) {
            if (i > 0) {
              buf.append(COMMA_STRING);
            }
            getTypeSignatureLabel(types[i], flags, buf);
          }
        }
      }

      if (getFlag(flags, M_APP_RETURNTYPE) && method.exists() && !method.isConstructor()) {
        DartX.todo();
        if (method.getReturnTypeName() != null) {
          buf.append(DECL_STRING);
          buf.append(method.getReturnTypeName());
        }
      }

      // category
//      if (getFlag(flags, M_CATEGORY) && method.exists())
//        getCategoryLabel(method, buf);

      // post qualification
      if (getFlag(flags, M_POST_QUALIFIED)) {
        buf.append(CONCAT_STRING);
        if (declaringType != null) {
          getTypeLabel(method.getDeclaringType(), T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
        } else {
          getFileLabel(method, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
        }
      }
    } catch (DartModelException ex) {
      DartToolsPlugin.log(ex);
    }
  }

  /**
   * Appends the label for a local variable to a {@link StringBuffer}.
   * 
   * @param localVariable The element to render.
   * @param flags The rendering flags. Flags with names starting with 'F_' are considered.
   * @param buf The buffer to append the resulting label to. TODO implement labels for local
   *          variables
   */
//  public static void getLocalVariableLabel(ILocalVariable localVariable,
//      long flags, StringBuffer buf) {
//    if (getFlag(flags, F_PRE_TYPE_SIGNATURE)) {
//      getTypeSignatureLabel(localVariable.getTypeSignature(), flags, buf);
//      buf.append(' ');
//    }
//
//    if (getFlag(flags, F_FULLY_QUALIFIED)) {
//      getElementLabel(localVariable.getParent(), M_PARAMETER_TYPES
//          | M_FULLY_QUALIFIED | T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS),
//          buf);
//      buf.append('.');
//    }
//
//    buf.append(localVariable.getDisplayName());
//
//    if (getFlag(flags, F_APP_TYPE_SIGNATURE)) {
//      buf.append(DECL_STRING);
//      getTypeSignatureLabel(localVariable.getTypeSignature(), flags, buf);
//    }
//
//    // post qualification
//    if (getFlag(flags, F_POST_QUALIFIED)) {
//      buf.append(CONCAT_STRING);
//      getElementLabel(localVariable.getParent(), M_PARAMETER_TYPES
//          | M_FULLY_QUALIFIED | T_FULLY_QUALIFIED | (flags & QUALIFIER_FLAGS),
//          buf);
//    }
//  }

  /**
   * Appends the label for a package fragment to a {@link StringBuffer}. Considers the P_* flags.
   * 
   * @param pack The element to render.
   * @param flags The rendering flags. Flags with names starting with P_' are considered.
   * @param buf The buffer to append the resulting label to.
   */
//  public static void getPackageFragmentLabel(IPackageFragment pack, long flags,
//      StringBuffer buf) {
//    if (getFlag(flags, P_QUALIFIED)) {
//      getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(),
//          ROOT_QUALIFIED, buf);
//      buf.append('/');
//    }
//    refreshPackageNamePattern();
//    if (pack.isDefaultPackage()) {
//      buf.append(DEFAULT_PACKAGE);
//    } else if (getFlag(flags, P_COMPRESSED) && fgPkgNameLength >= 0) {
//      String name = pack.getDisplayName();
//      int start = 0;
//      int dot = name.indexOf('.', start);
//      while (dot > 0) {
//        if (dot - start > fgPkgNameLength - 1) {
//          buf.append(fgPkgNamePrefix);
//          if (fgPkgNameChars > 0)
//            buf.append(name.substring(start,
//                Math.min(start + fgPkgNameChars, dot)));
//          buf.append(fgPkgNamePostfix);
//        } else
//          buf.append(name.substring(start, dot + 1));
//        start = dot + 1;
//        dot = name.indexOf('.', start);
//      }
//      buf.append(name.substring(start));
//    } else {
//      // buf.append(pack.getDisplayName());
//    }
//    if (getFlag(flags, P_POST_QUALIFIED)) {
//      buf.append(CONCAT_STRING);
//      getPackageFragmentRootLabel((IPackageFragmentRoot) pack.getParent(),
//          ROOT_QUALIFIED, buf);
//    }
//  }

  /**
   * Appends the label for a package fragment root to a {@link StringBuffer}. Considers the ROOT_*
   * flags.
   * 
   * @param root The element to render.
   * @param flags The rendering flags. Flags with names starting with ROOT_' are considered.
   * @param buf The buffer to append the resulting label to.
   */
//  public static void getPackageFragmentRootLabel(IPackageFragmentRoot root,
//      long flags, StringBuffer buf) {
//    if (root.isArchive())
//      getArchiveLabel(root, flags, buf);
//    else
//      getFolderLabel(root, flags, buf);
//  }

  /**
   * Returns the styled label of the given object. The object must be of type {@link DartElement} or
   * adapt to {@link IWorkbenchAdapter}. If the element type is not known, the empty string is
   * returned. The returned label is BiDi-processed with
   * {@link TextProcessor#process(String, String)}.
   * 
   * @param obj object to get the label for
   * @param flags the rendering flags
   * @return the label or the empty string if the object type is not supported
   */
  public static StyledString getStyledTextLabel(Object obj, long flags) {
    if (obj instanceof IResource) {
      return getStyledResourceLabel((IResource) obj);

//    } else if (obj instanceof ClassPathContainer) {
//      ClassPathContainer container= (ClassPathContainer) obj;
//      return getStyledContainerEntryLabel(container.getClasspathEntry().getPath(), container.getJavaProject());

    } else if (obj instanceof IStorage) {
      return getStyledStorageLabel((IStorage) obj);

    } else if (obj instanceof IAdaptable) {
      IWorkbenchAdapter wbadapter = (IWorkbenchAdapter) ((IAdaptable) obj).getAdapter(IWorkbenchAdapter.class);
      if (wbadapter != null) {
        return Strings.markLTR(new StyledString(wbadapter.getLabel(obj)));
      }
    }
    return new StyledString();
  }

  /**
   * Returns the label of the given object. The object must be of type {@link DartElement} or adapt
   * to {@link IWorkbenchAdapter}. The empty string is returned if the element type is not known.
   * 
   * @param obj Object to get the label from.
   * @param flags The rendering flags
   * @return Returns the label or the empty string if the object type is not supported.
   */
  public static String getTextLabel(Object obj, long flags) {
    return NewDartElementLabels.getTextLabel(obj, flags);
  }

  /**
   * Appends the label for a type to a {@link StringBuffer}. Considers the T_* flags.
   * 
   * @param type The element to render.
   * @param flags The rendering flags. Flags with names starting with 'T_' are considered.
   * @param buf The buffer to append the resulting label to.
   */
  public static void getTypeLabel(Type type, long flags, StringBuffer buf) {

    if (getFlag(flags, T_FULLY_QUALIFIED)) {
      DartX.todo();
//      IPackageFragment pack = type.getPackageFragment();
//      if (!pack.isDefaultPackage()) {
//        getPackageFragmentLabel(pack, (flags & QUALIFIER_FLAGS), buf);
//        // buf.append(' ');
//      }
    }
    if (getFlag(flags, T_FULLY_QUALIFIED | T_CONTAINER_QUALIFIED)) {
      DartX.todo();
//      Type declaringType = type.getDeclaringType();
//      if (declaringType != null) {
//        getTypeLabel(declaringType, T_CONTAINER_QUALIFIED
//            | (flags & QUALIFIER_FLAGS), buf);
//        buf.append('.');
//      }
//      int parentType = type.getParent().getElementType();
//      if (parentType == DartElement.METHOD || parentType == DartElement.FIELD
//          || parentType == DartElement.INITIALIZER) { // anonymous or local
//        getElementLabel(type.getParent(), 0, buf);
//        buf.append('.');
//      }
    }

    String typeName = type.getElementName(); // was getDisplayName()
    if (typeName.length() == 0) { // anonymous
      try {
        String supertypeName = type.getSuperclassName();

        typeName = Messages.format(DartUIMessages.JavaElementLabels_anonym_type, supertypeName);

      } catch (DartModelException e) {
        // ignore
        typeName = DartUIMessages.JavaElementLabels_anonym;
      }
    }
    buf.append(typeName);
    if (getFlag(flags, T_TYPE_PARAMETERS)) {
      DartX.todo();
//      if (getFlag(flags, USE_RESOLVED) && type.isResolved()) {
//        getTypeParameterSignaturesLabel(new String[0], flags, buf);
//      }
    }

    // category
//    if (getFlag(flags, T_CATEGORY) && type.exists()) {
//      try {
//        getCategoryLabel(type, buf);
//      } catch (DartModelException e) {
//        // ignore
//      }
//    }

    // post qualification
    if (getFlag(flags, T_POST_QUALIFIED)) {
      DartX.todo();
//      buf.append(CONCAT_STRING);
//      Type declaringType = type.getDeclaringType();
//      if (declaringType != null) {
//        getTypeLabel(declaringType, T_FULLY_QUALIFIED
//            | (flags & QUALIFIER_FLAGS), buf);
//        int parentType = type.getParent().getElementType();
//        if (parentType == DartElement.METHOD || parentType == DartElement.FIELD
//            || parentType == DartElement.INITIALIZER) { // anonymous or local
//          buf.append('.');
//          getElementLabel(type.getParent(), 0, buf);
//        }
//      } else {
//        getPackageFragmentLabel(type.getPackageFragment(), flags
//            & QUALIFIER_FLAGS, buf);
//      }
    }
  }

  /**
   * Returns a label for a working set
   * 
   * @param set the working set
   * @return the label of the working set
   */
  public static String getWorkingSetLabel(IWorkingSet set) {
    return Strings.markLTR(set.getLabel());
  }

  @SuppressWarnings("unused")
  private static void getCategoryLabel(TypeMember member, StringBuffer buf)
      throws DartModelException {
    DartX.todo();
//    String[] categories = member.getCategories();
//    if (categories.length > 0) {
//      StringBuffer categoriesBuf = new StringBuffer(30);
//      for (int i = 0; i < categories.length; i++) {
//        if (i > 0)
//          categoriesBuf.append(CATEGORY_SEPARATOR_STRING);
//        categoriesBuf.append(categories[i]);
//      }
//      buf.append(CONCAT_STRING);
//      buf.append(Messages.format(DartUIMessages.JavaElementLabels_category,
//          categoriesBuf.toString()));
//    }
  }

  private static void getCommonFunctionLabelElements(DartFunction method, long flags,
      StringBuffer buf) {
    if (method instanceof Method) {
      Method m = (Method) method;
      if (m.isGetter()) {
        return;
      }
    }
    // parameters
    try {
      buf.append('(');
      if (getFlag(flags, M_PARAMETER_TYPES | M_PARAMETER_NAMES)) {
        String[] types = null;
        int nParams = 0;
        boolean renderVarargs = false;
        if (getFlag(flags, M_PARAMETER_TYPES)) {
          DartX.todo();
          types = method.getParameterTypeNames();
          nParams = types.length;
          DartX.todo();
//        renderVarargs = method.exists() && Flags.isVarargs(method.getFlags());
        }
        String[] names = null;
        if (getFlag(flags, M_PARAMETER_NAMES) && method.exists()) {
          DartX.todo();
          try {
            names = method.getParameterNames();
          } catch (DartModelException ex) {
            names = new String[0];
          }
          nParams = names.length;
          if (types == null) {
            nParams = names.length;
          } else { // types != null
            if (nParams != names.length) {
              if (types.length > names.length) {
                nParams = names.length;
                String[] typesWithoutSyntheticParams = new String[nParams];
                System.arraycopy(
                    types,
                    types.length - nParams,
                    typesWithoutSyntheticParams,
                    0,
                    nParams);
                types = typesWithoutSyntheticParams;
              } else {
                names = null; // no names rendered
              }
            }
          }
        }

        for (int i = 0; i < nParams; i++) {
          if (i > 0) {
            buf.append(COMMA_STRING);
          }
          if (types != null) {
            String paramSig = types[i];
            if (renderVarargs && i == nParams - 1) {
              DartX.todo();
              buf.append(paramSig);
//            int newDim = Signature.getArrayCount(paramSig) - 1;
//            getTypeSignatureLabel(Signature.getElementType(paramSig), flags,
//                buf);
//            for (int k = 0; k < newDim; k++) {
//              buf.append('[').append(']');
//            }
              buf.append(ELLIPSIS_STRING);
            } else {
//              getTypeSignatureLabel(paramSig, flags, buf);
              buf.append(paramSig);
            }
          }
          if (names != null) {
            if (types != null) {
              buf.append(' ');
            }
            buf.append(names[i]);
          }
        }
      } else {
        if (method.getParameterTypeNames().length > 0) {
          buf.append(ELLIPSIS_STRING);
        }
      }
      buf.append(')');
    } catch (DartModelException ex) {
      DartToolsPlugin.log(ex);
    }
  }

//  private static void getArchiveLabel(IPackageFragmentRoot root, long flags,
//      StringBuffer buf) {
//    // Handle variables different
//    if (getFlag(flags, ROOT_VARIABLE) && getVariableLabel(root, flags, buf))
//      return;
//    boolean external = root.isExternal();
//    if (external)
//      getExternalArchiveLabel(root, flags, buf);
//    else
//      getInternalArchiveLabel(root, flags, buf);
//  }

  private static final boolean getFlag(long flags, long flag) {
    return (flags & flag) != 0;
  }

//  private static void getExternalArchiveLabel(IPackageFragmentRoot root,
//      long flags, StringBuffer buf) {
//    IPath path = root.getPath();
//    if (getFlag(flags, REFERENCED_ROOT_POST_QUALIFIED)) {
//      int segements = path.segmentCount();
//      if (segements > 0) {
//        buf.append(path.segment(segements - 1));
//        if (segements > 1 || path.getDevice() != null) {
//          buf.append(CONCAT_STRING);
//          buf.append(path.removeLastSegments(1).toOSString());
//        }
//      } else {
//        buf.append(path.toOSString());
//      }
//    } else {
//      buf.append(path.toOSString());
//    }
//  }

  /**
   * Returns the styled string for the given resource. The returned label is BiDi-processed with
   * {@link TextProcessor#process(String, String)}.
   * 
   * @param resource the resource
   * @return the styled string
   */
  private static StyledString getStyledResourceLabel(IResource resource) {
    StyledString result = new StyledString(resource.getName());
    return Strings.markLTR(result);
  }

//  private static void getFolderLabel(IPackageFragmentRoot root, long flags,
//      StringBuffer buf) {
//    IResource resource = root.getResource();
//    boolean rootQualified = getFlag(flags, ROOT_QUALIFIED);
//    boolean referencedQualified = getFlag(flags, REFERENCED_ROOT_POST_QUALIFIED)
//        && isReferenced(root);
//    if (rootQualified) {
//      // buf.append(root.getPath().makeRelative().toString());
//      // for libraries stored in our metadata area, just show the filename
//      IPath stateLocation = JavaScriptCore.getJavaScriptCore().getStateLocation();
//      if (stateLocation.isPrefixOf(root.getPath())) {
//        buf.append(root.getPath().lastSegment().toString());
//      } else {
//        buf.append(root.getPath().toString());
//      }
//    } else {
//      if (resource != null) {
//        IPath projectRelativePath = resource.getProjectRelativePath();
//        if (projectRelativePath.segmentCount() == 0) {
//          buf.append(resource.getName());
//          referencedQualified = false;
//        } else {
//          buf.append(projectRelativePath.toString());
//        }
//      } else
//        buf.append(root.getDisplayName());
//      if (referencedQualified) {
//        buf.append(CONCAT_STRING);
//        buf.append(resource.getProject().getName());
//      } else if (getFlag(flags, ROOT_POST_QUALIFIED)) {
//        buf.append(CONCAT_STRING);
//        buf.append(root.getParent().getDisplayName());
//      }
//    }
//  }

//  private static void getInternalArchiveLabel(IPackageFragmentRoot root,
//      long flags, StringBuffer buf) {
//    IResource resource = root.getResource();
//    boolean rootQualified = getFlag(flags, ROOT_QUALIFIED);
//    boolean referencedQualified = getFlag(flags, REFERENCED_ROOT_POST_QUALIFIED)
//        && isReferenced(root);
//    if (rootQualified) {
//      buf.append(root.getPath().makeRelative().toString());
//    } else {
//      buf.append(root.getDisplayName());
//      if (referencedQualified) {
//        buf.append(CONCAT_STRING);
//        buf.append(resource.getParent().getFullPath().makeRelative().toString());
//      } else if (getFlag(flags, ROOT_POST_QUALIFIED)) {
//        buf.append(CONCAT_STRING);
//        buf.append(root.getParent().getPath().makeRelative().toString());
//      }
//    }
//  }

  /**
   * Returns the styled string for the given storage. The returned label is BiDi-processed with
   * {@link TextProcessor#process(String, String)}.
   * 
   * @param storage the storage
   * @return the styled string
   */
  private static StyledString getStyledStorageLabel(IStorage storage) {
    StyledString result = new StyledString(storage.getName());
    return Strings.markLTR(result);
  }

  @SuppressWarnings("unused")
  private static void getTypeArgumentSignaturesLabel(String[] typeArgsSig, long flags,
      StringBuffer buf) {
    if (typeArgsSig.length > 0) {
      buf.append('<');
      for (int i = 0; i < typeArgsSig.length; i++) {
        if (i > 0) {
          buf.append(COMMA_STRING);
        }
        getTypeSignatureLabel(typeArgsSig[i], flags, buf);
      }
      buf.append('>');
    }
  }

  @SuppressWarnings("unused")
  private static void getTypeParameterSignaturesLabel(String[] typeParamSigs, long flags,
      StringBuffer buf) {
    if (typeParamSigs.length > 0) {
      buf.append('<');
      for (int i = 0; i < typeParamSigs.length; i++) {
        if (i > 0) {
          buf.append(COMMA_STRING);
        }
//        buf.append(Signature.getTypeVariable(typeParamSigs[i]));
        buf.append(typeParamSigs[i]);
      }
      buf.append('>');
    }
  }

  private static void getTypeSignatureLabel(String typeSig, long flags, StringBuffer buf) {
//    int sigKind = Signature.getTypeSignatureKind(typeSig);
//    switch (sigKind) {
//      case Signature.BASE_TYPE_SIGNATURE:
//        buf.append(Signature.toString(typeSig));
//        break;
//      case Signature.ARRAY_TYPE_SIGNATURE:
//        getTypeSignatureLabel(Signature.getElementType(typeSig), flags, buf);
//        for (int dim = Signature.getArrayCount(typeSig); dim > 0; dim--) {
//          buf.append('[').append(']');
//        }
//        break;
//      case Signature.CLASS_TYPE_SIGNATURE:
//        String baseType = Signature.toString(typeSig);
//
//        // @GINO: Anonymous UI Label
//        Util.insertTypeLabel(
//            Signature.getSimpleName(baseType), buf);
//
//        getTypeArgumentSignaturesLabel(new String[0], flags, buf);
//        break;
//      default:
//        // unknown
//    }
  }

//  private static boolean getVariableLabel(IPackageFragmentRoot root,
//      long flags, StringBuffer buf) {
//    try {
//      IIncludePathEntry rawEntry = root.getRawIncludepathEntry();
//      if (rawEntry != null
//          && rawEntry.getEntryKind() == IIncludePathEntry.CPE_VARIABLE) {
//        IPath path = rawEntry.getPath().makeRelative();
//        if (getFlag(flags, REFERENCED_ROOT_POST_QUALIFIED)) {
//          int segements = path.segmentCount();
//          if (segements > 0) {
//            buf.append(path.segment(segements - 1));
//            if (segements > 1) {
//              buf.append(CONCAT_STRING);
//              buf.append(path.removeLastSegments(1).toOSString());
//            }
//          } else {
//            buf.append(path.toString());
//          }
//        } else {
//          buf.append(path.toString());
//        }
//        buf.append(CONCAT_STRING);
//        if (root.isExternal())
//          buf.append(root.getPath().toOSString());
//        else
//          buf.append(root.getPath().makeRelative().toString());
//        return true;
//      }
//    } catch (DartModelException e) {
//      DartToolsPlugin.log(e); // problems with class path
//    }
//    return false;
//  }

  /**
   * Returns <code>true</code> if the given package fragment root is referenced. This means it is
   * own by a different project but is referenced by the root's parent. Returns <code>false</code>
   * if the given root doesn't have an underlying resource.
   */
//  private static boolean isReferenced(IPackageFragmentRoot root) {
//    IResource resource = root.getResource();
//    if (resource != null) {
//      IProject jarProject = resource.getProject();
//      IProject container = root.getJavaScriptProject().getProject();
//      return !container.equals(jarProject);
//    }
//    return false;
//  }

  private DartElementLabels() {
  }

}
