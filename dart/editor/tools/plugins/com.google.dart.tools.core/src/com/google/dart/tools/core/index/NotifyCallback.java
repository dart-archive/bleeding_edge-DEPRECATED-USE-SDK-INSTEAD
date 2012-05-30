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
package com.google.dart.tools.core.index;

import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;

/**
 * The interface <code>NotifyCallback</code> is used to find out when this callback is reached in
 * the index, this is used to determine how long the indexer takes to complete on start-up.
 * 
 * @see InMemoryIndex#notify(NotifyCallback)
 */
public interface NotifyCallback {
  /**
   * This method is invoked as soon as the index reaches this callback.
   */
  public void done();
}
