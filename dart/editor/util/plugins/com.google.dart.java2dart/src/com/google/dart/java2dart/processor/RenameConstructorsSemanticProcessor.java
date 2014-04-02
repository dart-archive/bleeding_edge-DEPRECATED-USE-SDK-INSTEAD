/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.java2dart.processor;

import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.java2dart.Context;

import java.util.Set;

/**
 * {@link SemanticProcessor} to rename {@link ConstructorDeclaration} to give them unique names.
 */
public class RenameConstructorsSemanticProcessor extends SemanticProcessor {
  public RenameConstructorsSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new RecursiveAstVisitor<Void>() {
      private final Set<String> memberNamesInClass = Sets.newHashSet();
      private int numConstructors;

      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        memberNamesInClass.clear();
        numConstructors = 0;
        NodeList<ClassMember> members = node.getMembers();
        for (ClassMember member : members) {
          if (member instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructor = (ConstructorDeclaration) member;
            if (hasName(constructor)) {
              continue;
            }
            numConstructors++;
          }
          if (member instanceof MethodDeclaration) {
            String name = ((MethodDeclaration) member).getName().getName();
            memberNamesInClass.add(name);
          }
          if (member instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) member;
            NodeList<VariableDeclaration> variables = fieldDeclaration.getFields().getVariables();
            for (VariableDeclaration variable : variables) {
              String name = variable.getName().getName();
              memberNamesInClass.add(name);
            }
          }
        }
        return super.visitClassDeclaration(node);
      }

      @Override
      public Void visitConstructorDeclaration(ConstructorDeclaration node) {
        if (hasName(node)) {
          return null;
        }
        // prepare name
        String name = null;
        if (numConstructors == 1 || node.getParameters().getParameters().isEmpty()) {
          // don't set name, use unnamed constructor
        } else {
          int index = 1;
          while (true) {
            name = "con" + index++;
            if (!memberNamesInClass.contains(name)) {
              break;
            }
          }
        }
        memberNamesInClass.add(name);
        // apply name
        if ("<empty>".equals(name)) {
          name = null;
        }
        context.renameConstructor(node, name);
        // continue
        return super.visitConstructorDeclaration(node);
      }

      private boolean hasName(ConstructorDeclaration node) {
        SimpleIdentifier name = node.getName();
        if (name == null) {
          return false;
        }
        return !name.getName().startsWith("jtd_constructor_");
      }
    });
  }
}
