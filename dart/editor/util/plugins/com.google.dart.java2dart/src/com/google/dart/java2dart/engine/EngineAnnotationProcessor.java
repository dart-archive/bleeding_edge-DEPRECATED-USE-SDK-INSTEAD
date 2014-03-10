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

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.utilities.translation.DartBlockBody;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.ParsedAnnotation;
import com.google.dart.java2dart.processor.SemanticProcessor;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import static com.google.dart.java2dart.util.AstFactory.annotation;
import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.namedExpression;
import static com.google.dart.java2dart.util.AstFactory.namedFormalParameter;
import static com.google.dart.java2dart.util.AstFactory.positionalFormalParameter;

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
        String annotationName = annotation.getName();
        if (annotationName.equals("DartBlockBody")) {
          List<String> bodyLines = (List<String>) annotation.get("value");
          String bodySource = "";
          if (!bodyLines.isEmpty()) {
            bodySource = StringUtils.join(bodyLines, "\n    ");
            bodySource = "    " + bodySource.trim();
          }
          node.setProperty(ToFormattedSourceVisitor.BLOCK_BODY_KEY, bodySource);
        } else if (annotationName.equals("DartExpressionBody")) {
          String bodySource = (String) annotation.get("value");
          node.setProperty(ToFormattedSourceVisitor.EXPRESSION_BODY_KEY, bodySource);
        } else if (annotationName.equals("DartName")) {
          String newName = (String) annotation.get("value");
          if (node instanceof ClassDeclaration) {
            ClassDeclaration classDeclaration = (ClassDeclaration) node;
            SimpleIdentifier nameNode = classDeclaration.getName();
            context.renameIdentifier(nameNode, newName);
          } else if (node instanceof ConstructorDeclaration) {
            ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) node;
            context.renameConstructor(constructorDeclaration, newName);
          } else if (node instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration) node;
            List<VariableDeclaration> fields = fieldDeclaration.getFields().getVariables();
            if (fields.size() != 1) {
              throw new IllegalArgumentException(
                  "@DartName is supported only field declarations with a single field.");
            }
            SimpleIdentifier nameNode = fields.get(0).getName();
            context.renameIdentifier(nameNode, newName);
          } else if (node instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) node;
            SimpleIdentifier nameNode = methodDeclaration.getName();
            context.renameIdentifier(nameNode, newName);
          } else {
            throw new IllegalArgumentException("@DartName is not supported for: " + node.getClass());
          }
        } else if (annotationName.equals("DartOmit")) {
          AstNode parent = node.getParent();
          if (parent instanceof CompilationUnit) {
            unit.getDeclarations().remove(node);
          } else if (parent instanceof ClassDeclaration) {
            ((ClassDeclaration) parent).getMembers().remove(node);
          }
        } else if (annotationName.equals("DartOptional")) {
          SimpleFormalParameter parameter = (SimpleFormalParameter) node;
          AstNode parameterList = parameter.getParent();
          // prepare annotation arguments
          String kindName = (String) annotation.get("kind");
          String defaultValueSource = (String) annotation.get("defaultValue");
          // replace normal parameter with default
          DefaultFormalParameter defaultParameter;
          if (kindName == null || kindName.endsWith(".POSITIONAL")) {
            defaultParameter = positionalFormalParameter(parameter, null);
          } else {
            replaceInvocationArgumentWithNamed(parameter);
            defaultParameter = namedFormalParameter(parameter, null);
          }
          replaceNode(parameterList, parameter, defaultParameter);
          // set default value
          defaultParameter.setProperty(
              ToFormattedSourceVisitor.DEFAULT_VALUE_KEY,
              defaultValueSource);
        } else if (annotationName.equals("Override")) {
          if (node instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) node;
            method.setMetadata(Lists.newArrayList(annotation(identifier("override"))));
          }
        } else {
//          throw new IllegalArgumentException("Unknown annotation: " + annotationName);
        }
      }
    }
  }

  private void replaceInvocationArgumentWithNamed(SimpleFormalParameter parameter) {
    String parameterName = parameter.getIdentifier().getName();
    FormalParameterList parameterList = (FormalParameterList) parameter.getParent();
    AstNode member = parameterList.getParent();
    int index = parameterList.getParameters().indexOf(parameter);
    if (member instanceof MethodDeclaration) {
      MethodDeclaration method = (MethodDeclaration) member;
      List<MethodInvocation> invocations = context.getInvocations(method);
      for (MethodInvocation invocation : invocations) {
        List<Expression> arguments = invocation.getArgumentList().getArguments();
        Expression argument = arguments.get(index);
        arguments.set(index, namedExpression(parameterName, argument));
      }
    }
  }
}
