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
package com.google.dart.tools.ui.internal.cleanup;

import com.google.dart.tools.ui.cleanup.CleanUpOptions;
import com.google.dart.tools.ui.cleanup.ICleanUpOptionsInitializer;

/**
 * The clean up initializer for clean up mode.
 * 
 * @since 3.5
 */
public class DefaultCleanUpOptionsInitializer implements ICleanUpOptionsInitializer {

  /*
   * @see
   * org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer#setDefaultOptions(org.eclipse.jdt.ui.
   * cleanup.CleanUpOptions)
   * 
   * @since 3.5
   */
  @Override
  public void setDefaultOptions(CleanUpOptions options) {
    CleanUpConstants.setDefaultOptions(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS, options);
  }

}
