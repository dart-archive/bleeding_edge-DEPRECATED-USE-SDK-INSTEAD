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
 * The abstract class <code>Literal</code> defines the behavior common to nodes that represent a
 * literal expression.
 * 
 * <pre>
 * literal ::=
 *     {@link BooleanLiteral booleanLiteral}
 *   | {@link DoubleLiteral doubleLiteral}
 *   | {@link IntegerLiteral integerLiteral}
 *   | {@link ListLiteral listLiteral}
 *   | {@link MapLiteral mapLiteral}
 *   | {@link NullLiteral nullLiteral}
 *   | {@link StringLiteral stringLiteral}
 * </pre>
 */
public abstract class Literal extends Expression {
}
