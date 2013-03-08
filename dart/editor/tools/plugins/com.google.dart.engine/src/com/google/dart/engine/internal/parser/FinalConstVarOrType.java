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
package com.google.dart.engine.internal.parser;

import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code FinalConstVarOrType} implement a simple data-holder for a method
 * that needs to return multiple values.
 * 
 * @coverage dart.engine.parser
 */
public class FinalConstVarOrType {
  /**
   * The 'final', 'const' or 'var' keyword, or {@code null} if none was given.
   */
  private Token keyword;

  /**
   * The type, of {@code null} if no type was specified.
   */
  private TypeName type;

  /**
   * Initialize a newly created holder with the given data.
   * 
   * @param keyword the 'final', 'const' or 'var' keyword
   * @param type the type
   */
  public FinalConstVarOrType(Token keyword, TypeName type) {
    this.keyword = keyword;
    this.type = type;
  }

  /**
   * Return the 'final', 'const' or 'var' keyword, or {@code null} if none was given.
   * 
   * @return the 'final', 'const' or 'var' keyword
   */
  public Token getKeyword() {
    return keyword;
  }

  /**
   * Return the type, of {@code null} if no type was specified.
   * 
   * @return the type
   */
  public TypeName getType() {
    return type;
  }
}
