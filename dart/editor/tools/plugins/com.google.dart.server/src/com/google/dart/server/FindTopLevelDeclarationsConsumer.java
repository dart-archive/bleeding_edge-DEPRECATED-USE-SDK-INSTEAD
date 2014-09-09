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

/**
 * The interface {@code FindTopLevelDeclarationsConsumer} defines the behavior of objects that
 * consume the find member references request.
 * 
 * @coverage dart.server
 */
public interface FindTopLevelDeclarationsConsumer extends Consumer {
  /**
   * A search id {@link String}.
   * 
   * @param searchId the identifier used to associate results with this search request
   */
  public void computedSearchId(String searchId);
}
