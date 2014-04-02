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
 * The annotation {@code DartLibrary} specifies the name of the library to which the annotated type
 * should be added. The library at that path must be defined elsewhere using a
 * {@link DartLibraryDeclaration} annotation.
 * <p>
 * For example, to add a class to the AST library, you could use the following:
 * 
 * <pre>
 * @DartLibrary("engine.ast")
 * public class ASTNode ...
 * </pre>
 */
@Target(ElementType.TYPE)
public @interface DartLibrary {
  /**
   * Return the name of the library to which the type is to be added.
   * 
   * @return the name of the library to which the type is to be added
   */
  String value();
}
