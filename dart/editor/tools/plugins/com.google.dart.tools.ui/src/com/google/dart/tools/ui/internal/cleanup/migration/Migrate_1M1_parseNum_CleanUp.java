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
package com.google.dart.tools.ui.internal.cleanup.migration;

import com.google.dart.compiler.Source;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.Elements;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;

/**
 * In specification 1.0 M1 <code>parseInt()</code> and <code>parseDouble()</code> replaced with
 * <code>int.parse()</code> and <code>double.parse()</code>.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M1_parseNum_CleanUp extends AbstractMigrateCleanUp {
  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(DartMethodInvocation node) {
        processInvocation(node);
        return super.visitMethodInvocation(node);
      }

      @Override
      public Void visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
        processInvocation(node);
        return super.visitUnqualifiedInvocation(node);
      }

      private void processInvocation(DartInvocation node) {
        Element element = node.getElement();
        if (element instanceof MethodElement && node.getArguments().size() == 1) {
          MethodElement methodElement = (MethodElement) element;
          boolean parseInt = methodElement.getName().equals("parseInt");
          boolean parseDouble = methodElement.getName().equals("parseDouble");
          if (parseInt || parseDouble) {
            Source source = element.getSourceInfo().getSource();
            if (Elements.isLibrarySource(source, "/math/math.dart")) {
              DartExpression argument = node.getArguments().get(0);
              SourceRange range = SourceRangeFactory.forStartStart(node, argument);
              if (parseInt) {
                addReplaceEdit(range, "int.parse(");
              }
              if (parseDouble) {
                addReplaceEdit(range, "double.parse(");
              }
            }
          }
        }
      }
    });
  }
}
