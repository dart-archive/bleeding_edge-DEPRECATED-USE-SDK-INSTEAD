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

/**
 * Interface for preference and property configuration blocks which can either be wrapped by a
 * {@link org.eclipse.jdt.internal.ui.preferences.AbstractConfigurationBlockPreferenceAndPropertyPage}
 * or be included some preference page.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @since 3.3
 */
public interface IPreferenceAndPropertyConfigurationBlock extends IPreferenceConfigurationBlock {

  /**
   * Disable project specific settings for the settings configured by this block.
   */
  public abstract void disableProjectSettings();

  /**
   * Enabled project specific settings for the settings configured by this block.
   */
  public abstract void enableProjectSettings();

}
