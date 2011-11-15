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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.buffer.BufferChangedEvent;
import com.google.dart.tools.core.buffer.BufferChangedListener;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.OpenableElement;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Adapts <code>IDocument</code> to <code>Buffer</code>. Uses the same algorithm as the text widget
 * to determine the buffer's line delimiter. All text inserted into the buffer is converted to this
 * line delimiter. This class is <code>public</code> for test purposes only.
 */
public class DocumentAdapter implements Buffer, IDocumentListener {

  /**
   * Executes a document replace call in the UI thread.
   */
  protected class DocumentReplaceCommand implements Runnable {

    private int offset;
    private int length;
    private String text;

    public void replace(int offset, int length, String text) {
      this.offset = offset;
      this.length = length;
      this.text = text;
      DocumentAdapter.run(this);
    }

    @Override
    public void run() {
      try {
        if (!isClosed()) {
          document.replace(offset, length, text);
        }
      } catch (BadLocationException x) {
        // ignore
      }
    }
  }

  /**
   * Executes a document set content call in the UI thread.
   */
  protected class DocumentSetCommand implements Runnable {

    private String contents;

    @Override
    public void run() {
      if (!isClosed()) {
        document.set(contents);
      }
    }

    public void set(String contents) {
      this.contents = contents;
      DocumentAdapter.run(this);
    }
  }

  /**
   * Internal implementation of a NULL instance of Buffer.
   */
  static private class NullBuffer implements Buffer {

    @Override
    public void addBufferChangedListener(BufferChangedListener listener) {
    }

    @Override
    public void append(char[] text) {
    }

    @Override
    public void append(String text) {
    }

    @Override
    public void close() {
    }

    @Override
    public char getChar(int position) {
      return 0;
    }

    @Override
    public char[] getCharacters() {
      return null;
    }

    @Override
    public String getContents() {
      return null;
    }

    @Override
    public int getLength() {
      return 0;
    }

    @Override
    public OpenableElement getOwner() {
      return null;
    }

    @Override
    public String getText(int offset, int length) {
      return null;
    }

    @Override
    public IResource getUnderlyingResource() {
      return null;
    }

    @Override
    public boolean hasUnsavedChanges() {
      return false;
    }

    @Override
    public boolean isClosed() {
      return false;
    }

    @Override
    public boolean isReadOnly() {
      return true;
    }

    @Override
    public void removeBufferChangedListener(BufferChangedListener listener) {
    }

    @Override
    public void replace(int position, int length, char[] text) {
    }

    @Override
    public void replace(int position, int length, String text) {
    }

    @Override
    public void save(IProgressMonitor progress, boolean force) throws DartModelException {
    }

    @Override
    public void setContents(char[] contents) {
    }

    @Override
    public void setContents(String contents) {
    }
  }

  /** NULL implementing <code>Buffer</code> */
  public static final Buffer NULL = new NullBuffer();

  private static final boolean DEBUG_LINE_DELIMITERS = true;

  /**
   * Run the given runnable in the UI thread.
   * 
   * @param runnable the runnable
   */
  private static final void run(Runnable runnable) {
    Display currentDisplay = Display.getCurrent();
    if (currentDisplay != null) {
      runnable.run();
    } else {
      try {
        currentDisplay = Display.getDefault();
      } catch (Throwable exception) {
        // We can't get the display, possibly because we're running headless. Fall through to run
        // the runnable anyway.
      }
      if (currentDisplay == null) {
        runnable.run();
      } else {
        currentDisplay.syncExec(runnable);
      }
    }
  }

  private OpenableElement owner;
  private IFile file;
  private ITextFileBuffer textFileBuffer;
  private IDocument document;

  private DocumentSetCommand setCmd;
  private DocumentReplaceCommand replaceCmd;

  private Set<String> legalLineDelimiters;

  private List<BufferChangedListener> bufferListeners;

  private IStatus status;
  private IPath path;
  private LocationKind locationKind;

  /**
   * Constructs a new document adapter.
   * 
   * @param owner the owner of this buffer
   * @param file the <code>IFile</code> that backs the buffer
   */
  public DocumentAdapter(OpenableElement owner, IFile file) {
    this(owner);
    this.file = file;
    this.path = file.getFullPath();
    this.locationKind = LocationKind.IFILE;

    initialize();
  }

  /**
   * Constructs a new document adapter.
   * 
   * @param owner the owner of this buffer
   * @param path the path of the file that backs the buffer
   */
  public DocumentAdapter(OpenableElement owner, IPath path) {
    this(owner);
    Assert.isLegal(path != null);

    this.path = path;
    this.locationKind = LocationKind.NORMALIZE;

    initialize();
  }

  private DocumentAdapter(OpenableElement owner) {
    this.owner = owner;
    this.bufferListeners = new ArrayList<BufferChangedListener>(3);
    this.setCmd = new DocumentSetCommand();
    this.replaceCmd = new DocumentReplaceCommand();
  }

  /*
   * @see Buffer#addBufferChangedListener(IBufferChangedListener)
   */
  @Override
  public void addBufferChangedListener(BufferChangedListener listener) {
    Assert.isNotNull(listener);
    if (!bufferListeners.contains(listener)) {
      bufferListeners.add(listener);
    }
  }

  /*
   * @see Buffer#append(char[])
   */
  @Override
  public void append(char[] text) {
    append(new String(text));
  }

  /*
   * @see Buffer#append(String)
   */
  @Override
  public void append(String text) {
    if (DEBUG_LINE_DELIMITERS) {
      validateLineDelimiters(text);
    }
    replaceCmd.replace(document.getLength(), 0, text);
  }

  /*
   * @see Buffer#close()
   */
  @Override
  public void close() {

    if (isClosed()) {
      return;
    }

    IDocument d = document;
    document = null;
    d.removePrenotifiedDocumentListener(this);

    if (textFileBuffer != null) {
      ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
      try {
        manager.disconnect(path, locationKind, new NullProgressMonitor());
      } catch (CoreException x) {
        // ignore
      }
      textFileBuffer = null;
    }

    fireBufferChanged(new BufferChangedEvent(this, 0, 0, null));
    bufferListeners.clear();
  }

  /*
   * @see IDocumentListener#documentAboutToBeChanged(DocumentEvent)
   */
  @Override
  public void documentAboutToBeChanged(DocumentEvent event) {
    // there is nothing to do here
  }

  /*
   * @see IDocumentListener#documentChanged(DocumentEvent)
   */
  @Override
  public void documentChanged(DocumentEvent event) {
    fireBufferChanged(new BufferChangedEvent(this, event.getOffset(), event.getLength(),
        event.getText()));
  }

  /*
   * @see Buffer#getChar(int)
   */
  @Override
  public char getChar(int position) {
    try {
      return document.getChar(position);
    } catch (BadLocationException x) {
      throw new ArrayIndexOutOfBoundsException();
    }
  }

  /*
   * @see Buffer#getCharacters()
   */
  @Override
  public char[] getCharacters() {
    String content = getContents();
    return content == null ? null : content.toCharArray();
  }

  /*
   * @see Buffer#getContents()
   */
  @Override
  public String getContents() {
    return document.get();
  }

  /**
   * Returns the adapted document.
   * 
   * @return the adapted document
   */
  public IDocument getDocument() {
    return document;
  }

  /*
   * @see Buffer#getLength()
   */
  @Override
  public int getLength() {
    return document.getLength();
  }

  /*
   * @see Buffer#getOwner()
   */
  @Override
  public OpenableElement getOwner() {
    return owner;
  }

  /**
   * Returns the status of this document adapter.
   * 
   * @return the status
   */
  public IStatus getStatus() {
    if (status != null) {
      return status;
    }
    if (textFileBuffer != null) {
      return textFileBuffer.getStatus();
    }
    return null;
  }

  /*
   * @see Buffer#getText(int, int)
   */
  @Override
  public String getText(int offset, int length) {
    try {
      return document.get(offset, length);
    } catch (BadLocationException x) {
      throw new ArrayIndexOutOfBoundsException();
    }
  }

  /*
   * @see Buffer#getUnderlyingResource()
   */
  @Override
  public IResource getUnderlyingResource() {
    return file;
  }

  /*
   * @see Buffer#hasUnsavedChanges()
   */
  @Override
  public boolean hasUnsavedChanges() {
    return textFileBuffer != null ? textFileBuffer.isDirty() : false;
  }

  /*
   * @see Buffer#isClosed()
   */
  @Override
  public boolean isClosed() {
    return document == null;
  }

  /*
   * @see Buffer#isReadOnly()
   */
  @Override
  public boolean isReadOnly() {
    if (textFileBuffer != null) {
      return !textFileBuffer.isCommitable();
    }

    IResource resource = getUnderlyingResource();
    if (resource == null) {
      return true;
    }

    final ResourceAttributes attributes = resource.getResourceAttributes();
    return attributes == null ? false : attributes.isReadOnly();
  }

  /*
   * @see Buffer#removeBufferChangedListener(IBufferChangedListener)
   */
  @Override
  public void removeBufferChangedListener(BufferChangedListener listener) {
    Assert.isNotNull(listener);
    bufferListeners.remove(listener);
  }

  /*
   * @see Buffer#replace(int, int, char[])
   */
  @Override
  public void replace(int position, int length, char[] text) {
    replace(position, length, new String(text));
  }

  /*
   * @see Buffer#replace(int, int, String)
   */
  @Override
  public void replace(int position, int length, String text) {
    if (DEBUG_LINE_DELIMITERS) {
      validateLineDelimiters(text);
    }
    replaceCmd.replace(position, length, text);
  }

  /*
   * @see Buffer#save(IProgressMonitor, boolean)
   */
  @Override
  public void save(IProgressMonitor progress, boolean force) throws DartModelException {
    try {
      if (textFileBuffer != null) {
        textFileBuffer.commit(progress, force);
      }
    } catch (CoreException e) {
      throw new DartModelException(e);
    }
  }

  /*
   * @see Buffer#setContents(char[])
   */
  @Override
  public void setContents(char[] contents) {
    setContents(new String(contents));
  }

  /*
   * @see Buffer#setContents(String)
   */
  @Override
  public void setContents(String contents) {
    int oldLength = document.getLength();

    if (contents == null) {

      if (oldLength != 0) {
        setCmd.set(""); //$NON-NLS-1$
      }

    } else {

      // set only if different
      if (DEBUG_LINE_DELIMITERS) {
        validateLineDelimiters(contents);
      }

      if (!contents.equals(document.get())) {
        setCmd.set(contents);
      }
    }
  }

  private void fireBufferChanged(BufferChangedEvent event) {
    if (bufferListeners != null && bufferListeners.size() > 0) {
      Iterator<BufferChangedListener> e = new ArrayList<BufferChangedListener>(bufferListeners).iterator();
      while (e.hasNext()) {
        e.next().bufferChanged(event);
      }
    }
  }

  private void initialize() {
    ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
    try {
      manager.connect(path, locationKind, new NullProgressMonitor());
      textFileBuffer = manager.getTextFileBuffer(path, locationKind);
      document = textFileBuffer.getDocument();
    } catch (CoreException x) {
      status = x.getStatus();
      document = manager.createEmptyDocument(path, locationKind);
      if (document instanceof ISynchronizable) {
        ((ISynchronizable) document).setLockObject(new Object());
      }
    }
    document.addPrenotifiedDocumentListener(this);
  }

  private void validateLineDelimiters(String contents) {

    if (legalLineDelimiters == null) {
      // collect all line delimiters in the document
      Set<String> existingDelimiters = new HashSet<String>();

      for (int i = document.getNumberOfLines() - 1; i >= 0; i--) {
        try {
          String curr = document.getLineDelimiter(i);
          if (curr != null) {
            existingDelimiters.add(curr);
          }
        } catch (BadLocationException e) {
          DartToolsPlugin.log(e);
        }
      }
      if (existingDelimiters.isEmpty()) {
        return; // first insertion of a line delimiter: no test
      }
      legalLineDelimiters = existingDelimiters;

    }

    DefaultLineTracker tracker = new DefaultLineTracker();
    tracker.set(contents);

    int lines = tracker.getNumberOfLines();
    if (lines <= 1) {
      return;
    }

    for (int i = 0; i < lines; i++) {
      try {
        String curr = tracker.getLineDelimiter(i);
        if (curr != null && !legalLineDelimiters.contains(curr)) {
          StringBuffer buf = new StringBuffer(
              "WARNING: javaeditor.DocumentAdapter added new line delimiter to code: "); //$NON-NLS-1$
          for (int k = 0; k < curr.length(); k++) {
            if (k > 0) {
              buf.append(' ');
            }
            buf.append((int) curr.charAt(k));
          }
          IStatus status = new Status(IStatus.WARNING, DartUI.ID_PLUGIN, IStatus.OK,
              buf.toString(), new Throwable());
          DartToolsPlugin.log(status);
        }
      } catch (BadLocationException e) {
        DartToolsPlugin.log(e);
      }
    }
  }
}
