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
 * The annotation {@code DartBlockBody} specifies the Dart code that should be used as the body of
 * the annotated method. Each element of the array will be written on a separate line, preceded by
 * enough white-space to indent the body two spaces more than the method declaration.
 * <p>
 * For example, to replace the body of a method with a block, you could use the following:
 * 
 * <pre>
 * @DartBlockBody({"return height * width;"})
 * public int getArea() ...
 * </pre>
 */
@Target(ElementType.METHOD)
public @interface DartBlockBody {
  /**
   * Return an array containing the lines in the body of the method.
   * 
   * @return an array containing the lines in the body of the method
   */
  String[] value();
}
