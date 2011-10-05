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
package com.google.dart.tools.ui.text.folding;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Contributors to the <code>org.eclipse.wst.jsdt.ui.foldingStructureProvider</code> extension point
 * can specify an implementation of this interface to be displayed on the JavaScript &gt; Editor
 * &gt; Folding preference page.
 * <p>
 * Clients may implement this interface.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public interface IDartFoldingPreferenceBlock {

  /**
   * Creates the control that will be displayed on the JavaScript &gt; Editor &gt; Folding
   * preference page.
   * 
   * @param parent the parent composite to which to add the preferences control
   * @return the control that was added to <code>parent</code>
   */
  Control createControl(Composite parent);

  /**
   * Called when the preference page is being disposed. Implementations should free any resources
   * they are holding on to.
   */
  void dispose();

  /**
   * Called after creating the control. Implementations should load the preferences values and
   * update the controls accordingly.
   */
  void initialize();

  /**
   * Called when the <code>Defaults</code> button is pressed on the preference page. Implementation
   * should reset any preference settings to their default values and adjust the controls
   * accordingly.
   */
  void performDefaults();

  /**
   * Called when the <code>OK</code> button is pressed on the preference page. Implementations
   * should commit the configured preference settings into their form of preference storage.
   */
  void performOk();

}
