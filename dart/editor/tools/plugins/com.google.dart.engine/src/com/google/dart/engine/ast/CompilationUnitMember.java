/*
 * Copyright 2012, the Dart project authors.
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

/**
 * Instances of the class {@code CompilationUnitMember} defines the behavior common to nodes that
 * declare a name within the scope of a compilation unit.
 * 
 * <pre>
 * compilationUnitMember ::=
 *     {@link ClassDeclaration classDeclaration}
 *   | {@link TypeAlias typeAlias}
 *   | {@link FunctionDeclaration functionDeclaration}
 *   | {@link MethodDeclaration getOrSetDeclaration}
 *   | {@link VariableDeclaration constantsDeclaration}
 *   | {@link VariableDeclaration variablesDeclaration}
 * </pre>
 */
public abstract class CompilationUnitMember extends Declaration {
  /**
   * Initialize a newly created generic compilation unit member.
   */
  public CompilationUnitMember() {
  }

  /**
   * Initialize a newly created generic compilation unit member.
   * 
   * @param comment the documentation comment associated with this member
   */
  public CompilationUnitMember(Comment comment) {
    super(comment);
  }
}
