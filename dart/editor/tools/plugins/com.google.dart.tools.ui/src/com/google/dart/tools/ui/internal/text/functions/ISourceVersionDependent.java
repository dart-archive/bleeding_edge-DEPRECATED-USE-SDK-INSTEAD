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
package com.google.dart.tools.ui.internal.text.functions;

/**
 * Mix-in for any rule that changes its behavior based on the Java source version.
 */
public interface ISourceVersionDependent {

  /**
   * Sets the configured java source version to one of the <code>JavaScriptCore.VERSION_X_Y</code>
   * values.
   * 
   * @param version the new java source version
   * @see org.eclipse.wst.jsdt.core.JavaScriptCore
   */
  void setSourceVersion(String version);
}
