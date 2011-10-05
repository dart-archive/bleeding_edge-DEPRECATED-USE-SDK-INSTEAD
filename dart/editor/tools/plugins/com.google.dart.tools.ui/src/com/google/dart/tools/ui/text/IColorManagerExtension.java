/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.text;

import org.eclipse.swt.graphics.RGB;

/**
 * Extends {@link com.google.dart.tools.ui.text.config.IColorManager} with the ability to bind and
 * un-bind colors. Provisional API: This class/interface is part of an interim API that is still
 * under development and expected to change significantly before reaching stability. It is being
 * made available at this early stage to solicit feedback from pioneering adopters on the
 * understanding that any code that uses this API will almost certainly be broken (repeatedly) as
 * the API evolves.
 */
public interface IColorManagerExtension {

  /**
   * Remembers the given color specification under the given key.
   * 
   * @param key the color key
   * @param rgb the color specification
   * @throws java.lang.UnsupportedOperationException if there is already a color specification
   *           remembered under the given key
   */
  void bindColor(String key, RGB rgb);

  /**
   * Forgets the color specification remembered under the given key.
   * 
   * @param key the color key
   */
  void unbindColor(String key);
}
