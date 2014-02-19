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
package com.google.dart.engine.utilities.translation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * The annotation {@code DartLibraryDeclaration} specifies the path to and name of a library that
 * should be created. The type that has this annotation is implicitly part of the library it defines
 * as if the {@link DartLibrary} annotation had been used.
 * <p>
 * For example, to define a library for the AST structure, you could use the following:
 * 
 * <pre>
 * @DartLibraryDeclaration(path = "lib/ast.dart", name = "engine.ast", imports = {"engine.token"});
 * </pre>
 */
@Target(ElementType.TYPE)
public @interface DartLibraryDeclaration {
  /**
   * Return an array containing the names of the libraries that should be imported into the library
   * being declared.
   * 
   * @return the libraries that should be imported into the library being declared
   */
  String[] imports();

  /**
   * Return the name of the library being declared.
   * 
   * @return the name of the library being declared
   */
  String name();

  /**
   * Return the path, relative to the root of the package, of the library being declared.
   * 
   * @return the path to the library being declared
   */
  String path();
}
