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
package com.google.dart.tools.core.utilities.ast;

import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartParameterizedTypeNode;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartRedirectConstructorInvocation;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.type.TypeKind;

/**
 * The class <code>DartAstUtilities</code> defines utility methods that operate on nodes in a Dart
 * AST structure (instances of {@link DartNode} and its subclasses).
 */
public class DartAstUtilities {
  /**
   * Get the element associated with the given AST node.
   * 
   * @param node the target node
   * @param includeDeclarations <code>true</code> if elements should be returned for declaration
   *          sites as well as for reference sites
   * @return the associated element (or <code>null</code> if none can be found)
   */
  public static Element getElement(DartNode node, boolean includeDeclarations) {
    Element targetElement = node.getElement();
    DartNode parent = node.getParent();
    // target of "new X()" or "new X.a()" is not just a type, it is a constructor
    if (parent instanceof DartTypeNode || parent instanceof DartPropertyAccess) {
      DartNode grandparent = parent.getParent();
      if (grandparent instanceof DartNewExpression) {
        targetElement = ((DartNewExpression) grandparent).getElement();
      }
    } else if (parent instanceof DartRedirectConstructorInvocation
        || parent instanceof DartSuperConstructorInvocation) {
      targetElement = parent.getElement();
    }
    // analyze "targetElement"
    if (targetElement == null) {
      if (parent instanceof DartTypeNode) {
        DartNode grandparent = parent.getParent();
        if (grandparent instanceof DartNewExpression) {
          targetElement = ((DartNewExpression) grandparent).getElement();
        }
        if (targetElement == null) {
          Type type = DartAstUtilities.getType((DartTypeNode) parent);
          if (type != null) {
            targetElement = type.getElement();
          }
        }
      } else if (parent instanceof DartMethodInvocation) {
        DartMethodInvocation invocation = (DartMethodInvocation) parent;
        if (node == invocation.getFunctionName()) {
          targetElement = invocation.getElement();
        }
      } else if (parent instanceof DartPropertyAccess) {
        DartPropertyAccess access = (DartPropertyAccess) parent;
        if (node == access.getName()) {
          targetElement = access.getElement();
        }
      } else if (parent instanceof DartUnqualifiedInvocation) {
        DartUnqualifiedInvocation invocation = (DartUnqualifiedInvocation) parent;
        if (node == invocation.getTarget()) {
          targetElement = invocation.getElement();
        }
      } else if (parent instanceof DartParameterizedTypeNode) {
        DartParameterizedTypeNode typeNode = (DartParameterizedTypeNode) parent;
        DartNode grandparent = typeNode.getParent();
        if (grandparent instanceof DartClass) {
          DartClass classDef = (DartClass) grandparent;
          if (typeNode == classDef.getDefaultClass()) {
            targetElement = classDef.getElement().getDefaultClass().getElement();
          }
        }
      } else if (includeDeclarations) {
        DartNode nameNode = node;
        if (parent instanceof DartPropertyAccess) {
          nameNode = parent;
          parent = nameNode.getParent();
        }
        if (parent instanceof DartClass) {
          DartClass classDefinition = (DartClass) parent;
          if (nameNode == classDefinition.getName()) {
            targetElement = classDefinition.getElement();
          }
        } else if (parent instanceof DartField) {
          DartField field = (DartField) parent;
          if (nameNode == field.getName()) {
            targetElement = field.getElement();
          }
        } else if (parent instanceof DartMethodDefinition) {
          DartMethodDefinition method = (DartMethodDefinition) parent;
          if (nameNode == method.getName()) {
            targetElement = method.getElement();
          }
        } else if (parent instanceof DartParameter) {
          DartParameter parameter = (DartParameter) parent;
          if (nameNode == parameter.getName()) {
            targetElement = parameter.getElement();
          }
        } else if (parent instanceof DartVariable) {
          DartVariable variable = (DartVariable) parent;
          if (nameNode == variable.getName()) {
            targetElement = variable.getElement();
          }
        } else if (parent instanceof DartFunctionTypeAlias) {
          DartFunctionTypeAlias alias = (DartFunctionTypeAlias) parent;
          if (nameNode == alias.getName()) {
            targetElement = alias.getElement();
          }
        }
      }
      if (targetElement == null) {
        targetElement = node.getElement();
      }
    }
    return targetElement;
  }

  /**
   * Return the class definition enclosing the given node, or <code>null</code> if the node is not a
   * child of a class definition.
   * 
   * @param node the node enclosed in the class definition to be returned
   * @return the class definition enclosing the given node
   */
  public static DartClass getEnclosingDartClass(DartNode node) {
    return getEnclosingNodeOfType(DartClass.class, node);
  }

  /**
   * Return the first node of the given class that encloses the given node, or <code>null</code> if
   * the node is not a child of a node of the given class. The node itself will <b>not</b> be
   * returned, even if it is an instance of the given class.
   * 
   * @param enclosingNodeClass the class of node to be returned
   * @param node the child of the node to be returned
   * @return the specified parent of the given node
   */
  @SuppressWarnings("unchecked")
  public static <E extends DartNode> E getEnclosingNodeOfType(Class<E> enclosingNodeClass,
      DartNode node) {
    DartNode parent = node.getParent();
    while (parent != null && !enclosingNodeClass.isInstance(parent)) {
      parent = parent.getParent();
    }
    return (E) parent;
  }

  /**
   * Return the type associated with the given type node, or <code>null</code> if the type could not
   * be determined.
   * 
   * @param typeNode the type node whose type is to be returned
   * @return the type associated with the given type node
   */
  public static Type getType(DartTypeNode typeNode) {
    Type type = typeNode.getType();
    if (type == null) {
      DartNode parent = typeNode.getParent();
      if (parent instanceof DartTypeNode) {
        Type parentType = getType((DartTypeNode) parent);
        if (parentType != null && parentType.getKind() == TypeKind.INTERFACE) {
          int index = ((DartTypeNode) parent).getTypeArguments().indexOf(typeNode);
          return ((InterfaceType) parentType).getArguments().get(index);
        }
      }
    }
    return type;
  }

  /**
   * Return <code>true</code> if the given method is a constructor.
   * 
   * @param method the method being tested
   * @return <code>true</code> if the given method is a constructor
   */
  public static boolean isConstructor(DartMethodDefinition method) {
    MethodElement methodElement = method.getElement();
    if (methodElement != null) {
      return methodElement.isConstructor();
    }
    return isConstructor(((DartClass) method.getParent()).getClassName(), method);
  }

  /**
   * Return <code>true</code> if the given method is a constructor.
   * 
   * @param className the name of the type containing the method definition
   * @param method the method being tested
   * @return <code>true</code> if the given method is a constructor
   */
  public static boolean isConstructor(String className, DartMethodDefinition method) {
    if (method.getModifiers().isFactory()) {
      return true;
    }
    DartExpression name = method.getName();
    if (name instanceof DartIdentifier) {
      return ((DartIdentifier) name).getName().equals(className);
    } else if (name instanceof DartPropertyAccess) {
      DartPropertyAccess property = (DartPropertyAccess) name;
      DartNode qualifier = property.getQualifier();
      if (qualifier instanceof DartIdentifier) {
        return ((DartIdentifier) qualifier).getName().equals(className);
      }
    }
    return false;
  }
}
