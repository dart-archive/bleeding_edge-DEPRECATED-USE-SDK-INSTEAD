/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server;

import com.google.dart.server.generated.types.Element;

/**
 * The interface {@code FindElementReferencesConsumer} defines the behavior of objects consume the
 * find element references request.
 * 
 * @coverage dart.server
 */
public interface FindElementReferencesConsumer extends Consumer {
  /**
   * A search id {@link String}.
   * 
   * @param searchId the identifier used to associate results with this search request
   * @param element the element referenced or defined at the given offset and whose references will
   *          be returned in the search results. If no element was found at the given location, this
   *          field will be absent.
   */
  public void computedElementReferences(String searchId, Element element);
}
