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
import com.google.dart.tools.core.buffer.BufferChangedEvent;
import com.google.dart.tools.core.buffer.BufferChangedListener;
import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.OpenableElement;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.content.IContentDescription;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Instances of the class <code>FileBuffer</code> implement a buffer whose contents is associated
 * with a {@link IFile file}.
 */
public class FileBuffer implements Buffer {
  private IFile file;
  private int flags;
  private char[] contents;
  private ArrayList<BufferChangedListener> changeListeners;
  private OpenableElement owner;
  private int gapStart = -1;
  private int gapEnd = -1;

  private Object lock = new Object();

  private static final int F_HAS_UNSAVED_CHANGES = 1;
  private static final int F_IS_READ_ONLY = 2;
  private static final int F_IS_CLOSED = 4;

  /**
   * Initialize a newly created buffer on an underlying resource.
   */
  protected FileBuffer(IFile file, OpenableElement owner, boolean readOnly) {
    this.file = file;
    this.owner = owner;
    if (file == null) {
      setReadOnly(readOnly);
    }
  }

  @Override
  public synchronized void addBufferChangedListener(BufferChangedListener listener) {
    if (changeListeners == null) {
      changeListeners = new ArrayList<BufferChangedListener>(5);
    }
    if (!changeListeners.contains(listener)) {
      changeListeners.add(listener);
    }
  }

  /**
   * Append the <code>text</code> to the actual content, the gap is moved to the end of the
   * <code>text</code>.
   */
  @Override
  public void append(char[] text) {
    if (!isReadOnly()) {
      if (text == null || text.length == 0) {
        return;
      }
      int length = getLength();
      synchronized (lock) {
        if (contents == null) {
          return;
        }
        moveAndResizeGap(length, text.length);
        System.arraycopy(text, 0, contents, length, text.length);
        gapStart += text.length;
        flags |= F_HAS_UNSAVED_CHANGES;
      }
      notifyChanged(new BufferChangedEvent(this, length, 0, new String(text)));
    }
  }

  /**
   * Append the <code>text</code> to the actual content, the gap is moved to the end of the
   * <code>text</code>.
   */
  @Override
  public void append(String text) {
    if (text == null) {
      return;
    }
    append(text.toCharArray());
  }

  @Override
  public void close() {
    BufferChangedEvent event = null;
    synchronized (lock) {
      if (isClosed()) {
        return;
      }
      event = new BufferChangedEvent(this, 0, 0, null);
      contents = null;
      flags |= F_IS_CLOSED;
    }
    notifyChanged(event); // notify outside of synchronized block
    synchronized (this) { // ensure that no other thread is adding/removing a
                          // listener at the same time
                          // (https://bugs.eclipse.org/bugs/show_bug.cgi?id=126673)
      changeListeners = null;
    }
  }

  @Override
  public char getChar(int position) {
    synchronized (lock) {
      if (contents == null) {
        return Character.MIN_VALUE;
      }
      if (position < gapStart) {
        return contents[position];
      }
      int gapLength = gapEnd - gapStart;
      return contents[position + gapLength];
    }
  }

  @Override
  public char[] getCharacters() {
    synchronized (lock) {
      if (contents == null) {
        return null;
      }
      if (gapStart < 0) {
        return contents;
      }
      int length = contents.length;
      char[] newContents = new char[length - gapEnd + gapStart];
      System.arraycopy(contents, 0, newContents, 0, gapStart);
      System.arraycopy(contents, gapEnd, newContents, gapStart, length - gapEnd);
      return newContents;
    }
  }

  @Override
  public String getContents() {
    char[] chars = getCharacters();
    if (chars == null) {
      return null;
    }
    return new String(chars);
  }

  @Override
  public int getLength() {
    synchronized (lock) {
      if (contents == null) {
        return -1;
      }
      int length = gapEnd - gapStart;
      return (contents.length - length);
    }
  }

  @Override
  public OpenableElement getOwner() {
    return owner;
  }

  @Override
  public String getText(int offset, int length) {
    synchronized (lock) {
      if (contents == null) {
        return ""; //$NON-NLS-1$
      }
      if (offset + length < gapStart) {
        return new String(contents, offset, length);
      }
      if (gapStart < offset) {
        int gapLength = gapEnd - gapStart;
        return new String(contents, offset + gapLength, length);
      }
      StringBuffer buf = new StringBuffer();
      buf.append(contents, offset, gapStart - offset);
      buf.append(contents, gapEnd, offset + length - gapStart);
      return buf.toString();
    }
  }

  @Override
  public IResource getUnderlyingResource() {
    return file;
  }

  @Override
  public boolean hasUnsavedChanges() {
    return (flags & F_HAS_UNSAVED_CHANGES) != 0;
  }

  @Override
  public boolean isClosed() {
    return (flags & F_IS_CLOSED) != 0;
  }

  @Override
  public boolean isReadOnly() {
    return (flags & F_IS_READ_ONLY) != 0;
  }

  @Override
  public synchronized void removeBufferChangedListener(BufferChangedListener listener) {
    if (changeListeners != null) {
      changeListeners.remove(listener);
      if (changeListeners.size() == 0) {
        changeListeners = null;
      }
    }
  }

  /**
   * Replace <code>length</code> characters starting from <code>position</code> with
   * <code>text<code>.
   * After that operation, the gap is placed at the end of the
   * inserted <code>text</code>.
   */
  @Override
  public void replace(int position, int length, char[] text) {
    if (!isReadOnly()) {
      int textLength = text == null ? 0 : text.length;
      synchronized (lock) {
        if (contents == null) {
          return;
        }

        // move gap
        moveAndResizeGap(position + length, textLength - length);

        // overwrite
        int min = Math.min(textLength, length);
        if (min > 0) {
          System.arraycopy(text, 0, contents, position, min);
        }
        if (length > textLength) {
          // enlarge the gap
          gapStart -= length - textLength;
        } else if (textLength > length) {
          // shrink gap
          gapStart += textLength - length;
          System.arraycopy(text, 0, contents, position, textLength);
        }
        flags |= F_HAS_UNSAVED_CHANGES;
      }
      String string = null;
      if (textLength > 0) {
        string = new String(text);
      }
      notifyChanged(new BufferChangedEvent(this, position, length, string));
    }
  }

  /**
   * Replace <code>length</code> characters starting from <code>position</code> with
   * <code>text<code>.
   * After that operation, the gap is placed at the end of the
   * inserted <code>text</code>.
   */
  @Override
  public void replace(int position, int length, String text) {
    replace(position, length, text == null ? null : text.toCharArray());
  }

  @Override
  public void save(IProgressMonitor progress, boolean force) throws DartModelException {
    // determine if saving is required
    if (isReadOnly() || file == null) {
      return;
    }
    if (!hasUnsavedChanges()) {
      return;
    }

    // use a platform operation to update the resource contents
    try {
      String stringContents = getContents();
      if (stringContents == null) {
        return;
      }

      // Get encoding
      String encoding = null;
      try {
        encoding = file.getCharset();
      } catch (CoreException ce) {
        // use no encoding
      }

      // Create bytes array
      byte[] bytes = encoding == null ? stringContents.getBytes()
          : stringContents.getBytes(encoding);

      // Special case for UTF-8 BOM files
      // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=110576
      if (encoding != null && encoding.equals("UTF-8")) {
        IContentDescription description = file.getContentDescription();
        if (description != null
            && description.getProperty(IContentDescription.BYTE_ORDER_MARK) != null) {
          int bomLength = IContentDescription.BOM_UTF_8.length;
          byte[] bytesWithBOM = new byte[bytes.length + bomLength];
          System.arraycopy(IContentDescription.BOM_UTF_8, 0, bytesWithBOM, 0, bomLength);
          System.arraycopy(bytes, 0, bytesWithBOM, bomLength, bytes.length);
          bytes = bytesWithBOM;
        }
      }

      // Set file contents
      ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
      if (file.exists()) {
        file.setContents(stream, force ? IResource.FORCE | IResource.KEEP_HISTORY
            : IResource.KEEP_HISTORY, null);
      } else {
        file.create(stream, force, null);
      }
    } catch (IOException e) {
      throw new DartModelException(e, DartModelStatusConstants.IO_EXCEPTION);
    } catch (CoreException e) {
      throw new DartModelException(e);
    }

    // the resource no longer has unsaved changes
    flags &= ~(F_HAS_UNSAVED_CHANGES);
  }

  @Override
  public void setContents(char[] newContents) {
    // allow special case for first initialization
    // after creation by buffer factory
    if (contents == null) {
      synchronized (lock) {
        contents = newContents;
        flags &= ~(F_HAS_UNSAVED_CHANGES);
      }
      return;
    }

    if (!isReadOnly()) {
      String string = null;
      if (newContents != null) {
        string = new String(newContents);
      }
      synchronized (lock) {
        if (contents == null) {
          return; // ignore if buffer is closed (as per spec)
        }
        contents = newContents;
        flags |= F_HAS_UNSAVED_CHANGES;
        gapStart = -1;
        gapEnd = -1;
      }
      BufferChangedEvent event = new BufferChangedEvent(this, 0, getLength(), string);
      notifyChanged(event);
    }
  }

  @Override
  public void setContents(String newContents) {
    setContents(newContents.toCharArray());
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Owner: " + ((DartElementImpl) owner).toStringWithAncestors()); //$NON-NLS-1$
    buffer.append("\nHas unsaved changes: " + hasUnsavedChanges()); //$NON-NLS-1$
    buffer.append("\nIs readonly: " + isReadOnly()); //$NON-NLS-1$
    buffer.append("\nIs closed: " + isClosed()); //$NON-NLS-1$
    buffer.append("\nContents:\n"); //$NON-NLS-1$
    char[] charContents = getCharacters();
    if (charContents == null) {
      buffer.append("<null>"); //$NON-NLS-1$
    } else {
      int length = charContents.length;
      for (int i = 0; i < length; i++) {
        char c = charContents[i];
        switch (c) {
          case '\n':
            buffer.append("\\n\n"); //$NON-NLS-1$
            break;
          case '\r':
            if (i < length - 1 && contents[i + 1] == '\n') {
              buffer.append("\\r\\n\n"); //$NON-NLS-1$
              i++;
            } else {
              buffer.append("\\r\n"); //$NON-NLS-1$
            }
            break;
          default:
            buffer.append(c);
            break;
        }
      }
    }
    return buffer.toString();
  }

  /**
   * Moves the gap to location and adjust its size to the anticipated change size. The size
   * represents the expected range of the gap that will be filled after the gap has been moved. Thus
   * the gap is resized to actual size + the specified size and moved to the given position.
   */
  private void moveAndResizeGap(int position, int size) {
    char[] content = null;
    int oldSize = gapEnd - gapStart;
    if (size < 0) {
      if (oldSize > 0) {
        content = new char[contents.length - oldSize];
        System.arraycopy(contents, 0, content, 0, gapStart);
        System.arraycopy(contents, gapEnd, content, gapStart, content.length - gapStart);
        contents = content;
      }
      gapStart = gapEnd = position;
      return;
    }
    content = new char[contents.length + (size - oldSize)];
    int newGapStart = position;
    int newGapEnd = newGapStart + size;
    if (oldSize == 0) {
      System.arraycopy(contents, 0, content, 0, newGapStart);
      System.arraycopy(contents, newGapStart, content, newGapEnd, content.length - newGapEnd);
    } else if (newGapStart < gapStart) {
      int delta = gapStart - newGapStart;
      System.arraycopy(contents, 0, content, 0, newGapStart);
      System.arraycopy(contents, newGapStart, content, newGapEnd, delta);
      System.arraycopy(contents, gapEnd, content, newGapEnd + delta, contents.length - gapEnd);
    } else {
      int delta = newGapStart - gapStart;
      System.arraycopy(contents, 0, content, 0, gapStart);
      System.arraycopy(contents, gapEnd, content, gapStart, delta);
      System.arraycopy(contents, gapEnd + delta, content, newGapEnd, content.length - newGapEnd);
    }
    contents = content;
    gapStart = newGapStart;
    gapEnd = newGapEnd;
  }

  /**
   * Notify the listeners that this buffer has changed. To avoid deadlock, this should not be called
   * in a synchronized block.
   */
  private void notifyChanged(final BufferChangedEvent event) {
    ArrayList<BufferChangedListener> listeners = changeListeners;
    if (listeners != null) {
      for (int i = 0, size = listeners.size(); i < size; ++i) {
        final BufferChangedListener listener = listeners.get(i);
        SafeRunner.run(new ISafeRunnable() {
          @Override
          public void handleException(Throwable exception) {
            Util.log(exception, "Exception occurred in listener of buffer change notification"); //$NON-NLS-1$
          }

          @Override
          public void run() throws Exception {
            listener.bufferChanged(event);
          }
        });

      }
    }
  }

  /**
   * Sets this <code>Buffer</code> to be read only.
   */
  private void setReadOnly(boolean readOnly) {
    if (readOnly) {
      flags |= F_IS_READ_ONLY;
    } else {
      flags &= ~(F_IS_READ_ONLY);
    }
  }
}
