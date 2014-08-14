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
import com.google.dart.tools.mock.ui.StubUtility;

import org.eclipse.core.runtime.CoreException;

/**
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class CodeGeneration {

  /**
   * Returns the content of the body for a method or constructor using the method body templates.
   * <code>null</code> is returned if the template is empty.
   * <p>
   * The returned string is unformatted and not indented.
   * 
   * @param cu The compilation unit to which the method belongs. The compilation unit does not need
   *          to exist.
   * @param declaringTypeName Name of the type to which the method belongs. For inner types the name
   *          must be qualified and include the outer types names (dot separated). See
   *          {@link org.eclipse.Type.jsdt.core.IType#getTypeQualifiedName(char)} .
   * @param methodName Name of the method.
   * @param isConstructor Defines if the created body is for a constructor.
   * @param bodyStatement The code to be entered at the place of the variable ${body_statement}.
   * @param lineDelimiter The line delimiter to be used.
   * @return Returns the constructed body content or <code>null</code> if the comment code template
   *         is empty. The returned string is unformatted and and has no indent (formatting
   *         required).
   * @throws CoreException Thrown when the evaluation of the code template fails.
   */
  public static String getMethodBodyContent(CompilationUnit cu, String declaringTypeName,
      String methodName, boolean isConstructor, String bodyStatement, String lineDelimiter)
      throws CoreException {
    return StubUtility.getMethodBodyContent(
        isConstructor,
        cu.getDartProject(),
        declaringTypeName,
        methodName,
        bodyStatement,
        lineDelimiter);
  }

//  /**
//   * Returns the comment for a method or constructor using the comment code
//   * templates (constructor / method / overriding method). <code>null</code> is
//   * returned if the template is empty.
//   * <p>
//   * The returned string is unformatted and not indented.
//   * 
//   * @param method The method to be documented. The method must exist.
//   * @param overridden The method that will be overridden by the created method
//   *          or <code>null</code> for non-overriding methods. If not
//   *          <code>null</code>, the method must exist.
//   * @param lineDelimiter The line delimiter to be used.
//   * @return Returns the constructed comment or <code>null</code> if the comment
//   *         code template is empty. The returned string is unformatted and and
//   *         has no indent (formatting required).
//   * @throws CoreException Thrown when the evaluation of the code template
//   *           fails.
//   */
//  public static String getMethodComment(IFunction method, IFunction overridden,
//      String lineDelimiter) throws CoreException {
//    String retType = method.getReturnType();
//    String[] paramNames = method.getParameterNames();
//
//    String typeName = (method.getDeclaringType() != null)
//        ? method.getDeclaringType().getElementName() : ""; //$NON-NLS-1$
//    return StubUtility.getMethodComment(method.getJavaScriptUnit(), typeName,
//        method.getElementName(), paramNames, new String[0], retType,
//        overridden, false, lineDelimiter);
//  }
//
//  /**
//   * Returns the comment for a setter method using the setter method body
//   * template. <code>null</code> is returned if the template is empty.
//   * <p>
//   * The returned string is unformatted and not indented.
//   * 
//   * @param cu The compilation unit to which the method belongs. The compilation
//   *          unit does not need to exist.
//   * @param declaringTypeName Name of the type to which the method belongs. For
//   *          inner types the name must be qualified and include the outer types
//   *          names (dot separated). See
//   *          {@link org.eclipse.Type.jsdt.core.IType#getTypeQualifiedName(char)}
//   *          .
//   * @param methodName Name of the method.
//   * @param fieldName Name of the field that is set.
//   * @param fieldType The type of the field that is to set.
//   * @param paramName The name of the parameter that used to set.
//   * @param bareFieldName The field name without prefix or suffix.
//   * @param lineDelimiter The line delimiter to be used.
//   * @return Returns the generated setter comment or <code>null</code> if the
//   *         code template is empty. The returned comment is not indented.
//   * @throws CoreException Thrown when the evaluation of the code template
//   *           fails.
//   */
//  public static String getSetterComment(CompilationUnit cu,
//      String declaringTypeName, String methodName, String fieldName,
//      String fieldType, String paramName, String bareFieldName,
//      String lineDelimiter) throws CoreException {
//    return StubUtility.getSetterComment(cu, declaringTypeName, methodName,
//        fieldName, fieldType, paramName, bareFieldName, lineDelimiter);
//  }
//
//  /**
//   * Returns the content of body for a setter method using the setter method
//   * body template. <code>null</code> is returned if the template is empty.
//   * <p>
//   * The returned string is unformatted and not indented.
//   * 
//   * @param cu The compilation unit to which the method belongs. The compilation
//   *          unit does not need to exist.
//   * @param declaringTypeName Name of the type to which the method belongs. For
//   *          inner types the name must be qualified and include the outer types
//   *          names (dot separated). See
//   *          {@link org.eclipse.Type.jsdt.core.IType#getTypeQualifiedName(char)}
//   *          .
//   * @param methodName The name of the setter method.
//   * @param fieldName The name of the field to be set in the setter method,
//   *          corresponding to the template variable for ${field}.
//   * @param paramName The name of the parameter passed to the setter method,
//   *          corresponding to the template variable for $(param).
//   * @param lineDelimiter The line delimiter to be used.
//   * @return Returns the constructed body content or <code>null</code> if the
//   *         comment code template is empty. The returned string is unformatted
//   *         and and has no indent (formatting required).
//   * @throws CoreException Thrown when the evaluation of the code template
//   *           fails.
//   */
//  public static String getSetterMethodBodyContent(CompilationUnit cu,
//      String declaringTypeName, String methodName, String fieldName,
//      String paramName, String lineDelimiter) throws CoreException {
//    return StubUtility.getSetterMethodBodyContent(cu.getDartProject(),
//        declaringTypeName, methodName, fieldName, paramName, lineDelimiter);
//  }
//
//  /**
//   * Returns the content of a new new type body using the 'type body' code
//   * templates. The returned content is unformatted and is not indented.
//   * 
//   * @param typeKind The type kind ID of the body template. Valid values are
//   *          {@link #CLASS_BODY_TEMPLATE_ID},
//   *          {@link #INTERFACE_BODY_TEMPLATE_ID},
//   *          {@link #ENUM_BODY_TEMPLATE_ID} and
//   *          {@link #ANNOTATION_BODY_TEMPLATE_ID}.
//   * @param cu The compilation unit where the type is contained. The compilation
//   *          unit does not need to exist.
//   * @param typeName The name of the type(for embedding in the template as a
//   *          user variable).
//   * @param lineDelim The line delimiter to be used.
//   * @return Returns the new content or <code>null</code> if the code template
//   *         is undefined or empty. The returned content is unformatted and is
//   *         not indented.
//   * @throws CoreException Thrown when the evaluation of the code template
//   *           fails.
//   */
//  public static String getTypeBody(String typeKind, CompilationUnit cu,
//      String typeName, String lineDelim) throws CoreException {
//    return StubUtility.getTypeBody(typeKind, cu, typeName, lineDelim);
//  }
//
//  /**
//   * Returns the content for a new type comment using the 'type comment' code
//   * template. The returned content is unformatted and is not indented.
//   * 
//   * @param cu The compilation unit where the type is contained. The compilation
//   *          unit does not need to exist.
//   * @param typeQualifiedName The name of the type to which the comment is
//   *          added. For inner types the name must be qualified and include the
//   *          outer types names (dot separated). See
//   *          {@link org.eclipse.Type.jsdt.core.IType#getTypeQualifiedName(char)}
//   *          .
//   * @param lineDelimiter The line delimiter to be used.
//   * @return Returns the new content or <code>null</code> if the code template
//   *         is undefined or empty. The returned content is unformatted and is
//   *         not indented.
//   * @throws CoreException Thrown when the evaluation of the code template
//   *           fails.
//   */
//  public static String getTypeComment(CompilationUnit cu,
//      String typeQualifiedName, String lineDelimiter) throws CoreException {
//    return StubUtility.getTypeComment(cu, typeQualifiedName, lineDelimiter);
//  }

  private CodeGeneration() {
  }
}
