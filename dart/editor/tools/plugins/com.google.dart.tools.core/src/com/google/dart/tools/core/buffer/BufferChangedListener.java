/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.buffer;

/**
 * The interface <code>BufferChangedListener</code> defines the behavior of objects that implement a
 * listener that gets notified when the contents of a specific buffer have changed, or when the
 * buffer is closed. When a buffer is closed, the listener is notified <em>after</em> the buffer has
 * been closed. A listener is not notified when a buffer is saved.
 */
public interface BufferChangedListener {
  /**
   * Notifies that the given event has occurred.
   * 
   * @param event the change event
   */
  public void bufferChanged(BufferChangedEvent event);
}
