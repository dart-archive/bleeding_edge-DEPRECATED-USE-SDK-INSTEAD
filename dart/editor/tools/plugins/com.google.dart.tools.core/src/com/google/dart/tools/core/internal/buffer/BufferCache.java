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
package com.google.dart.tools.core.internal.buffer;

import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.internal.cache.LRUCache;
import com.google.dart.tools.core.internal.cache.OverflowingLRUCache;
import com.google.dart.tools.core.internal.model.OpenableElementImpl;
import com.google.dart.tools.core.model.OpenableElement;

import java.util.ArrayList;

/**
 * Instances of the class <code>BufferCache</code> implement an LRU cache of <code>Buffers</code>.
 */
public class BufferCache extends OverflowingLRUCache<OpenableElement, Buffer> {

  private ThreadLocal<ArrayList<Buffer>> buffersToClose = new ThreadLocal<ArrayList<Buffer>>();

  /**
   * Constructs a new buffer cache of the given size.
   */
  public BufferCache(int size) {
    super(size);
  }

  /**
   * Constructs a new buffer cache of the given size.
   */
  public BufferCache(int size, int overflow) {
    super(size, overflow);
  }

  /**
   * Returns true if the buffer is successfully closed and removed from the cache, otherwise false.
   * <p>
   * NOTE: this triggers an external removal of this buffer by closing the buffer.
   */
  @Override
  protected boolean close(LRUCacheEntry<OpenableElement, Buffer> entry) {
    Buffer buffer = entry.value;
    // prevent buffer that have unsaved changes or working copy buffer to be
    // removed
    // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=39311
    if (!((OpenableElementImpl) buffer.getOwner()).canBufferBeRemovedFromCache(buffer)) {
      return false;
    } else {
      ArrayList<Buffer> buffers = buffersToClose.get();
      if (buffers == null) {
        buffers = new ArrayList<Buffer>();
        buffersToClose.set(buffers);
      }
      buffers.add(buffer);
      return true;
    }
  }

  /**
   * Returns a new instance of the receiver.
   */
  @Override
  protected LRUCache<OpenableElement, Buffer> newInstance(int size, int overflow) {
    return new BufferCache(size, overflow);
  }

  void closeBuffers() {
    ArrayList<Buffer> buffers = buffersToClose.get();
    if (buffers == null) {
      return;
    }
    buffersToClose.set(null);
    for (int i = 0, length = buffers.size(); i < length; i++) {
      buffers.get(i).close();
    }
  }
}
