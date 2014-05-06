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
package com.google.dart.engine.services.completion;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.EphemeralIdentifier;
import com.google.dart.engine.scanner.Token;

/**
 * An {@link Ident} is a wrapper for a String that provides type equivalence with SimpleIdentifier.
 */
class Ident extends EphemeralIdentifier {
  private String name;

  Ident(AstNode parent, int offset) {
    super(parent, offset);
  }

  Ident(AstNode parent, String name, int offset) {
    super(parent, offset);
    this.name = name;
  }

  Ident(AstNode parent, Token name) {
    super(parent, name.getOffset());
    this.name = name.getLexeme();
  }

  @Override
  public String getName() {
    if (name != null) {
      return name;
    }
    String n = super.getName();
    if (n != null) {
      return n;
    }
    return "";
  }
}
