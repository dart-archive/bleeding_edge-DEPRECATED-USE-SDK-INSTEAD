/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.element.angular;

import com.google.dart.engine.source.Source;

/**
 * The interface {@code AngularHasTemplateElement} defines common behavior for
 * {@link AngularElement} that have template URI / {@link Source}.
 * 
 * @coverage dart.engine.element
 */
public interface AngularHasTemplateElement extends AngularElement {
  /**
   * Returns the HTML template {@link Source}, {@code null} if not resolved.
   */
  Source getTemplateSource();

  /**
   * Returns the HTML template URI.
   */
  String getTemplateUri();

  /**
   * Return the offset of the {@link #getTemplateUri()} in the {@link #getSource()}.
   * 
   * @return the offset of the template URI
   */
  int getTemplateUriOffset();
}
