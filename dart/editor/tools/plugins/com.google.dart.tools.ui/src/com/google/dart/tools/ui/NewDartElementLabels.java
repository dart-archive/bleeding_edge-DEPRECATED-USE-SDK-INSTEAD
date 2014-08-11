/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.tools.ui.internal.viewsupport.StorageLabelProvider;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Provides helper methods to render names of Dart elements.
 * <p>
 * <b>NOTE:</b> this will replace {@link DartElementLabels}.
 */
public class NewDartElementLabels {

  /**
   * Field names contain the declared type (prepended) e.g. <code>int fHello</code>
   */
  public final static long F_PRE_TYPE_SIGNATURE = 1L << 15;

  /**
   * Fields names are fully qualified. e.g. <code>java.lang.System.out</code>
   */
  public final static long F_FULLY_QUALIFIED = 1L << 16;

  /**
   * Field names contain the declared type (appended) e.g. <code>fHello : int</code>
   */
  public final static long F_APP_TYPE_SIGNATURE = 1L << 14;

  /**
   * Fields names are post qualified. e.g. <code>out - java.lang.System</code>
   */
  public final static long F_POST_QUALIFIED = 1L << 17;

  /**
   * Package names are compressed. e.g. <code>o*.e*.search</code>
   */
  public final static long P_COMPRESSED = 1L << 37;

  /**
   * Specified to use the resolved information of a Type, IFunction or IField. See
   * {@link Type#isResolved()}. If resolved information is available, types will be rendered with
   * type parameters of the instantiated type. Resolved method render with the parameter types of
   * the method instance. <code>Vector&lt;String&gt;.get(String)</code>
   */
  public final static long USE_RESOLVED = 1L << 48;

  private final static long QUALIFIER_FLAGS = P_COMPRESSED | USE_RESOLVED;

  /**
   * Type names are fully qualified. e.g. <code>java.util.Map.MapEntry</code>
   */
  public final static long T_FULLY_QUALIFIED = 1L << 18;

  /**
   * User-readable string for separating the return type (e.g. " : ").
   */
  public final static String DECL_STRING = DartUIMessages.JavaElementLabels_declseparator_string;

  /**
   * User-readable string for separating post qualified names (e.g. " - ").
   */
  public final static String CONCAT_STRING = DartUIMessages.JavaElementLabels_concat_string;

  /**
   * Method names contain return type (appended) e.g. <code>int foo</code>
   */
  public final static long M_PRE_RETURNTYPE = 1L << 6;

  /**
   * Method names contain parameter types. e.g. <code>foo(int)</code>
   */
  public final static long M_PARAMETER_TYPES = 1L << 0;

  /**
   * Method names contain parameter names. e.g. <code>foo(index)</code>
   */
  public final static long M_PARAMETER_NAMES = 1L << 1;

  /**
   * User-readable string for separating list items (e.g. ", ").
   */
  public final static String COMMA_STRING = DartUIMessages.JavaElementLabels_comma_string;

  /**
   * User-readable string for ellipsis ("...").
   */
  public final static String ELLIPSIS_STRING = "..."; //$NON-NLS-1$

  /**
   * Method names contain return type (appended) e.g. <code>foo : int</code>
   */
  public final static long M_APP_RETURNTYPE = 1L << 5;

  /**
   * Method names contain thrown exceptions. e.g. <code>foo throws IOException</code>
   */
  public final static long M_EXCEPTIONS = 1L << 4;

  /**
   * Method names are fully qualified. e.g. <code>java.util.Vector.size</code>
   */
  public final static long M_FULLY_QUALIFIED = 1L << 7;

  /**
   * Method names are post qualified. e.g. <code>size - java.util.Vector</code>
   */
  public final static long M_POST_QUALIFIED = 1L << 8;

  /**
   * Returns the label for a Dart element with the given flags.
   * 
   * @param element The element to render.
   * @param flags The rendering flags.
   * @return the label of the Dart element
   */
  public static String getElementLabel(Element element, long flags) {
    StringBuffer buf = new StringBuffer(60);
    getElementLabel(element, flags, buf);
    return buf.toString();
  }

  /**
   * Returns the label for a Dart element with the given flags.
   * 
   * @param element The element to render.
   * @param flags The rendering flags.
   * @param buf The buffer to append the resulting label to.
   */
  public static void getElementLabel(Element element, long flags, StringBuffer buf) {

    ElementKind kind = element.getKind();

    switch (kind) {

      case CONSTRUCTOR:
      case METHOD:
        getMethodLabel((ExecutableElement) element, flags, buf);
        break;

      case FIELD:
        getFieldLabel((FieldElement) element, flags, buf);
        break;

      case FUNCTION:
        getFunctionLabel((FunctionElement) element, flags, buf);
        break;

      case CLASS:
        getClassLabel((ClassElement) element, buf);
        break;

      default:
        buf.append(element.getDisplayName());
    }

  }

  public static String getTextLabel(Object obj, long flags) {
    if (obj instanceof Element) {
      return getElementLabel((Element) obj, flags);
    } else if (obj instanceof IResource) {
      return ((IResource) obj).getName();
    } else if (obj instanceof IStorage) {
      StorageLabelProvider storageLabelProvider = new StorageLabelProvider();
      String label = storageLabelProvider.getText(obj);
      storageLabelProvider.dispose();
      return label;
    } else if (obj instanceof IAdaptable) {
      IWorkbenchAdapter wbadapter = (IWorkbenchAdapter) ((IAdaptable) obj).getAdapter(IWorkbenchAdapter.class);
      if (wbadapter != null) {
        return wbadapter.getLabel(obj);
      }
    }
    return ""; //$NON-NLS-1$

  }

  private static void getClassLabel(ClassElement cls, StringBuffer buf) {

    String clsName = cls.getDisplayName();
    if (clsName.length() == 0) { // anonymous
      String supertypeName = cls.getSupertype().getDisplayName();
      clsName = Messages.format(DartUIMessages.JavaElementLabels_anonym_type, supertypeName);
    }

    buf.append(clsName);
  }

  private static void getCommonFunctionLabelElements(ExecutableElement method, long flags,
      StringBuffer buf) {

    if (method instanceof MethodElement) {
      MethodElement m = (MethodElement) method;
      if (m.getKind() == ElementKind.GETTER) {
        return;
      }
    }

    buf.append('(');
    if (getFlag(flags, M_PARAMETER_TYPES | M_PARAMETER_NAMES)) {
      String[] types = null;
      int nParams = 0;
      boolean renderVarargs = false;
      if (getFlag(flags, M_PARAMETER_TYPES)) {
        types = getParameterTypeNames(method);
        nParams = types.length;
      }
      String[] names = null;
      if (getFlag(flags, M_PARAMETER_NAMES)) {
        names = getParameterNames(method);
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
            buf.append(paramSig);
            buf.append(ELLIPSIS_STRING);
          } else {
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
      if (getParameterTypeNames(method).length > 0) {
        buf.append(ELLIPSIS_STRING);
      }
    }
    buf.append(')');
  }

  private static void getCompilationUnitLabel(CompilationUnitElement cu, long flags,
      StringBuffer buf) {
    buf.append(cu.getDisplayName());
  }

  private static String getDisplayName(ExecutableElement elem, ClassElement declaringType) {
    if (elem instanceof ConstructorElement) {
      ConstructorElement ce = (ConstructorElement) elem;
      String name = declaringType.getDisplayName();
      String constructorName = ce.getDisplayName();
      if (constructorName != null && constructorName.length() > 0) {
        name = name + '.' + constructorName;
      }
      return name;
    }
    return elem.getDisplayName();
  }

  private static ClassElement getEnclosingElement(ExecutableElement elem) {
    if (elem instanceof ConstructorElement) {
      return ((ConstructorElement) elem).getEnclosingElement();
    }
    if (elem instanceof MethodElement) {
      return ((MethodElement) elem).getEnclosingElement();
    }
    throw new IllegalArgumentException("Expected Constructor or Method but got: " + elem.getKind());
  }

  private static void getFieldLabel(FieldElement field, long flags, StringBuffer buf) {

    String typeName = getTypeName(field);

    if (getFlag(flags, F_PRE_TYPE_SIGNATURE)) {
      if (typeName != null) {
        buf.append(typeName);
        buf.append(' ');
      }
    }

    // Qualification
    ClassElement declaringType = field.getAncestor(ClassElement.class);
    if (getFlag(flags, F_FULLY_QUALIFIED)) {
      if (declaringType != null) {
        getTypeLabel(declaringType, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
      } else {
        getFileLabel(field, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
      }
      buf.append('.');
    }
    buf.append(field.getDisplayName());

    if (getFlag(flags, F_APP_TYPE_SIGNATURE)) {
      if (typeName != null) {
        buf.append(DECL_STRING);
        buf.append(typeName);
      }
    }

    // Post qualification
    if (getFlag(flags, F_POST_QUALIFIED)) {
      buf.append(CONCAT_STRING);
      if (declaringType != null) {
        getTypeLabel(declaringType, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
      } else {
        getFileLabel(field, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
      }
    }
  }

  private static void getFileLabel(Element elem, long flags, StringBuffer buf) {
    CompilationUnitElement compUnit = elem.getAncestor(CompilationUnitElement.class);
    if (compUnit != null) {
      getCompilationUnitLabel(compUnit, flags, buf);
    }
  }

  private static final boolean getFlag(long flags, long flag) {
    return (flags & flag) != 0;
  }

  private static void getFunctionLabel(FunctionElement function, long flags, StringBuffer buf) {
    // Return type
    if (getFlag(flags, M_PRE_RETURNTYPE)) {
      buf.append(function.getType().getReturnType().getDisplayName());
      buf.append(' ');
    }

    String name = function.getDisplayName();
    if (name == null || name.length() == 0) {
      buf.append("function");
    } else {
      buf.append(name);
    }

    getCommonFunctionLabelElements(function, flags, buf);
  }

  private static void getMethodLabel(ExecutableElement elem, long flags, StringBuffer buf) {

    // return type
    if (getFlag(flags, M_PRE_RETURNTYPE) && elem.getKind() != ElementKind.CONSTRUCTOR) {
      buf.append(elem.getType().getReturnType().getDisplayName());
      buf.append(' ');
    }

    // qualification
    ClassElement declaringType = getEnclosingElement(elem);

    if (getFlag(flags, M_FULLY_QUALIFIED)) {
      if (declaringType != null && elem.getKind() != ElementKind.CONSTRUCTOR) {
        getTypeLabel(declaringType, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
        buf.append('.');
      }
    }

    buf.append(getDisplayName(elem, declaringType));

    getCommonFunctionLabelElements(elem, flags, buf);

    if (getFlag(flags, M_EXCEPTIONS)) {
      String[] types;

      types = new String[0];
      if (types.length > 0) {
        buf.append(" throws "); //$NON-NLS-1$
        for (int i = 0; i < types.length; i++) {
          if (i > 0) {
            buf.append(COMMA_STRING);
          }
        }
      }
    }

    if (getFlag(flags, M_APP_RETURNTYPE) && elem.getKind() != ElementKind.CONSTRUCTOR) {

      String returnTypeName = elem.getType().getReturnType().getDisplayName();

      if (returnTypeName != null) {
        buf.append(DECL_STRING);
        buf.append(returnTypeName);
      }
    }

    // post qualification
    if (getFlag(flags, M_POST_QUALIFIED)) {
      buf.append(CONCAT_STRING);
      if (declaringType != null) {
        getTypeLabel(declaringType, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
      } else {
        getFileLabel(elem, T_FULLY_QUALIFIED | flags & QUALIFIER_FLAGS, buf);
      }
    }
  }

  private static String[] getParameterNames(ExecutableElement function) {

    ParameterElement[] parameters = function.getParameters();
    String[] names = new String[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
      names[i] = parameters[i].getDisplayName();
    }

    return names;
  }

  private static String[] getParameterTypeNames(ExecutableElement function) {

    ParameterElement[] parameters = function.getParameters();
    String[] names = new String[parameters.length];

    com.google.dart.engine.type.Type type;
    for (int i = 0; i < parameters.length; i++) {
      type = parameters[i].getType();
      names[i] = type == null ? "var" : type.getDisplayName();
    }

    return names;
  }

  private static String getSuperTypeName(ClassElement type) {
    InterfaceType supertype = type.getSupertype();
    if (supertype != null) {
      return supertype.getDisplayName();
    }
    return null;
  }

  private static void getTypeLabel(ClassElement type, long flags, StringBuffer buf) {

    String typeName = type.getDisplayName();

    if (typeName.length() == 0) { // anonymous

      String supertypeName = getSuperTypeName(type);
      if (supertypeName != null) {
        typeName = Messages.format(DartUIMessages.JavaElementLabels_anonym_type, supertypeName);
      } else {
        typeName = DartUIMessages.JavaElementLabels_anonym;
      }

    }

    buf.append(typeName);
  }

  private static String getTypeName(FieldElement field) {
    com.google.dart.engine.type.Type type = field.getType();
    if (type != null) {
      return type.getDisplayName();
    }
    return null;
  }
}
