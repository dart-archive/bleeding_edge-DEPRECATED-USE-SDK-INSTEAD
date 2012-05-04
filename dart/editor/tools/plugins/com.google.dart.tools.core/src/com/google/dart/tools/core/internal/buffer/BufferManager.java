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
package com.google.dart.tools.core.internal.buffer;

import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.internal.model.OpenableElementImpl;
import com.google.dart.tools.core.model.OpenableElement;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import java.util.Iterator;

/**
 * The unique instance of the class <code>BufferManager</code> manages the set of open buffers by
 * implementing a LRU cache of buffers.
 */
public class BufferManager {
  public static Buffer createBuffer(OpenableElement owner) {
    OpenableElementImpl element = (OpenableElementImpl) owner;
    IResource resource = element.resource();
    return new FileBuffer(
        resource instanceof IFile ? (IFile) resource : null,
        owner,
        element.isReadOnly());
  }

  public static Buffer createNullBuffer(OpenableElement owner) {
    OpenableElementImpl element = (OpenableElementImpl) owner;
    IResource resource = element.resource();
    return new NullBuffer(
        resource instanceof IFile ? (IFile) resource : null,
        owner,
        element.isReadOnly());
  }

  /**
   * LRU cache of buffers. The key and value for an entry in the table is the identical buffer.
   */
  private BufferCache openBuffers = new BufferCache(60);

  /**
   * The unique instance of this class.
   */
  private static final BufferManager UniqueInstance = new BufferManager();

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static BufferManager getInstance() {
    return UniqueInstance;
  }

  /**
   * Disallow the creation of instances of this class.
   */
  private BufferManager() {
    super();
  }

  /**
   * Adds a buffer to the table of open buffers.
   */
  public void addBuffer(Buffer buffer) {
    // if (VERBOSE) {
    // String owner = buffer.getOwner().toStringWithAncestors();
    //      System.out.println("Adding buffer for " + owner); //$NON-NLS-1$
    // }
    synchronized (openBuffers) {
      openBuffers.put(buffer.getOwner(), buffer);
    }
    // close buffers that were removed from the cache if space was needed
    openBuffers.closeBuffers();
    // if (VERBOSE) {
    //      System.out.println("-> Buffer cache filling ratio = " + NumberFormat.getInstance().format(openBuffers.fillingRatio()) + "%"); //$NON-NLS-1$//$NON-NLS-2$
    // }
  }

  /**
   * Returns the open buffer associated with the given owner, or <code>null</code> if the owner does
   * not have an open buffer associated with it.
   */
  public Buffer getBuffer(OpenableElement owner) {
    synchronized (openBuffers) {
      return openBuffers.get(owner);
    }
  }

  /**
   * Return an iterator that returns all of the open buffers with the most recently used first.
   * 
   * @return an iterator that returns all of the open buffers
   */
  public Iterator<Buffer> getOpenBuffers() {
    Iterator<Buffer> result;
    synchronized (openBuffers) {
      openBuffers.shrink();
      result = openBuffers.iterator();
    }
    // close buffers that were removed from the cache if space was needed
    openBuffers.closeBuffers();
    return result;
  }

  /**
   * Remove the given buffer from the table of open buffers.
   * 
   * @param buffer the buffer to be removed
   */
  public void removeBuffer(Buffer buffer) {
    // if (VERBOSE) {
    // String owner = buffer.getOwner().toStringWithAncestors();
    //      System.out.println("Removing buffer for " + owner); //$NON-NLS-1$
    // }
    synchronized (openBuffers) {
      openBuffers.remove(buffer.getOwner());
    }
    // close buffers that were removed from the cache (should be only one)
    openBuffers.closeBuffers();
    // if (VERBOSE) {
    //      System.out.println("-> Buffer cache filling ratio = " + NumberFormat.getInstance().format(openBuffers.fillingRatio()) + "%"); //$NON-NLS-1$//$NON-NLS-2$
    // }
  }
}
