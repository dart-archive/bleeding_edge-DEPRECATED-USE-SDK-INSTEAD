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

package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

import java.util.List;

/**
 * Angular filter node.
 * 
 * @coverage dart.engine.ast
 */
public class AngularFilterNode {
  /**
   * The {@link TokenType#BAR} token.
   */
  private final Token token;

  /**
   * The name of the filter.
   */
  private final SimpleIdentifier name;

  /**
   * The arguments for this filter.
   */
  private final List<AngularFilterArgument> arguments;

  public AngularFilterNode(Token token, SimpleIdentifier name, List<AngularFilterArgument> arguments) {
    this.token = token;
    this.name = name;
    this.arguments = arguments;
  }

  /**
   * Returns the arguments.
   */
  public List<AngularFilterArgument> getArguments() {
    return arguments;
  }

  /**
   * Returns the filter name.
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Returns the {@link TokenType#BAR} token.
   */
  public Token getToken() {
    return token;
  }
}
