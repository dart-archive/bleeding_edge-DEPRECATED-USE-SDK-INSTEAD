/*
 * Copyright 2013, the Dart project authors.
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
package com.google.dart.engine.ast;

import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.TokenType;

/**
 * Ephemeral identifiers are created as needed to mimic the presence of an empty identifier.
 * 
 * @coverage dart.engine.ast
 */
public class EphemeralIdentifier extends SimpleIdentifier {

  public EphemeralIdentifier(AstNode parent, int location) {
    super(new StringToken(TokenType.IDENTIFIER, "", location));
    parent.becomeParentOf(this);
  }
}
