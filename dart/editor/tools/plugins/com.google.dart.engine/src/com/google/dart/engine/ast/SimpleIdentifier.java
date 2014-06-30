/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.AuxiliaryElements;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

/**
 * Instances of the class {@code SimpleIdentifier} represent a simple identifier.
 * 
 * <pre>
 * simpleIdentifier ::=
 *     initialCharacter internalCharacter*
 *
 * initialCharacter ::= '_' | '$' | letter
 *
 * internalCharacter ::= '_' | '$' | letter | digit
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class SimpleIdentifier extends Identifier {
  /**
   * The token representing the identifier.
   */
  private Token token;

  /**
   * The element associated with this identifier based on static type information, or {@code null}
   * if the AST structure has not been resolved or if this identifier could not be resolved.
   */
  private Element staticElement;

  /**
   * The element associated with this identifier based on propagated type information, or
   * {@code null} if the AST structure has not been resolved or if this identifier could not be
   * resolved.
   */
  private Element propagatedElement;

  /**
   * If this expression is both in a getter and setter context, the {@link AuxiliaryElements} will
   * be set to hold onto the static and propagated information. The auxiliary element will hold onto
   * the elements from the getter context.
   */
  private AuxiliaryElements auxiliaryElements = null;

  /**
   * Initialize a newly created identifier.
   * 
   * @param token the token representing the identifier
   */
  public SimpleIdentifier(Token token) {
    this.token = token;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitSimpleIdentifier(this);
  }

  /**
   * Get the auxiliary elements, this will be {@code null} if the node is not in a getter and setter
   * context, or if it is not yet fully resolved.
   */
  public AuxiliaryElements getAuxiliaryElements() {
    return auxiliaryElements;
  }

  @Override
  public Token getBeginToken() {
    return token;
  }

  @Override
  public Element getBestElement() {
    if (propagatedElement == null) {
      return staticElement;
    }
    return propagatedElement;
  }

  @Override
  public Token getEndToken() {
    return token;
  }

  @Override
  public String getName() {
    return token.getLexeme();
  }

  @Override
  public int getPrecedence() {
    return 16;
  }

  @Override
  public Element getPropagatedElement() {
    return propagatedElement;
  }

  @Override
  public Element getStaticElement() {
    return staticElement;
  }

  /**
   * Return the token representing the identifier.
   * 
   * @return the token representing the identifier
   */
  public Token getToken() {
    return token;
  }

  /**
   * Return {@code true} if this identifier is the name being declared in a declaration.
   * 
   * @return {@code true} if this identifier is the name being declared in a declaration
   */
  public boolean inDeclarationContext() {
    AstNode parent = getParent();
    if (parent instanceof CatchClause) {
      CatchClause clause = (CatchClause) parent;
      return this == clause.getExceptionParameter() || this == clause.getStackTraceParameter();
    } else if (parent instanceof ClassDeclaration) {
      return this == ((ClassDeclaration) parent).getName();
    } else if (parent instanceof ClassTypeAlias) {
      return this == ((ClassTypeAlias) parent).getName();
    } else if (parent instanceof ConstructorDeclaration) {
      return this == ((ConstructorDeclaration) parent).getName();
    } else if (parent instanceof DeclaredIdentifier) {
      return this == ((DeclaredIdentifier) parent).getIdentifier();
    } else if (parent instanceof FunctionDeclaration) {
      return this == ((FunctionDeclaration) parent).getName();
    } else if (parent instanceof FunctionTypeAlias) {
      return this == ((FunctionTypeAlias) parent).getName();
    } else if (parent instanceof Label) {
      return this == ((Label) parent).getLabel()
          && (parent.getParent() instanceof LabeledStatement);
    } else if (parent instanceof MethodDeclaration) {
      return this == ((MethodDeclaration) parent).getName();
    } else if (parent instanceof FunctionTypedFormalParameter
        || parent instanceof SimpleFormalParameter) {
      return this == ((NormalFormalParameter) parent).getIdentifier();
    } else if (parent instanceof TypeParameter) {
      return this == ((TypeParameter) parent).getName();
    } else if (parent instanceof VariableDeclaration) {
      return this == ((VariableDeclaration) parent).getName();
    }
    return false;
  }

  /**
   * Return {@code true} if this expression is computing a right-hand value.
   * <p>
   * Note that {@link #inGetterContext()} and {@link #inSetterContext()} are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return {@code true}
   * when invoked on the same node.
   * 
   * @return {@code true} if this expression is in a context where a getter will be invoked
   */
  public boolean inGetterContext() {
    AstNode parent = getParent();
    AstNode target = this;
    // skip prefix
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      if (prefixed.getPrefix() == this) {
        return true;
      }
      parent = prefixed.getParent();
      target = prefixed;
    } else if (parent instanceof PropertyAccess) {
      PropertyAccess access = (PropertyAccess) parent;
      if (access.getTarget() == this) {
        return true;
      }
      parent = access.getParent();
      target = access;
    }
    // skip label
    if (parent instanceof Label) {
      return false;
    }
    // analyze usage
    if (parent instanceof AssignmentExpression) {
      AssignmentExpression expr = (AssignmentExpression) parent;
      if (expr.getLeftHandSide() == target && expr.getOperator().getType() == TokenType.EQ) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return {@code true} if this expression is computing a left-hand value.
   * <p>
   * Note that {@link #inGetterContext()} and {@link #inSetterContext()} are not opposites, nor are
   * they mutually exclusive. In other words, it is possible for both methods to return {@code true}
   * when invoked on the same node.
   * 
   * @return {@code true} if this expression is in a context where a setter will be invoked
   */
  public boolean inSetterContext() {
    AstNode parent = getParent();
    AstNode target = this;
    // skip prefix
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      // if this is the prefix, then return false
      if (prefixed.getPrefix() == this) {
        return false;
      }
      parent = prefixed.getParent();
      target = prefixed;
    } else if (parent instanceof PropertyAccess) {
      PropertyAccess access = (PropertyAccess) parent;
      if (access.getTarget() == this) {
        return false;
      }
      parent = access.getParent();
      target = access;
    }
    // analyze usage
    if (parent instanceof PrefixExpression) {
      return ((PrefixExpression) parent).getOperator().getType().isIncrementOperator();
    } else if (parent instanceof PostfixExpression) {
      return true;
    } else if (parent instanceof AssignmentExpression) {
      return ((AssignmentExpression) parent).getLeftHandSide() == target;
    }
    return false;
  }

  /**
   * Returns {@code true} if this identifier is the "name" part of a prefixed identifier or a method
   * invocation.
   * 
   * @return {@code true} if this identifier is the "name" part of a prefixed identifier or a method
   *         invocation
   */
  public boolean isQualified() {
    AstNode parent = getParent();
    if (parent instanceof PrefixedIdentifier) {
      return ((PrefixedIdentifier) parent).getIdentifier() == this;
    }
    if (parent instanceof PropertyAccess) {
      return ((PropertyAccess) parent).getPropertyName() == this;
    }
    if (parent instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) parent;
      return invocation.getMethodName() == this && invocation.getRealTarget() != null;
    }
    return false;
  }

  @Override
  public boolean isSynthetic() {
    return token.isSynthetic();
  }

  /**
   * Set the auxiliary elements.
   */
  public void setAuxiliaryElements(AuxiliaryElements auxiliaryElements) {
    this.auxiliaryElements = auxiliaryElements;
  }

  /**
   * Set the element associated with this identifier based on propagated type information to the
   * given element.
   * 
   * @param element the element to be associated with this identifier
   */
  public void setPropagatedElement(Element element) {
    propagatedElement = validateElement(element);
  }

  /**
   * Set the element associated with this identifier based on static type information to the given
   * element.
   * 
   * @param element the element to be associated with this identifier
   */
  public void setStaticElement(Element element) {
    staticElement = validateElement(element);
  }

  /**
   * Set the token representing the identifier to the given token.
   * 
   * @param token the token representing the literal
   */
  public void setToken(Token token) {
    this.token = token;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    // There are no children to visit.
  }

  /**
   * Return the given element if it is valid, or report the problem and return {@code null} if it is
   * not appropriate.
   * 
   * @param parent the parent of the element, used for reporting when there is a problem
   * @param isValid {@code true} if the element is appropriate
   * @param element the element to be associated with this identifier
   * @return the element to be associated with this identifier
   */
  private Element returnOrReportElement(AstNode parent, boolean isValid, Element element) {
    if (!isValid) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Internal error: attempting to set the name of a " + parent.getClass().getName()
              + " to a " + element.getClass().getName(),
          new Exception());
      return null;
    }
    return element;
  }

  /**
   * Return the given element if it is an appropriate element based on the parent of this
   * identifier, or {@code null} if it is not appropriate.
   * 
   * @param element the element to be associated with this identifier
   * @return the element to be associated with this identifier
   */
  private Element validateElement(Element element) {
    if (element == null) {
      return null;
    }
    AstNode parent = getParent();
    if (parent instanceof ClassDeclaration && ((ClassDeclaration) parent).getName() == this) {
      return returnOrReportElement(parent, element instanceof ClassElement, element);
    } else if (parent instanceof ClassTypeAlias && ((ClassTypeAlias) parent).getName() == this) {
      return returnOrReportElement(parent, element instanceof ClassElement, element);
    } else if (parent instanceof DeclaredIdentifier
        && ((DeclaredIdentifier) parent).getIdentifier() == this) {
      return returnOrReportElement(parent, element instanceof LocalVariableElement, element);
    } else if (parent instanceof FormalParameter
        && ((FormalParameter) parent).getIdentifier() == this) {
      return returnOrReportElement(parent, element instanceof ParameterElement, element);
    } else if (parent instanceof FunctionDeclaration
        && ((FunctionDeclaration) parent).getName() == this) {
      return returnOrReportElement(parent, element instanceof ExecutableElement, element);
    } else if (parent instanceof FunctionTypeAlias
        && ((FunctionTypeAlias) parent).getName() == this) {
      return returnOrReportElement(parent, element instanceof FunctionTypeAliasElement, element);
    } else if (parent instanceof MethodDeclaration
        && ((MethodDeclaration) parent).getName() == this) {
      return returnOrReportElement(parent, element instanceof ExecutableElement, element);
    } else if (parent instanceof TypeParameter && ((TypeParameter) parent).getName() == this) {
      return returnOrReportElement(parent, element instanceof TypeParameterElement, element);
    } else if (parent instanceof VariableDeclaration
        && ((VariableDeclaration) parent).getName() == this) {
      return returnOrReportElement(parent, element instanceof VariableElement, element);
    }
    return element;
  }
}
