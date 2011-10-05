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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;

/**
 * Semantic highlightings
 */
public class SemanticHighlightings {

//  /**
//   * Semantic highlighting for deprecated members.
//   */
//  private static final class DeprecatedMemberHighlighting extends
//      SemanticHighlighting {
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
//     * .dart.tools.ui.editor.SemanticToken)
//     */
//    public boolean consumes(SemanticToken token) {
//      IBinding binding = getMethodBinding(token);
//      return binding != null ? binding.isDeprecated() : false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor
//     * ()
//     */
//    public RGB getDefaultDefaultTextColor() {
//      return new RGB(0, 0, 0);
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName()
//     */
//    public String getDisplayName() {
//      return DartEditorMessages.SemanticHighlighting_deprecatedMember;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey()
//     */
//    public String getPreferenceKey() {
//      return DEPRECATED_MEMBER;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold
//     * ()
//     */
//    public boolean isBoldByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault()
//     */
//    public boolean isEnabledByDefault() {
//      return true;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault()
//     */
//    public boolean isItalicByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isStrikethroughByDefault
//     * ()
//     */
//    public boolean isStrikethroughByDefault() {
//      return true;
//    }
//
//    /**
//     * Extracts the method binding from the token's simple name. The method
//     * binding is either the token's binding (if the parent of token is a method
//     * call or declaration) or the constructor binding of a class instance
//     * creation if the node is the type name of a class instance creation.
//     * 
//     * @param token the token to extract the method binding from
//     * @return the corresponding method binding, or <code>null</code>
//     */
//    private IBinding getMethodBinding(SemanticToken token) {
//      IBinding binding = null;
//      // work around: https://bugs.eclipse.org/bugs/show_bug.cgi?id=62605
//      DartNode node = token.getNode();
//      DartNode parent = node.getParent();
//      while (isTypePath(node, parent)) {
//        node = parent;
//        parent = parent.getParent();
//      }
//
//      if (parent != null
//          && PropertyDescriptorHelper.getLocationInParent(node) == ClassInstanceCreation.TYPE_PROPERTY)
//        binding = ((DartNewExpression) parent).resolveConstructorBinding();
//      else
//        binding = token.getBinding();
//      return binding;
//    }
//
//    /**
//     * Returns <code>true</code> if the given child/parent nodes are valid sub
//     * nodes of a <code>Type</code> DartNode.
//     * 
//     * @param child the child node
//     * @param parent the parent node
//     * @return <code>true</code> if the nodes may be the sub nodes of a type
//     *         node, false otherwise
//     */
//    private boolean isTypePath(DartNode child, DartNode parent) {
//      // TODO(brianwilkerson) "Type" should probably be "DartClass".
//      if (parent instanceof Type) {
//        StructuralPropertyDescriptor location = PropertyDescriptorHelper.getLocationInParent(child);
//        return location == SimpleType.NAME_PROPERTY;
//      }
////      else if (parent instanceof QualifiedName) {
////        StructuralPropertyDescriptor location = PropertyDescriptorHelper.getLocationInParent(child);
////        return location == QualifiedName.NAME_PROPERTY;
////      }
//      return false;
//    }
//  }
//
//  /**
//   * Semantic highlighting for local variable declarations.
//   */
//  private static final class LocalVariableDeclarationHighlighting extends
//      SemanticHighlighting {
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
//     * .dart.tools.ui.editor.SemanticToken)
//     */
//    public boolean consumes(SemanticToken token) {
//      DartIdentifier node = token.getNode();
//      StructuralPropertyDescriptor location = PropertyDescriptorHelper.getLocationInParent(node);
//      if (location == VariableDeclarationFragment.NAME_PROPERTY
//          || location == SingleVariableDeclaration.NAME_PROPERTY) {
//        DartNode parent = node.getParent();
//        if (parent instanceof VariableDeclaration) {
//          parent = parent.getParent();
//          return parent == null || !(parent instanceof DartFieldDefinition);
//        }
//      }
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor
//     * ()
//     */
//    public RGB getDefaultDefaultTextColor() {
//      return new RGB(0, 0, 0);
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName()
//     */
//    public String getDisplayName() {
//      return DartEditorMessages.SemanticHighlighting_localVariableDeclaration;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey()
//     */
//    public String getPreferenceKey() {
//      return LOCAL_VARIABLE_DECLARATION;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold
//     * ()
//     */
//    public boolean isBoldByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault()
//     */
//    public boolean isEnabledByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault()
//     */
//    public boolean isItalicByDefault() {
//      return false;
//    }
//  }
//
//  /**
//   * Semantic highlighting for local variables.
//   */
//  private static final class LocalVariableHighlighting extends
//      SemanticHighlighting {
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
//     * .dart.tools.ui.editor.SemanticToken)
//     */
//    public boolean consumes(SemanticToken token) {
//      IBinding binding = token.getBinding();
//      if (binding != null && binding.getKind() == IBinding.VARIABLE
//          && !((IVariableBinding) binding).isField()) {
//        DartNode decl = token.getRoot().findDeclaringNode(binding);
//        return decl instanceof VariableDeclaration;
//      }
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor
//     * ()
//     */
//    public RGB getDefaultDefaultTextColor() {
//      return new RGB(0, 0, 0);
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName()
//     */
//    public String getDisplayName() {
//      return DartEditorMessages.SemanticHighlighting_localVariable;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey()
//     */
//    public String getPreferenceKey() {
//      return LOCAL_VARIABLE;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold
//     * ()
//     */
//    public boolean isBoldByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault()
//     */
//    public boolean isEnabledByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault()
//     */
//    public boolean isItalicByDefault() {
//      return false;
//    }
//  }
//
//  /**
//   * Semantic highlighting for auto(un)boxed expressions.
//   */
//  /*
//   * private static final class AutoboxHighlighting extends SemanticHighlighting
//   * {
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey()
//   * public String getPreferenceKey() { return AUTOBOXING; } public RGB
//   * getDefaultDefaultTextColor() { return new RGB(171, 48, 0); } /*
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold
//   * () public boolean isBoldByDefault() { return false; }
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault()
//   * public boolean isItalicByDefault() { return false; }
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault()
//   * public boolean isEnabledByDefault() { return false; }
//   * 
//   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName()
//   * public String getDisplayName() { return
//   * DartEditorMessages.SemanticHighlighting_autoboxing; }
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.SemanticHighlighting#consumesLiteral(com
//   * .google.dart.tools.ui.editor.SemanticToken) public boolean
//   * consumesLiteral(SemanticToken token) { return
//   * isAutoUnBoxing(token.getLiteral()); }
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
//   * .dart.tools.ui.editor.SemanticToken) public boolean consumes(SemanticToken
//   * token) { return isAutoUnBoxing(token.getNode()); } private boolean
//   * isAutoUnBoxing(Expression node) { if (isAutoUnBoxingExpression(node))
//   * return true; // special cases: the autoboxing conversions happens at a //
//   * location that is not mapped directly to a simple name // or a literal, but
//   * can still be mapped somehow // A) expressions StructuralPropertyDescriptor
//   * desc= node.getLocationInParent(); if (desc == ArrayAccess.ARRAY_PROPERTY ||
//   * desc == InfixExpression.LEFT_OPERAND_PROPERTY || desc ==
//   * InfixExpression.RIGHT_OPERAND_PROPERTY || desc ==
//   * ConditionalExpression.THEN_EXPRESSION_PROPERTY || desc ==
//   * PrefixExpression.OPERAND_PROPERTY || desc ==
//   * CastExpression.EXPRESSION_PROPERTY || desc ==
//   * ConditionalExpression.ELSE_EXPRESSION_PROPERTY) { DartNode parent=
//   * node.getParent(); if (parent instanceof Expression) return
//   * isAutoUnBoxingExpression((Expression) parent); } // B) constructor
//   * invocations if (desc == SimpleType.NAME_PROPERTY || desc ==
//   * QualifiedType.NAME_PROPERTY) { DartNode parent= node.getParent(); if
//   * (parent != null && parent.getLocationInParent() ==
//   * ClassInstanceCreation.TYPE_PROPERTY) { parent= parent.getParent(); if
//   * (parent instanceof Expression) return isAutoUnBoxingExpression((Expression)
//   * parent); } } return false; } private boolean
//   * isAutoUnBoxingExpression(Expression expression) { return
//   * expression.resolveBoxing() || expression.resolveUnboxing(); } }
//   */
//  /**
//   * Semantic highlighting for method declarations.
//   */
//  private static final class MethodDeclarationHighlighting extends
//      SemanticHighlighting {
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#isMatched(org.eclipse
//     * .wst.jsdt.core.dom.ASTNode)
//     */
//    public boolean consumes(SemanticToken token) {
//      StructuralPropertyDescriptor location = PropertyDescriptorHelper.getLocationInParent(token.getNode());
//      return location == FunctionDeclaration.NAME_PROPERTY;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor
//     * ()
//     */
//    public RGB getDefaultDefaultTextColor() {
//      return new RGB(0, 0, 0);
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName()
//     */
//    public String getDisplayName() {
//      return DartEditorMessages.SemanticHighlighting_methodDeclaration;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey()
//     */
//    public String getPreferenceKey() {
//      return METHOD_DECLARATION;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold
//     * ()
//     */
//    public boolean isBoldByDefault() {
//      return true;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault()
//     */
//    public boolean isEnabledByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault()
//     */
//    public boolean isItalicByDefault() {
//      return false;
//    }
//  }
//
//  /**
//   * Semantic highlighting for inherited method invocations.
//   */
//  private static final class MethodHighlighting extends SemanticHighlighting {
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#isMatched(org.eclipse
//     * .wst.jsdt.core.dom.ASTNode)
//     */
//    public boolean consumes(SemanticToken token) {
//      IBinding binding = getMethodBinding(token);
//      return binding != null && binding.getKind() == IBinding.METHOD;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor
//     * ()
//     */
//    public RGB getDefaultDefaultTextColor() {
//      return new RGB(0, 0, 0);
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName()
//     */
//    public String getDisplayName() {
//      return DartEditorMessages.SemanticHighlighting_method;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey()
//     */
//    public String getPreferenceKey() {
//      return METHOD;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold
//     * ()
//     */
//    public boolean isBoldByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault()
//     */
//    public boolean isEnabledByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault()
//     */
//    public boolean isItalicByDefault() {
//      return false;
//    }
//
//    /**
//     * Extracts the method binding from the token's simple name. The method
//     * binding is either the token's binding (if the parent of token is a method
//     * call or declaration) or the constructor binding of a class instance
//     * creation if the node is the type name of a class instance creation.
//     * 
//     * @param token the token to extract the method binding from
//     * @return the corresponding method binding, or <code>null</code>
//     */
//    private IBinding getMethodBinding(SemanticToken token) {
//      IBinding binding = null;
//      // work around: https://bugs.eclipse.org/bugs/show_bug.cgi?id=62605
//      DartNode node = token.getNode();
//      DartNode parent = node.getParent();
//      while (isTypePath(node, parent)) {
//        node = parent;
//        parent = parent.getParent();
//      }
//
//      if (parent != null
//          && PropertyDescriptorHelper.getLocationInParent(node) == ClassInstanceCreation.TYPE_PROPERTY)
//        binding = ((DartNewExpression) parent).resolveConstructorBinding();
//      else
//        binding = token.getBinding();
//      return binding;
//    }
//
//    /**
//     * Returns <code>true</code> if the given child/parent nodes are valid sub
//     * nodes of a <code>Type</code> DartNode.
//     * 
//     * @param child the child node
//     * @param parent the parent node
//     * @return <code>true</code> if the nodes may be the sub nodes of a type
//     *         node, false otherwise
//     */
//    private boolean isTypePath(DartNode child, DartNode parent) {
//      if (parent instanceof Type) {
//        StructuralPropertyDescriptor location = PropertyDescriptorHelper.getLocationInParent(child);
//        return location == SimpleType.NAME_PROPERTY;
//      }
//      DartX.todo();
////      else if (parent instanceof QualifiedName) {
////        StructuralPropertyDescriptor location = PropertyDescriptorHelper.getLocationInParent(child);
////        return location == QualifiedName.NAME_PROPERTY;
////      }
//      return false;
//    }
//  }
//
//  /**
//   * Semantic highlighting for fields.
//   */
//  /*
//   * private static final class FieldHighlighting extends SemanticHighlighting {
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey()
//   * public String getPreferenceKey() { return FIELD; }
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor()
//   * public RGB getDefaultTextColor() { return new RGB(0, 0, 192); }
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold
//   * () public boolean isBoldByDefault() { return false; }
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault()
//   * public boolean isItalicByDefault() { return false; }
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault()
//   * public boolean isEnabledByDefault() { return true; }
//   * 
//   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName()
//   * public String getDisplayName() { return
//   * DartEditorMessages.SemanticHighlighting_field; }
//   * 
//   * @see
//   * com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
//   * .dart.tools.ui.editor.SemanticToken) public boolean consumes(SemanticToken
//   * token) { IBinding binding= token.getBinding(); return binding != null &&
//   * binding.getKind() == IBinding.VARIABLE &&
//   * ((IVariableBinding)binding).isField(); } }
//   */
//  /**
//   * Semantic highlighting for fields.
//   * 
//   * @author STP
//   */
//  private static final class ObjectInitializerHighlighting extends
//      SemanticHighlighting {
//
//    public boolean consumes(SemanticToken token) {
//      DartIdentifier node = token.getNode();
//      StructuralPropertyDescriptor location = PropertyDescriptorHelper.getLocationInParent(node);
//      if (location == ObjectLiteralField.FIELD_NAME_PROPERTY) {
//        DartNode parent = node.getParent();
//        return (parent != null && parent instanceof ObjectLiteral);
//      }
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor
//     * ()
//     */
//    public RGB getDefaultDefaultTextColor() {
//      return new RGB(0, 0, 0);
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName()
//     */
//    public String getDisplayName() {
//      return DartEditorMessages.SemanticHighlighting_objectInitializer;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey()
//     */
//    public String getPreferenceKey() {
//      return OBJECT_INITIALIZER;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold
//     * ()
//     */
//    public boolean isBoldByDefault() {
//      return true;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault()
//     */
//    public boolean isEnabledByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
//     * .dart.tools.ui.editor.SemanticToken)
//     */
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault()
//     */
//    public boolean isItalicByDefault() {
//      return false;
//    }
//  }
//
//  /**
//   * Semantic highlighting for parameter variables.
//   */
//  private static final class ParameterVariableHighlighting extends
//      SemanticHighlighting {
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
//     * .dart.tools.ui.editor.SemanticToken)
//     */
//    public boolean consumes(SemanticToken token) {
//      IBinding binding = token.getBinding();
//      if (binding != null && binding.getKind() == IBinding.VARIABLE
//          && !((IVariableBinding) binding).isField()) {
//        DartNode decl = token.getRoot().findDeclaringNode(binding);
//        return decl != null
//            && PropertyDescriptorHelper.getLocationInParent(decl) == FunctionDeclaration.PARAMETERS_PROPERTY;
//      }
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor
//     * ()
//     */
//    public RGB getDefaultDefaultTextColor() {
//      return new RGB(0, 0, 0);
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName()
//     */
//    public String getDisplayName() {
//      return DartEditorMessages.SemanticHighlighting_parameterVariable;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey()
//     */
//    public String getPreferenceKey() {
//      return PARAMETER_VARIABLE;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold
//     * ()
//     */
//    public boolean isBoldByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault()
//     */
//    public boolean isEnabledByDefault() {
//      return false;
//    }
//
//    /*
//     * @see
//     * com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault()
//     */
//    public boolean isItalicByDefault() {
//      return false;
//    }
//  }

  /**
   * A named preference part that controls the highlighting of static final fields.
   */
  public static final String STATIC_FINAL_FIELD = "staticFinalField"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of static fields.
   */
  public static final String STATIC_FIELD = "staticField"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of fields.
   */
  public static final String FIELD = "field"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of method declarations.
   */
  public static final String METHOD_DECLARATION = "methodDeclarationName"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of static method invocations.
   */
  public static final String STATIC_METHOD_INVOCATION = "staticMethodInvocation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of inherited method invocations.
   */
  public static final String INHERITED_METHOD_INVOCATION = "inheritedMethodInvocation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of annotation element references.
   */
  public static final String ANNOTATION_ELEMENT_REFERENCE = "annotationElementReference"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of abstract method invocations.
   */
  public static final String ABSTRACT_METHOD_INVOCATION = "abstractMethodInvocation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of local variables.
   */
  public static final String LOCAL_VARIABLE_DECLARATION = "localVariableDeclaration"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of local variables.
   */
  public static final String LOCAL_VARIABLE = "localVariable"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of parameter variables.
   */
  public static final String PARAMETER_VARIABLE = "parameterVariable"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of deprecated members.
   */
  public static final String DEPRECATED_MEMBER = "deprecatedMember"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of type parameters.
   */
  public static final String TYPE_VARIABLE = "typeParameter"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of type parameters.
   * 
   * @author STP
   */
  public static final String OBJECT_INITIALIZER = "objectInitializer"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of methods (invocations and
   * declarations).
   */
  public static final String METHOD = "method"; //$NON-NLS-1$

  /**
   * Semantic highlighting for static final fields.
   */
  /*
   * private static final class StaticFinalFieldHighlighting extends SemanticHighlighting {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return STATIC_FINAL_FIELD; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor() public RGB
   * getDefaultTextColor() { return new RGB(0, 0, 0); }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_staticFinalField; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
   * .dart.tools.ui.editor.SemanticToken) public boolean consumes(SemanticToken token) { IBinding
   * binding= token.getBinding(); return binding != null && binding.getKind() == IBinding.VARIABLE
   * && ((IVariableBinding)binding).isField() && (binding.getModifiers() & (Modifier.FINAL |
   * Modifier.STATIC)) == (Modifier.FINAL | Modifier.STATIC); } }
   */

  /**
   * Semantic highlighting for static fields.
   */
  /*
   * private static final class StaticFieldHighlighting extends SemanticHighlighting {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return STATIC_FIELD; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor() public RGB
   * getDefaultTextColor() { return new RGB(0, 0, 192); }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return true; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return true; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_staticField; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
   * .dart.tools.ui.editor.SemanticToken) public boolean consumes(SemanticToken token) { IBinding
   * binding= token.getBinding(); return binding != null && binding.getKind() == IBinding.VARIABLE
   * && ((IVariableBinding)binding).isField() && (binding.getModifiers() & Modifier.STATIC) ==
   * Modifier.STATIC; } }
   */

  /**
   * A named preference part that controls the highlighting of auto(un)boxed expressions.
   */
  public static final String AUTOBOXING = "autoboxing"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of classes.
   */
  public static final String CLASS = "class"; //$NON-NLS-1$

  /**
   * Semantic highlighting for static method invocations.
   */
  /*
   * private static final class StaticMethodInvocationHighlighting extends SemanticHighlighting {
   * public String getPreferenceKey() { return STATIC_METHOD_INVOCATION; } public RGB
   * getDefaultDefaultTextColor() { return new RGB(0, 0, 0); } /*
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return true; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return true; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_staticMethodInvocation; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#isMatched(org.eclipse
   * .wst.jsdt.core.dom.ASTNode) public boolean consumes(SemanticToken token) { SimpleName node=
   * token.getNode(); if (node.isDeclaration()) return false; IBinding binding= token.getBinding();
   * return binding != null && binding.getKind() == IBinding.METHOD && (binding.getModifiers() &
   * Modifier.STATIC) == Modifier.STATIC; } }
   */

  /**
   * Semantic highlighting for annotation element references.
   */
  /*
   * private static final class AnnotationElementReferenceHighlighting extends SemanticHighlighting
   * {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return ANNOTATION_ELEMENT_REFERENCE; } public RGB
   * getDefaultDefaultTextColor() { return new RGB(0, 0, 0); } /*
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_annotationElementReference; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#isMatched(org.eclipse
   * .wst.jsdt.core.dom.ASTNode) public boolean consumes(SemanticToken token) { SimpleName node=
   * token.getNode(); if (node.getParent() instanceof MemberValuePair) { IBinding binding=
   * token.getBinding(); boolean isAnnotationElement= binding != null && binding.getKind() ==
   * IBinding.METHOD; return isAnnotationElement; } return false; } }
   */

  /**
   * Semantic highlighting for abstract method invocations.
   */
  /*
   * private static final class AbstractMethodInvocationHighlighting extends SemanticHighlighting {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return ABSTRACT_METHOD_INVOCATION; } public RGB
   * getDefaultDefaultTextColor() { return new RGB(0, 0, 0); } /*
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_abstractMethodInvocation; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#isMatched(org.eclipse
   * .wst.jsdt.core.dom.ASTNode) public boolean consumes(SemanticToken token) { SimpleName node=
   * token.getNode(); if (node.isDeclaration()) return false; IBinding binding= token.getBinding();
   * boolean isAbstractMethod= binding != null && binding.getKind() == IBinding.METHOD &&
   * (binding.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT; if (!isAbstractMethod)
   * return false; // filter out annotation value references if (binding != null) { ITypeBinding
   * declaringType= ((IFunctionBinding)binding).getDeclaringClass(); if
   * (declaringType.isAnnotation()) return false; } return true; } }
   */

  /**
   * Semantic highlighting for inherited method invocations.
   */
  /*
   * private static final class InheritedMethodInvocationHighlighting extends SemanticHighlighting {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return INHERITED_METHOD_INVOCATION; } public RGB
   * getDefaultDefaultTextColor() { return new RGB(0, 0, 0); } /*
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_inheritedMethodInvocation; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#isMatched(org.eclipse
   * .wst.jsdt.core.dom.ASTNode) public boolean consumes(SemanticToken token) { SimpleName node=
   * token.getNode(); if (node.isDeclaration()) return false; IBinding binding= token.getBinding();
   * if (binding == null || binding.getKind() != IBinding.METHOD) return false; ITypeBinding
   * currentType= Bindings.getBindingOfParentType(node); ITypeBinding declaringType=
   * ((IFunctionBinding) binding).getDeclaringClass(); if (currentType == declaringType ||
   * currentType == null) return false; return Bindings.isSuperType(declaringType, currentType); } }
   */

  /**
   * A named preference part that controls the highlighting of enums.
   */
  public static final String ENUM = "enum"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of interfaces.
   */
  public static final String INTERFACE = "interface"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of annotations.
   */
  public static final String ANNOTATION = "annotation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of type arguments.
   */
  public static final String TYPE_ARGUMENT = "typeArgument"; //$NON-NLS-1$

  /**
   * Semantic highlightings
   */
  private static SemanticHighlighting[] fgSemanticHighlightings;

  /**
   * Semantic highlighting for type variables.
   */
  /*
   * private static final class TypeVariableHighlighting extends SemanticHighlighting {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return TYPE_VARIABLE; } public RGB getDefaultDefaultTextColor() { return
   * new RGB(100, 70, 50); } /*
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return true; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_typeVariables; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
   * .dart.tools.ui.editor.SemanticToken) public boolean consumes(SemanticToken token) { // 1: match
   * types in type parameter lists SimpleName name= token.getNode(); DartNode node=
   * name.getParent(); if (node.getNodeType() != DartNode.SIMPLE_TYPE && node.getNodeType() !=
   * DartNode.TYPE_PARAMETER) return false; // 2: match generic type variable references IBinding
   * binding= token.getBinding(); return binding instanceof ITypeBinding && ((ITypeBinding)
   * binding).isTypeVariable(); } }
   */

  /**
   * Semantic highlighting for classes.
   */
  /*
   * private static final class ClassHighlighting extends SemanticHighlighting {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return CLASS; } public RGB getDefaultDefaultTextColor() { return new
   * RGB(0, 80, 50); } /*
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_classes; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
   * .dart.tools.ui.editor.SemanticToken) public boolean consumes(SemanticToken token) { // 1: match
   * types SimpleName name= token.getNode(); DartNode node= name.getParent(); int nodeType=
   * node.getNodeType(); if (nodeType != DartNode.SIMPLE_TYPE && nodeType !=
   * DartNode.THIS_EXPRESSION && nodeType != DartNode.QUALIFIED_TYPE && nodeType !=
   * DartNode.QUALIFIED_NAME && nodeType != DartNode.TYPE_DECLARATION) return false; while (nodeType
   * == DartNode.QUALIFIED_NAME) { node= node.getParent(); nodeType= node.getNodeType(); if
   * (nodeType == DartNode.IMPORT_DECLARATION) return false; } // 2: match classes IBinding binding=
   * token.getBinding(); return binding instanceof ITypeBinding && ((ITypeBinding)
   * binding).isClass(); } }
   */
  /*
   * private static final class EnumHighlighting extends SemanticHighlighting {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return ENUM; } public RGB getDefaultDefaultTextColor() { return new
   * RGB(100, 70, 50); } /*
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_enums; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
   * .dart.tools.ui.editor.SemanticToken) public boolean consumes(SemanticToken token) { // 1: match
   * types SimpleName name= token.getNode(); DartNode node= name.getParent(); int nodeType=
   * node.getNodeType(); if (nodeType != DartNode.SIMPLE_TYPE && nodeType != DartNode.QUALIFIED_TYPE
   * && nodeType != DartNode.QUALIFIED_NAME && nodeType != DartNode.QUALIFIED_NAME && nodeType !=
   * DartNode.ENUM_DECLARATION) return false; while (nodeType == DartNode.QUALIFIED_NAME) { node=
   * node.getParent(); nodeType= node.getNodeType(); if (nodeType == DartNode.IMPORT_DECLARATION)
   * return false; } // 2: match enums IBinding binding= token.getBinding(); return binding
   * instanceof ITypeBinding && ((ITypeBinding) binding).isEnum(); } }
   */

  /**
   * Semantic highlighting for interfaces.
   */
  /*
   * private static final class InterfaceHighlighting extends SemanticHighlighting {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return INTERFACE; } public RGB getDefaultDefaultTextColor() { return new
   * RGB(50, 63, 112); } /*
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_interfaces; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
   * .dart.tools.ui.editor.SemanticToken) public boolean consumes(SemanticToken token) { // 1: match
   * types SimpleName name= token.getNode(); DartNode node= name.getParent(); int nodeType=
   * node.getNodeType(); if (nodeType != DartNode.SIMPLE_TYPE && nodeType != DartNode.QUALIFIED_TYPE
   * && nodeType != DartNode.QUALIFIED_NAME && nodeType != DartNode.TYPE_DECLARATION) return false;
   * while (nodeType == DartNode.QUALIFIED_NAME) { node= node.getParent(); nodeType=
   * node.getNodeType(); if (nodeType == DartNode.IMPORT_DECLARATION) return false; } // 2: match
   * interfaces IBinding binding= token.getBinding(); return binding instanceof ITypeBinding &&
   * ((ITypeBinding) binding).isInterface(); } }
   */

  /**
   * Semantic highlighting for annotation types.
   */
  /*
   * private static final class AnnotationHighlighting extends SemanticHighlighting {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return ANNOTATION; } public RGB getDefaultDefaultTextColor() { return new
   * RGB(100, 100, 100); } /*
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return true; // as it replaces the syntax based highlighting which is
   * always enabled }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_annotations; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
   * .dart.tools.ui.editor.SemanticToken) public boolean consumes(SemanticToken token) { // 1: match
   * types SimpleName name= token.getNode(); DartNode node= name.getParent(); int nodeType=
   * node.getNodeType(); if (nodeType != DartNode.SIMPLE_TYPE && nodeType != DartNode.QUALIFIED_TYPE
   * && nodeType != DartNode.QUALIFIED_NAME && nodeType != DartNode.ANNOTATION_TYPE_DECLARATION &&
   * nodeType != DartNode.MARKER_ANNOTATION && nodeType != DartNode.NORMAL_ANNOTATION && nodeType !=
   * DartNode.SINGLE_MEMBER_ANNOTATION) return false; while (nodeType == DartNode.QUALIFIED_NAME) {
   * node= node.getParent(); nodeType= node.getNodeType(); if (nodeType ==
   * DartNode.IMPORT_DECLARATION) return false; } // 2: match annotations IBinding binding=
   * token.getBinding(); return binding instanceof ITypeBinding && ((ITypeBinding)
   * binding).isAnnotation(); } }
   */

  /**
   * Tests whether <code>event</code> in <code>store</code> affects the enablement of semantic
   * highlighting.
   * 
   * @param store the preference store where <code>event</code> was observed
   * @param event the property change under examination
   * @return <code>true</code> if <code>event</code> changed semantic highlighting enablement,
   *         <code>false</code> if it did not
   */
  public static boolean affectsEnablement(IPreferenceStore store, PropertyChangeEvent event) {
    String relevantKey = null;
    SemanticHighlighting[] highlightings = getSemanticHighlightings();
    for (int i = 0; i < highlightings.length; i++) {
      if (event.getProperty().equals(getEnabledPreferenceKey(highlightings[i]))) {
        relevantKey = event.getProperty();
        break;
      }
    }
    if (relevantKey == null) {
      return false;
    }

    for (int i = 0; i < highlightings.length; i++) {
      String key = getEnabledPreferenceKey(highlightings[i]);
      if (key.equals(relevantKey)) {
        continue;
      }
      if (store.getBoolean(key)) {
        return false; // another is still enabled or was enabled before
      }
    }

    // all others are disabled, so toggling relevantKey affects the enablement
    return true;
  }

  /*
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextStyleBold () public
   * boolean isBoldByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isItalicByDefault() public boolean
   * isItalicByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#isEnabledByDefault() public boolean
   * isEnabledByDefault() { return false; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDisplayName() public String
   * getDisplayName() { return DartEditorMessages.SemanticHighlighting_typeArguments; }
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#consumes(com.google
   * .dart.tools.ui.editor.SemanticToken) public boolean consumes(SemanticToken token) { // 1: match
   * types SimpleName name= token.getNode(); DartNode node= name.getParent(); int nodeType=
   * node.getNodeType(); if (nodeType != DartNode.SIMPLE_TYPE && nodeType !=
   * DartNode.QUALIFIED_TYPE) return false; // 2: match type arguments StructuralPropertyDescriptor
   * locationInParent= node.getLocationInParent(); if (locationInParent ==
   * ParameterizedType.TYPE_ARGUMENTS_PROPERTY) return true; return false; } }
   */

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * bold.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the bold preference key
   */
  public static String getBoldPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX;
  }

  /**
   * A named preference that controls the given semantic highlighting's color.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the color preference key
   */
  public static String getColorPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX;
  }

  /**
   * A named preference that controls if the given semantic highlighting is enabled.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the enabled preference key
   */
  public static String getEnabledPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * italic.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the italic preference key
   */
  public static String getItalicPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX;
  }

  /**
   * @return The semantic highlightings, the order defines the precedence of matches, the first
   *         match wins.
   */
  public static SemanticHighlighting[] getSemanticHighlightings() {
    if (fgSemanticHighlightings == null) {
      fgSemanticHighlightings = new SemanticHighlighting[] {
      // STP
//          new ObjectInitializerHighlighting(),
//          new DeprecatedMemberHighlighting(),
      // new AutoboxHighlighting(),
      // new StaticFinalFieldHighlighting(),
      // new StaticFieldHighlighting(),
      // new FieldHighlighting(),
//          new MethodDeclarationHighlighting(),
      // new StaticMethodInvocationHighlighting(),
      // new AbstractMethodInvocationHighlighting(),
      // new AnnotationElementReferenceHighlighting(),
      // new InheritedMethodInvocationHighlighting(),
//          new ParameterVariableHighlighting(),
//          new LocalVariableDeclarationHighlighting(),
//          new LocalVariableHighlighting(),
      // new TypeVariableHighlighting(), // before type arguments!
//          new MethodHighlighting(), // before types to get ctors
      // new TypeArgumentHighlighting(), // before other types
      // new ClassHighlighting(),
      // new EnumHighlighting(),
      // new AnnotationHighlighting(), // before interfaces
      // new InterfaceHighlighting(),
      };
    }
    return fgSemanticHighlightings;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * strikethrough.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the strikethrough preference key
   */
  public static String getStrikethroughPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_STRIKETHROUGH_SUFFIX;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * underline.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the underline preference key
   */
  public static String getUnderlinePreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_UNDERLINE_SUFFIX;
  }

  /**
   * Initialize default preferences in the given preference store.
   * 
   * @param store The preference store
   */
  public static void initDefaults(IPreferenceStore store) {
    SemanticHighlighting[] semanticHighlightings = getSemanticHighlightings();
    for (int i = 0, n = semanticHighlightings.length; i < n; i++) {
      SemanticHighlighting semanticHighlighting = semanticHighlightings[i];
      setDefaultAndFireEvent(store,
          SemanticHighlightings.getColorPreferenceKey(semanticHighlighting),
          semanticHighlighting.getDefaultTextColor());
      store.setDefault(SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting),
          semanticHighlighting.isBoldByDefault());
      store.setDefault(SemanticHighlightings.getItalicPreferenceKey(semanticHighlighting),
          semanticHighlighting.isItalicByDefault());
      store.setDefault(SemanticHighlightings.getStrikethroughPreferenceKey(semanticHighlighting),
          semanticHighlighting.isStrikethroughByDefault());
      store.setDefault(SemanticHighlightings.getUnderlinePreferenceKey(semanticHighlighting),
          semanticHighlighting.isUnderlineByDefault());
      store.setDefault(SemanticHighlightings.getEnabledPreferenceKey(semanticHighlighting),
          semanticHighlighting.isEnabledByDefault());
    }

    convertMethodHighlightingPreferences(store);
    convertAnnotationHighlightingPreferences(store);
  }

  /**
   * Tests whether semantic highlighting is currently enabled.
   * 
   * @param store the preference store to consult
   * @return <code>true</code> if semantic highlighting is enabled, <code>false</code> if it is not
   */
  public static boolean isEnabled(IPreferenceStore store) {
    SemanticHighlighting[] highlightings = getSemanticHighlightings();
    boolean enable = false;
    for (int i = 0; i < highlightings.length; i++) {
      String enabledKey = getEnabledPreferenceKey(highlightings[i]);
      if (store.getBoolean(enabledKey)) {
        enable = true;
        break;
      }
    }

    return enable;
  }

  /**
   * If the setting pointed to by <code>oldKey</code> is not the default setting, store that setting
   * under <code>newKey</code> and reset <code>oldKey</code> to its default setting.
   * <p>
   * Returns <code>true</code> if any changes were made.
   * </p>
   * 
   * @param store the preference store to read from and write to
   * @param oldKey the old preference key
   * @param newKey the new preference key
   * @return <code>true</code> if <code>store</code> was modified, <code>false</code> if not
   */
  private static boolean conditionalReset(IPreferenceStore store, String oldKey, String newKey) {
    if (!store.isDefault(oldKey)) {
      if (store.isDefault(newKey)) {
        store.setValue(newKey, store.getString(oldKey));
      }
      store.setToDefault(oldKey);
      return true;
    }
    return false;
  }

  /**
   * In 3.1, annotations were highlighted by a rule-based word matcher that matched any identifier
   * preceded by an '@' sign and possibly white space.
   * <p>
   * This does not work when there is a comment between the '@' and the annotation, results in stale
   * highlighting if there is a new line between the '@' and the annotation, and does not work for
   * highlighting annotation declarations. Because different preference key naming schemes are used,
   * we have to migrate the old settings to the new ones, which is done here. Nothing needs to be
   * done if the old settings were set to the default values.
   * </p>
   * 
   * @param store the preference store to migrate
   */
  private static void convertAnnotationHighlightingPreferences(IPreferenceStore store) {
    String colorkey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + ANNOTATION
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX;
    String boldkey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + ANNOTATION
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX;
    String italickey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + ANNOTATION
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX;
    String strikethroughKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + ANNOTATION
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_STRIKETHROUGH_SUFFIX;
    String underlineKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + ANNOTATION
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_UNDERLINE_SUFFIX;
    String enabledkey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + ANNOTATION
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;

    String oldColorkey = PreferenceConstants.EDITOR_JAVA_ANNOTATION_COLOR;
    String oldBoldkey = PreferenceConstants.EDITOR_JAVA_ANNOTATION_BOLD;
    String oldItalickey = PreferenceConstants.EDITOR_JAVA_ANNOTATION_ITALIC;
    String oldStrikethroughKey = PreferenceConstants.EDITOR_JAVA_ANNOTATION_STRIKETHROUGH;
    String oldUnderlineKey = PreferenceConstants.EDITOR_JAVA_ANNOTATION_UNDERLINE;

    if (conditionalReset(store, oldColorkey, colorkey)
        || conditionalReset(store, oldBoldkey, boldkey)
        || conditionalReset(store, oldItalickey, italickey)
        || conditionalReset(store, oldStrikethroughKey, strikethroughKey)
        || conditionalReset(store, oldUnderlineKey, underlineKey)) {
      store.setValue(enabledkey, true);
    }

  }

  /**
   * In 3.0, methods were highlighted by a rule-based word matcher that matched any identifier that
   * was followed by possibly white space and a left parenthesis.
   * <p>
   * With generics, this does not work any longer for constructors of generic types, and the
   * highlighting has been moved to be a semantic highlighting. Because different preference key
   * naming schemes are used, we have to migrate the old settings to the new ones, which is done
   * here. Nothing needs to be done if the old settings were set to the default values.
   * </p>
   * 
   * @param store the preference store to migrate
   */
  private static void convertMethodHighlightingPreferences(IPreferenceStore store) {
    String colorkey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + METHOD
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX;
    String boldkey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + METHOD
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX;
    String italickey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + METHOD
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX;
    String enabledkey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + METHOD
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;

    String oldColorkey = PreferenceConstants.EDITOR_JAVA_METHOD_NAME_COLOR;
    String oldBoldkey = PreferenceConstants.EDITOR_JAVA_METHOD_NAME_BOLD;
    String oldItalickey = PreferenceConstants.EDITOR_JAVA_METHOD_NAME_ITALIC;

    if (conditionalReset(store, oldColorkey, colorkey)
        || conditionalReset(store, oldBoldkey, boldkey)
        || conditionalReset(store, oldItalickey, italickey)) {
      store.setValue(enabledkey, true);
    }

  }

  /**
   * Sets the default value and fires a property change event if necessary.
   * 
   * @param store the preference store
   * @param key the preference key
   * @param newValue the new value
   */
  private static void setDefaultAndFireEvent(IPreferenceStore store, String key, RGB newValue) {
    RGB oldValue = null;
    if (store.isDefault(key)) {
      oldValue = PreferenceConverter.getDefaultColor(store, key);
    }

    PreferenceConverter.setDefault(store, key, newValue);

    if (oldValue != null && !oldValue.equals(newValue)) {
      store.firePropertyChangeEvent(key, oldValue, newValue);
    }
  }

  /**
   * Do not instantiate
   */
  private SemanticHighlightings() {
  }

  /**
   * Semantic highlighting for annotation types.
   */
  /*
   * private static final class TypeArgumentHighlighting extends SemanticHighlighting {
   * 
   * @see com.google.dart.tools.ui.editor.SemanticHighlighting#getPreferenceKey() public String
   * getPreferenceKey() { return TYPE_ARGUMENT; }
   * 
   * @see com.google.dart.tools.ui.editor.ISemanticHighlighting#getDefaultTextColor()
   */
  public RGB getDefaultDefaultTextColor() {
    return new RGB(13, 100, 0);
  }
}
