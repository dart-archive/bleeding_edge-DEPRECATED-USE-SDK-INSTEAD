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
package com.google.dart.engine.services.completion;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.DynamicElementImpl;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.services.completion.ProposalKind.METHOD;

/**
 * The analysis engine for code completion.
 * <p>
 * Note: During development package-private methods are used to group element-specific completion
 * utilities.
 */
public class CompletionEngine {

  private class CompletionVisitor extends GeneralizingASTVisitor<Void> {
    ASTNode completionNode;

    CompletionVisitor(ASTNode node) {
      completionNode = node;
    }
  }

  private class ParentNodeCompleter extends CompletionVisitor {

    ParentNodeCompleter(ASTNode node) {
      super(node);
    }

    @Override
    public Void visitConstructorName(ConstructorName node) {
      if (node.getName() == completionNode) {
        // { new A.!c(); }
        TypeName typeName = node.getType();
        if (typeName != null) {
          Type type = typeName.getType();
          Element typeElement = type.getElement();
          if (typeElement instanceof ClassElement) {
            ClassElement classElement = (ClassElement) typeElement;
            constructorReference(classElement, node.getName());
          }
        }
      }
      return null;
    }

    @Override
    public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
      if (node.getPrefix() == completionNode) {
        // { x!.y }
        node.getIdentifier();
      } else {
        // { v.! }
        SimpleIdentifier receiverName = node.getPrefix();
        Element receiver = receiverName.getElement();
        switch (receiver.getKind()) {
          case LIBRARY: {
            LibraryElement libraryElement = (LibraryElement) receiver;
            // Complete lib_prefix.name
            prefixedAccess(libraryElement, node.getIdentifier());
            break;
          }
          default: {
            Type receiverType = typeOf(receiver);
            if (receiverType != null) {
              // Complete x.y
              Element rcvrTypeElem = receiverType.getElement();
              if (rcvrTypeElem instanceof ClassElement) {
                prefixedAccess((ClassElement) rcvrTypeElem, node.getIdentifier());
              }
            }
            break;
          }
        }
      }
      return null;
    }
  }

  private class TerminalNodeCompleter extends CompletionVisitor {

    TerminalNodeCompleter(ASTNode node) {
      super(node);
    }

    @Override
    public Void visitIdentifier(Identifier node) {
      ASTNode parent = node.getParent();
      if (parent != null) {
        ParentNodeCompleter visitor = new ParentNodeCompleter(completionNode);
        return parent.accept(visitor);
      }
      return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocation node) {
      return null;
    }
  }

  private CompletionRequestor requestor;
  private CompletionFactory factory;
  private AssistContext context;

  public CompletionEngine(CompletionRequestor requestor, CompletionFactory factory) {
    this.requestor = requestor;
    this.factory = factory;
  }

  /**
   * Analyze the source unit in the given context to determine completion proposals at the selection
   * offset of the context.
   */
  public void complete(AssistContext context) {
    this.context = context;
    requestor.beginReporting();
    // TODO: Temporary code for exercising test framework.
    CompletionProposal prop = factory.createCompletionProposal(METHOD);
    prop.setCompletion("toString");
    requestor.accept(prop);
    // End temp
    ASTNode completionNode = context.getCoveredNode();
    if (completionNode != null) {
      TerminalNodeCompleter visitor = new TerminalNodeCompleter(completionNode);
      completionNode.accept(visitor);
    }
    requestor.endReporting();
  }

  public AssistContext getContext() {
    // TODO: Consider deleting this method.
    return context;
  }

  void constructorReference(ClassElement classElement, SimpleIdentifier completionNode) {
    classElement.getConstructors(); // TODO: These become the completion proposals.
  }

  void prefixedAccess(ClassElement classElement, SimpleIdentifier identifier) {
    // Complete identifier when it access field or method in classElement.
    InterfaceType[] allTypes = allTypes(classElement);
    for (InterfaceType type : allTypes) {
      type.getElement().getAccessors(); // TODO These become the completion proposals
      type.getElement().getMethods();
      type.getElement().getFields(); // (probably ignore fields; names duplicated in accessors)
    }
  }

  void prefixedAccess(LibraryElement libElement, SimpleIdentifier identifier) {
    // Complete identifier when it access a member defined in the libraryElement.
  }

  private InterfaceType[] allTypes(ClassElement classElement) {
    InterfaceType[] supertypes = classElement.getAllSupertypes();
    InterfaceType[] allTypes = new InterfaceType[supertypes.length + 1];
    allTypes[0] = classElement.getType();
    System.arraycopy(supertypes, 0, allTypes, 1, supertypes.length);
    return allTypes;
  }

  private Type typeOf(Element receiver) {
    Type receiverType;
    switch (receiver.getKind()) {
      case FIELD:
      case PARAMETER:
      case VARIABLE: {
        VariableElement receiverElement = (VariableElement) receiver;
        receiverType = receiverElement.getType();
        break;
      }
      case CONSTRUCTOR:
      case FUNCTION:
      case METHOD:
      case GETTER:
      case SETTER: {
        ExecutableElement receiverElement = (ExecutableElement) receiver;
        FunctionType funType = receiverElement.getType();
        receiverType = funType.getReturnType();
      }
      case CLASS: {
        ClassElement receiverElement = (ClassElement) receiver;
        receiverType = receiverElement.getType();
        break;
      }
      case DYNAMIC: {
        DynamicElementImpl receiverElement = (DynamicElementImpl) receiver;
        receiverType = receiverElement.getType();
      }
      case TYPE_ALIAS: {
        TypeAliasElement receiverElement = (TypeAliasElement) receiver;
        FunctionType funType = receiverElement.getType();
        receiverType = funType.getReturnType();
        break;
      }
      default: {
        receiverType = null;
      }
    }
    return receiverType;
  }
}
