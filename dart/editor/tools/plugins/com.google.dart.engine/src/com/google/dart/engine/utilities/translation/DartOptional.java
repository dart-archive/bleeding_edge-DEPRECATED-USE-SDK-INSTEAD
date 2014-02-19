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

import com.google.dart.engine.utilities.dart.ParameterKind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * The annotation {@code DartOptional} specifies a parameter as being one that should be optional in
 * the translated Dart method. If there is more than one optional parameter defined for a given
 * method, then all of them must have the same kind.
 * <p>
 * For example, to make a parameter be optional with a default value, you could use the following:
 * 
 * <pre>
 * public void process(ClassElement currentClass, @DartOptional(defaultValue = "new Map()") HashSet<ClassElement> visitedClasses) {
 *   process(rootClass, new HashSet<ClassElement>()}
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
public @interface DartOptional {
  /**
   * Return the Dart code that should be used as the default value of the parameter.
   * 
   * @return the Dart code that should be used as the default value of the parameter
   */
  String defaultValue() default "";

  /**
   * Return the kind of optional parameter to be created.
   * 
   * @return the kind of optional parameter to be created
   */
  ParameterKind kind() default ParameterKind.POSITIONAL;
}
