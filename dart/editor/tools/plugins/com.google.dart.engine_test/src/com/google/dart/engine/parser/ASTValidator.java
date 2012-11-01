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
package com.google.dart.engine.parser;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Instances of the class {@code ASTValidator} are used to validate the correct construction of an
 * AST structure.
 */
public class ASTValidator extends GeneralizingASTVisitor<Void> {
  /**
   * A list containing the errors found while traversing the AST structure.
   */
  private ArrayList<String> errors = new ArrayList<String>();

  /**
   * Assert that no errors were found while traversing any of the AST structures that have been
   * visited.
   */
  public void assertValid() {
    if (!errors.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      builder.append("Invalid AST structure:");
      for (String message : errors) {
        builder.append("\r\n   ");
        builder.append(message);
      }
      Assert.fail(builder.toString());
    }
  }

  @Override
  public Void visitNode(ASTNode node) {
    validate(node);
    return super.visitNode(node);
  }

  /**
   * Validate that the given AST node is correctly constructed.
   * 
   * @param node the AST node being validated
   */
  private void validate(ASTNode node) {
    ASTNode parent = node.getParent();
    if (node instanceof CompilationUnit) {
      if (parent != null) {
        errors.add("Compilation units should not have a parent");
      }
    } else {
      if (parent == null) {
        errors.add("No parent for " + node.getClass().getName());
      }
    }

    if (node.getBeginToken() == null) {
      errors.add("No begin token for " + node.getClass().getName());
    }
    if (node.getEndToken() == null) {
      errors.add("No end token for " + node.getClass().getName());
    }

    int nodeStart = node.getOffset();
    int nodeLength = node.getLength();
    if (nodeStart < 0 || nodeLength < 0) {
      errors.add("No source info for " + node.getClass().getName());
    }

    if (parent != null) {
      int nodeEnd = nodeStart + nodeLength;
      int parentStart = parent.getOffset();
      int parentEnd = parentStart + parent.getLength();
      if (nodeStart < parentStart) {
        errors.add("Invalid source start (" + nodeStart + ") for " + node.getClass().getName()
            + " inside " + parent.getClass().getName() + " (" + parentStart + ")");
      }
      if (nodeEnd > parentEnd) {
        errors.add("Invalid source end (" + nodeEnd + ") for " + node.getClass().getName()
            + " inside " + parent.getClass().getName() + " (" + parentStart + ")");
      }
    }
  }
}
