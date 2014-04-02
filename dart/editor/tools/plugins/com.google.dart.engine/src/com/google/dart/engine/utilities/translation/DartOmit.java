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
 * The annotation {@code DartOmit} specifies that the annotated method should be omitted from the
 * Dart code. This is intended to be used for overloaded methods where one version of the method has
 * been annotated with {@link DartOptional} to specify optional parameters.
 * <p>
 * For example, to omit a method, you could use the following:
 * 
 * <pre>
 * @DartOmit
 * public void process(ClassElement rootClass) {
 *   process(rootClass, new HashSet<ClassElement>()}
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface DartOmit {
}
