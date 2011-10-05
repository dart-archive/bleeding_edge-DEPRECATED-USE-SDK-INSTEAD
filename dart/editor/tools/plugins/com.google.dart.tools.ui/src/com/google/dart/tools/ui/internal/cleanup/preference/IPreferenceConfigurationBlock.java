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
package com.google.dart.tools.ui.internal.cleanup.preference;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Interface for preference configuration blocks which can either be wrapped by a
 * {@link org.eclipse.jdt.internal.ui.preferences.AbstractConfigurationBlockPreferencePage} or be
 * included some preference page.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @since 3.0
 */
public interface IPreferenceConfigurationBlock {

  /**
   * Creates the preference control.
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
