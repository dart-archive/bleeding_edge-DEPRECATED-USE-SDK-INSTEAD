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

package com.google.dart.java2dart.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

import java.util.List;

/**
 * Factory for {@link ASTNode}s.
 */
public class ASTFactory {
  public static final KeywordToken TOKEN_FINAL = new KeywordToken(Keyword.FINAL, 0);
  public static final KeywordToken TOKEN_NEW = new KeywordToken(Keyword.NEW, 0);
  public static final KeywordToken TOKEN_STATIC = new KeywordToken(Keyword.STATIC, 0);
  public static final KeywordToken TOKEN_IMPORT = new KeywordToken(Keyword.IMPORT, 0);
  public static final KeywordToken TOKEN_AS = new KeywordToken(Keyword.AS, 0);

  /**
   * <code>leftName = rightName;</code>
   */
  public static ExpressionStatement assignmentStatement(String leftName, String rightName) {
    return new ExpressionStatement(new AssignmentExpression(simpleIdentifier(leftName), new Token(
        TokenType.EQ,
        0), simpleIdentifier(rightName)), null);
  }

  /**
   * <code>typeName name;</code>
   */
  public static FieldDeclaration fieldDeclaration(String typeName, String name) {
    return fieldDeclaration(typeName(typeName), name, null);
  }

  /**
   * <code>typeName name = initializer;</code>
   */
  public static FieldDeclaration fieldDeclaration(TypeName typeName, String name,
      Expression initializer) {
    return new FieldDeclaration(null, null, null, new VariableDeclarationList(
        null,
        typeName,
        ImmutableList.of(new VariableDeclaration(
            null,
            null,
            simpleIdentifier(name),
            null,
            initializer))), null);
  }

  public static StringToken identifierToken(String name) {
    return new StringToken(TokenType.IDENTIFIER, name, 0);
  }

  public static ImportDirective importDirective(String uri, String prefix) {
    return new ImportDirective(null, null, TOKEN_IMPORT, stringLiteral(uri), prefix == null ? null
        : TOKEN_AS, prefix == null ? null : simpleIdentifier(prefix), null, null);
  }

  public static InstanceCreationExpression instanceCreation(String typeName, ArgumentList argList) {
    return new InstanceCreationExpression(TOKEN_NEW, new ConstructorName(
        typeName(typeName),
        null,
        null), argList);
  }

  public static IntegerLiteral integerLiteral(long value) {
    return new IntegerLiteral(new StringToken(TokenType.INT, "" + value, 0), value);
  }

  public static LibraryDirective libraryDirective(String... components) {
    LibraryIdentifier identifier = libraryIdentifier(components);
    return new LibraryDirective(null, null, null, identifier, null);
  }

  public static LibraryIdentifier libraryIdentifier(String... components) {
    List<SimpleIdentifier> componentList = Lists.newArrayList();
    for (String component : components) {
      componentList.add(simpleIdentifier(component));
    }
    return new LibraryIdentifier(componentList);
  }

  public static ListLiteral listLiteral(List<Expression> elements) {
    return new ListLiteral(null, null, null, elements, null);
  }

  /**
   * <code>List<List<...elementType>></code>
   */
  public static TypeName listType(TypeName elementType, int dimensions) {
    TypeName listType = elementType;
    for (int i = 0; i < dimensions; i++) {
      TypeArgumentList typeArguments = new TypeArgumentList(
          null,
          Lists.newArrayList(listType),
          null);
      listType = new TypeName(simpleIdentifier("List"), typeArguments);
    }
    return listType;
  }

  public static FormalParameterList newFormalParameterList(List<FormalParameter> parameters) {
    return new FormalParameterList(null, parameters, null, null, null);
  }

  /**
   * <code>typeName name</code>
   */
  public static SimpleFormalParameter simpleFormalParameter(String typeName, String name) {
    return new SimpleFormalParameter(null, null, null, typeName(typeName), simpleIdentifier(name));
  }

  /**
   * <code>typeName name</code>
   */
  public static SimpleFormalParameter simpleFormalParameter(TypeName typeName, String name) {
    return new SimpleFormalParameter(null, null, null, typeName, simpleIdentifier(name));
  }

  public static SimpleIdentifier simpleIdentifier(String name) {
    return new SimpleIdentifier(identifierToken(name));
  }

  public static SimpleStringLiteral stringLiteral(String value) {
    return new SimpleStringLiteral(new StringToken(TokenType.STRING, "'" + value + "'", 0), value);
  }

  public static TypeName typeName(SimpleIdentifier name) {
    return new TypeName(name, null);
  }

  public static TypeName typeName(String name) {
    return typeName(simpleIdentifier(name));
  }
}
