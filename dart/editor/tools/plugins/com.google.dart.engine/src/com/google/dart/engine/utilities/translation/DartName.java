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
 * The annotation {@code DartName} specifies the name to be used for the Dart element that is
 * created to replace the annotated Java element.
 * <p>
 * For example, to rename a class, you could use the following:
 * 
 * <pre>
 * @DartName("AstNode")
 * public class ASTNode ...
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
public @interface DartName {
  /**
   * Return the name to be used for the Dart element.
   * 
   * @return the name to be used for the Dart element
   */
  String value();
}
