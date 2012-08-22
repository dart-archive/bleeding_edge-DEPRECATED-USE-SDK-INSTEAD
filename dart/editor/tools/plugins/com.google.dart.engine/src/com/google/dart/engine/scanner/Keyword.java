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
package com.google.dart.engine.scanner;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The enumeration {@code Keyword} defines the keywords in the Dart programming language.
 */
public enum Keyword {
  BREAK("break"),
  CASE("case"),
  CATCH("catch"),
  CLASS("class"),
  CONST("const"),
  CONTINUE("continue"),
  DEFAULT("default"),
  DO("do"),
  ELSE("else"),
  EXTENDS("extends"),
  FALSE("false"),
  FINAL("final"),
  FINALLY("finally"),
  FOR("for"),
  IF("if"),
  IN("in"),
  IS("is"),
  NEW("new"),
  NULL("null"),
  RETURN("return"),
  SUPER("super"),
  SWITCH("switch"),
  THIS("this"),
  THROW("throw"),
  TRUE("true"),
  TRY("try"),
  VAR("var"),
  VOID("void"),
  WHILE("while"),

  // Pseudo keywords:
  ABSTRACT("abstract", true),
  AS("as", true),
  ASSERT("assert", true),
  //DYNAMIC("Dynamic", true),
  EXTERNAL("external", true),
  FACTORY("factory", true),
  GET("get", true),
  IMPLEMENTS("implements", true),
  OPERATOR("operator", true),
  SET("set", true),
  STATIC("static", true),
  TYPEDEF("typedef", true);

  /**
   * The lexeme for the keyword.
   */
  private final String syntax;

  /**
   * A flag indicating whether the keyword is a pseudo-keyword. Pseudo keywords can be used as
   * identifiers.
   */
  private final boolean isPseudoKeyword;

  /**
   * A table mapping the lexemes of keywords to the corresponding keyword.
   */
  public static Map<String, Keyword> keywords = createKeywordMap();

  /**
   * Create a table mapping the lexemes of keywords to the corresponding keyword.
   * 
   * @return the table that was created
   */
  private static Map<String, Keyword> createKeywordMap() {
    LinkedHashMap<String, Keyword> result = new LinkedHashMap<String, Keyword>();
    for (Keyword keyword : values()) {
      result.put(keyword.syntax, keyword);
    }
    return result;
  }

  /**
   * Initialize a newly created keyword to have the given syntax. The keyword is not a
   * pseudo-keyword.
   * 
   * @param syntax the lexeme for the keyword
   */
  Keyword(String syntax) {
    this(syntax, false);
  }

  /**
   * Initialize a newly created keyword to have the given syntax. The keyword is a pseudo-keyword if
   * the given flag is {@code true}.
   * 
   * @param syntax the lexeme for the keyword
   * @param isPseudoKeyword {@code true} if this keyword is a pseudo-keyword
   */
  Keyword(String syntax, boolean isPseudoKeyword) {
    this.syntax = syntax;
    this.isPseudoKeyword = isPseudoKeyword;
  }

  /**
   * Return the lexeme for the keyword.
   * 
   * @return the lexeme for the keyword
   */
  public String getSyntax() {
    return syntax;
  }

  /**
   * Return {@code true} if this keyword is a pseudo-keyword. Pseudo keywords can be used as
   * identifiers.
   * 
   * @return {@code true} if this keyword is a pseudo-keyword
   */
  public boolean isPseudoKeyword() {
    return isPseudoKeyword;
  }
}
