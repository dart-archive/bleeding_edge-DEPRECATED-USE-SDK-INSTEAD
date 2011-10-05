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
package com.google.dart.tools.ui.cleanup;

import org.eclipse.swt.widgets.Composite;

/**
 * Provides the UI to configure a clean up.
 * 
 * @since 3.5
 */
public interface ICleanUpConfigurationUI {

  /**
   * Creates the contents for this clean up configuration UI.
   * 
   * @param parent the parent composite
   * @return created content control
   */
  public Composite createContents(Composite parent);

  /**
   * Returns the number of clean ups that can be configured.
   * 
   * @return the number of clean ups that can be configured
   */
  public int getCleanUpCount();

  /**
   * A code snippet which complies to the current settings.
   * 
   * @return a code snippet
   */
  public String getPreview();

  /**
   * Returns the number of selected clean ups.
   * 
   * @return the number of selected clean ups at the moment
   */
  public int getSelectedCleanUpCount();

  /**
   * The options to modify in this section.
   * <p>
   * <strong>Note:</strong> If an option gets changed in the UI then this must immediately update
   * the corresponding option in the here given clean up options.
   * </p>
   * 
   * @param options the options to modify
   */
  public void setOptions(CleanUpOptions options);
}
