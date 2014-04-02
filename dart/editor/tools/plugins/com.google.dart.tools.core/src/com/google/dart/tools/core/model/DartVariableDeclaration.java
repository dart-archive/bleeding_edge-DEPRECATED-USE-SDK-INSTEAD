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
package com.google.dart.tools.core.model;


/**
 * The interface <code>DartVariableDeclaration</code> defines the behavior of elements representing
 * a variable defined within another element. Variables can be defined in {@link DartFunction
 * functions}, {@link Method methods} and {@link CompilationUnit compilation units}, and include
 * parameters defined for either methods or functions.
 * 
 * @coverage dart.tools.core.model
 */
public interface DartVariableDeclaration extends CompilationUnitElement, SourceReference {

}
