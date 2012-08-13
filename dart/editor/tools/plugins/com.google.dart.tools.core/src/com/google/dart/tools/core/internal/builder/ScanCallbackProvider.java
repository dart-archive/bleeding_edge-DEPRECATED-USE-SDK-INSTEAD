/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.analysis.ScanCallback;

/**
 * Indirect way for the UI to hook a {@link ScanCallback} into the build process to allow users to
 * cancel analysis of a new project.
 */
public abstract class ScanCallbackProvider {
  private static ScanCallbackProvider provider;

  public static ScanCallbackProvider getProvider() {
    return provider;
  }

  public static void setProvider(ScanCallbackProvider provider) {
    ScanCallbackProvider.provider = provider;
  }

  public abstract ScanCallback newCallback();
}
