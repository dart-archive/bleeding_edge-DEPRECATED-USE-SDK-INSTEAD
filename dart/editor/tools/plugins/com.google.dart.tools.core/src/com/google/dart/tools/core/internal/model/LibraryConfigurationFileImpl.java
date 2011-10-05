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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.ast.LibraryNode;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.internal.buffer.BufferManager;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartImportContainerInfo;
import com.google.dart.tools.core.internal.model.info.LibraryFileInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.LibraryConfigurationFile;
import com.google.dart.tools.core.problem.ProblemRequestor;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Instances of the class <code>LibraryConfigurationFileImpl</code> implement an object that
 * represents a Dart library configuration file that is either:
 * <ul>
 * <li>mapped into the Eclipse workspace... in which case: {@link #getResource()} returns non-
 * <code>null</code></li>
 * <li>external to the Eclipse workspace... in which case: {@link #getResource()} returns
 * <code>null</code> and {@link #isReadOnly()} returns <code>true</code></li>
 * </ul>
 */
public class LibraryConfigurationFileImpl extends SourceFileElementImpl<LibraryConfigurationFile>
    implements LibraryConfigurationFile {
  /**
   * Initialize a newly created library configuration file to be contained in the given library
   * using the default working copy owner.
   * 
   * @param library the library containing the configuration file
   * @param file the file being represented by the configuration file
   */
  public LibraryConfigurationFileImpl(DartLibraryImpl library, IFile file) {
    this(library, file, DefaultWorkingCopyOwner.getInstance());
  }

  /**
   * Initialize a newly created library configuration file to be contained in the given library.
   * 
   * @param library the library containing the configuration file
   * @param file the file being represented by the configuration file
   * @param owner the working copy owner
   */
  public LibraryConfigurationFileImpl(DartLibraryImpl library, IFile file, WorkingCopyOwner owner) {
    super(library, file, owner);
  }

  /**
   * Stop gap hack to add a source to an already existing library (Emphasis on HACK)
   * 
   * @param name
   * @throws IOException
   * @throws CoreException
   */
  public void addResource(String name) throws CoreException {
    LibraryUnit unit = DartCompilerUtilities.resolveLibrary(getLibrarySource(), null, null);
    Iterator<LibraryNode> itr = unit.getResourcePaths().iterator();
    int pos = 0;
    boolean isLast = true;
    boolean isEmpty = true;
    while (itr.hasNext()) {
      //this loop is to make sure we're inserting in alphabetical order
      isEmpty = false;
      LibraryNode node = itr.next();
      pos = node.getSourceStart();
      if (node.getText().compareTo(name) > 0) {
        isLast = false;
        break;
      }
      pos += node.getSourceLength();
    }
    Buffer buffer = getBuffer();
    String whole = buffer.getContents();
    final int indexOfResource = whole.indexOf("resource");
    if (indexOfResource == -1) {
      pos = whole.lastIndexOf('}');
      name = "  resource = [\n    \"" + name + "\"\n  ]\n";
    } else if (isEmpty) {
      pos = whole.indexOf("[", indexOfResource) + 1;
      name = "\n    \"" + name + "\"";
    } else if (isLast) {
      name = ",\n    " + "\"" + name + "\"";
    } else {
      name = "\"" + name + "\"" + ",\n    ";
    }
    String begin = whole.substring(0, pos);
    String end = whole.substring(pos);
    String rv = begin + name + end;
    getFile().setContents(new ByteArrayInputStream(rv.getBytes()), false, false,
        new NullProgressMonitor());
  }

  /**
   * Stop gap hack to add a source to an already existing library (Emphasis on HACK)
   * 
   * @param name
   * @throws IOException
   * @throws CoreException
   */
  public void addSource(String name) throws CoreException {
    LibraryUnit unit = DartCompilerUtilities.resolveLibrary(getLibrarySource(), null, null);
    Iterator<LibraryNode> itr = unit.getSourcePaths().iterator();
    int pos = 0;
    boolean isLast = true;
    boolean isEmpty = true;
    while (itr.hasNext()) {
      //this loop is to make sure we're inserting in alphabetical order
      isEmpty = false;
      LibraryNode node = itr.next();
      pos = node.getSourceStart();
      if (node.getText().compareTo(name) > 0) {
        isLast = false;
        break;
      }
      pos += node.getSourceLength();
    }
    Buffer buffer = getBuffer();
    String whole = buffer.getContents();
    if (isEmpty) {
      String source = "source = [";
      pos = whole.indexOf(source) + source.length();
      name = "\n    \"" + name + "\"";
    } else if (isLast) {
      name = ",\n    " + "\"" + name + "\"";
    } else {
      name = "\"" + name + "\"" + ",\n    ";
    }
    String begin = whole.substring(0, pos);
    String end = whole.substring(pos);
    String rv = begin + name + end;
    getFile().setContents(new ByteArrayInputStream(rv.getBytes()), false, false,
        new NullProgressMonitor());
  }

  @Override
  public void commitWorkingCopy(boolean force, IProgressMonitor monitor) throws DartModelException {
    //TODO(pquitslund): implement library working copy support
  }

  @Override
  public void discardWorkingCopy() throws DartModelException {
    //TODO(pquitslund): implement library working copy support
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LibraryConfigurationFileImpl)) {
      return false;
    }
    return super.equals(obj);
  }

  @Override
  public LibraryConfigurationFile findWorkingCopy(WorkingCopyOwner owner) {
    //TODO(pquitslund): implement library working copy support
    return null;
  }

  @Override
  public String getElementName() {
    if (getFile() == null) {
      return getLibrarySource().getName();
    }
    return super.getElementName();
  }

  @Override
  public int getElementType() {
    return 5;
  }

  @Override
  public IResource getResource() {
    if (getFile() == null) {
      return null;
    }
    return super.getResource();
  }

  @Override
  public boolean hasResourceChanged() {
    //TODO(pquitslund): implement library working copy support
    return false;
  }

  @Override
  public boolean isApplicationFile() {
    // TODO(brianwilkerson) Remove this method.
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return getFile() == null || super.isReadOnly();
  }

  @Override
  public void restore() throws DartModelException {
    //TODO(pquitslund): implement library working copy support
  }

  @Override
  protected void becomeWorkingCopy(ProblemRequestor requestor, IProgressMonitor monitor)
      throws DartModelException {
    //TODO(pquitslund): implement library working copy support
  }

  @Override
  protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm,
      Map<DartElement, DartElementInfo> newElements, final IResource underlyingResource)
      throws DartModelException {
    LibraryFileInfo libraryInfo = (LibraryFileInfo) info;
    LibrarySource sourceFile = getLibrarySource();
    DartImportContainerImpl container = new DartImportContainerImpl(this);
    ArrayList<DartElement> children = new ArrayList<DartElement>();

    LibraryUnit libUnit = DartCompilerUtilities.resolveLibrary(sourceFile, null, null);

    if (libUnit != null) {
      for (LibraryNode node : libUnit.getImportPaths()) {
        LibrarySource library;
        try {
          library = sourceFile.getImportFor(node.getText());
        } catch (IOException e) {
          throw new DartModelException(e, DartModelStatusConstants.IO_EXCEPTION);
        }
        children.add(new DartImportImpl(container, library));
      }
    }

    DartImportContainerInfo containerInfo = (DartImportContainerInfo) container.getElementInfo();
    containerInfo.setChildren(children.toArray(new DartElement[children.size()]));
    libraryInfo.setChildren(new DartElement[] {container});
    return true;
  }

  @Override
  protected void closing(DartElementInfo info) throws DartModelException {
    super.closing(info);
    // closeBuffer();
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return new LibraryFileInfo();
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    switch (token.charAt(0)) {
      case MEMENTO_DELIMITER_IMPORT_CONTAINER:
        DartImportContainerImpl container = new DartImportContainerImpl(this);
        return container.getHandleFromMemento(tokenizer, owner);
    }
    return null;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return '*';
  }

  @Override
  protected String getHandleMementoName() {
    // Because there is only one library file per library (or application file
    // per application), there is no need to specify a name for it.
    return ""; //$NON-NLS-1$
  }

  protected LibrarySource getLibrarySource() {
    DartLibraryImpl library = getAncestor(DartLibraryImpl.class);
    LibrarySource sourceFile = library.getLibrarySourceFile();
    return sourceFile;
  }

  @Override
  protected PerWorkingCopyInfo getPerWorkingCopyInfo() {
    //TODO(pquitslund): implement library working copy support
    return null;
  }

  @Override
  protected LibraryConfigurationFile getWorkingCopy(WorkingCopyOwner workingCopyOwner,
      ProblemRequestor problemRequestor, IProgressMonitor monitor) throws DartModelException {
    //TODO(pquitslund): implement library working copy support
    return null;
  }

  @Override
  protected boolean hasBuffer() {
    return true;
  }

  @Override
  protected Buffer openBuffer(IProgressMonitor pm, DartElementInfo info) throws DartModelException {
    // create buffer
    BufferManager bufManager = getBufferManager();
    boolean isWorkingCopy = isWorkingCopy();
    Buffer buffer = isWorkingCopy ? owner.createBuffer(this) : BufferManager.createBuffer(this);
    if (buffer == null) {
      return null;
    }
    LibraryConfigurationFileImpl original = null;
    boolean mustSetToOriginalContent = false;
    if (isWorkingCopy) {
      // ensure that isOpen() is called outside the bufManager synchronized
      // block see https://bugs.eclipse.org/bugs/show_bug.cgi?id=237772
      mustSetToOriginalContent = !isPrimary()
          && (original = new LibraryConfigurationFileImpl((DartLibraryImpl) getParent(), getFile(),
              DefaultWorkingCopyOwner.getInstance())).isOpen();
    }
    // synchronize to ensure that 2 threads are not putting 2 different buffers
    // at the same time see https://bugs.eclipse.org/bugs/show_bug.cgi?id=146331
    synchronized (bufManager) {
      Buffer existingBuffer = bufManager.getBuffer(this);
      if (existingBuffer != null) {
        return existingBuffer;
      }
      // set the buffer source
      if (buffer.getCharacters() == null) {
        if (mustSetToOriginalContent) {
          buffer.setContents(original.getSource());
        } else {
          readBuffer(buffer, isWorkingCopy);
        }
      }

      // add buffer to buffer cache
      // note this may cause existing buffers to be removed from the buffer
      // cache, but only primary compilation unit's buffer
      // can be closed, thus no call to a client's IBuffer#close() can be done
      // in this synchronized block.
      bufManager.addBuffer(buffer);

      // listen to buffer changes
      buffer.addBufferChangedListener(this);
    }
    return buffer;
  }

  @Override
  protected IStatus validateExistence(IResource underlyingResource) {
    return DartConventions.validateCompilationUnitName(getElementName());
  }
}
