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

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;

/**
 * In 1.0 M2 using "interface" should be replaced with "abstract class".
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M2_removeInterface_CleanUp extends AbstractMigrateCleanUp {
  @Override
  protected void createFix() {
    unitNode.accept(new ASTVisitor<Void>() {
      private String className;
      private String defaultName;

      @Override
      public Void visitClass(DartClass node) {
        if (node.isInterface()) {
          className = utils.getText(node.getName());
          // replace "interface" with "abstract class"
          SourceRange interfaceTokenRange = SourceRangeFactory.forStartLength(
              node.getTokenOffset(),
              node.getTokenLength());
          addReplaceEdit(interfaceTokenRange, "abstract class");
          // remove "default Impl"
          DartNode defaultClass = node.getDefaultClass();
          if (defaultClass != null) {
            defaultName = utils.getText(defaultClass);
            int start = node.getDefaultTokenOffset();
            int end = node.getOpenBraceOffset();
            SourceRange defaultRange = SourceRangeFactory.forStartEnd(start, end);
            addReplaceEdit(defaultRange, "");
          }
        }
        super.visitClass(node);
        className = null;
        defaultName = null;
        return null;
      }

      @Override
      public Void visitMethodDefinition(DartMethodDefinition node) {
        String methodName = utils.getText(node.getName());
        boolean isFactoryDefault = methodName.equals(className);
        boolean isFactoryNamed = methodName.startsWith(className + ".");
        if (isFactoryDefault || isFactoryNamed) {
          addReplaceEdit(SourceRangeFactory.forStartLength(node, 0), "factory ");
          SourceRange redirectRange = SourceRangeFactory.forStartLength(
              node.getFunction().getParametersCloseParen() + 1,
              0);
          String redirectName;
          if (isFactoryDefault) {
            redirectName = defaultName;
          } else {
            redirectName = defaultName + "." + StringUtils.substringAfterLast(methodName, ".");
          }
          addReplaceEdit(redirectRange, " = " + redirectName);
        }
        return super.visitMethodDefinition(node);
      }
    });
  }
}
