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

import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.graphics.Color;

/**
 * Manages SWT color objects for the given color keys and given <code>RGB</code> objects. Until the
 * <code>dispose</code> method is called, the same color object is returned for equal keys and equal
 * <code>RGB</code> values.
 * <p>
 * In order to provide backward compatibility for clients of <code>IColorManager</code>, extension
 * interfaces are used to provide a means of evolution. The following extension interfaces exist:
 * <ul>
 * <li>{@link com.google.dart.tools.ui.text.config.IColorManagerExtension} since version 2.0
 * introducing the ability to bind and un-bind colors.</li>
 * </ul>
 * </p>
 * <p>
 * This interface may be implemented by clients.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 * 
 * @see com.google.dart.tools.ui.text.config.IColorManagerExtension
 * @see com.google.dart.tools.ui.text.IDartColorConstants.IJavaScriptColorConstants
 */
public interface IColorManager extends ISharedTextColors {

  /**
   * Returns a color object for the given key. The color objects are remembered internally; the same
   * color object is returned for equal keys.
   * 
   * @param key the color key
   * @return the color object for the given key
   */
  Color getColor(String key);
}
