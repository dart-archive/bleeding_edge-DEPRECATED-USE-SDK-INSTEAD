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

package com.google.dart.java2dart.engine;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.utilities.translation.DartBlockBody;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.ParsedAnnotation;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Applies {@link DartBlockBody} and other annotations.
 */
public class EngineAnnotationProcessor extends SemanticProcessor {
  public EngineAnnotationProcessor(Context context) {
    super(context);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void process(CompilationUnit unit) {
    Map<AstNode, List<ParsedAnnotation>> nodeAnnotations = context.getNodeAnnotations();
    for (Entry<AstNode, List<ParsedAnnotation>> entry : nodeAnnotations.entrySet()) {
      AstNode node = entry.getKey();
      for (ParsedAnnotation annotation : entry.getValue()) {
        if (annotation.getName().equals("DartBlockBody")) {
          List<String> bodyLines = (List<String>) annotation.get("value");
          String bodySource = "";
          if (!bodyLines.isEmpty()) {
            bodySource = StringUtils.join(bodyLines, "\n    ");
            bodySource = "    " + bodySource.trim();
          }
          node.setProperty(ToFormattedSourceVisitor.BLOCK_BODY_KEY, bodySource);
        } else if (annotation.getName().equals("DartExpressionBody")) {
          String bodySource = (String) annotation.get("value");
          node.setProperty(ToFormattedSourceVisitor.EXPRESSION_BODY_KEY, bodySource);
        } else if (annotation.getName().equals("DartOmit")) {
          AstNode parent = node.getParent();
          if (parent instanceof CompilationUnit) {
            unit.getDeclarations().remove(node);
          } else if (parent instanceof ClassDeclaration) {
            ((ClassDeclaration) parent).getMembers().remove(node);
          }
        }
      }
    }
  }
}
