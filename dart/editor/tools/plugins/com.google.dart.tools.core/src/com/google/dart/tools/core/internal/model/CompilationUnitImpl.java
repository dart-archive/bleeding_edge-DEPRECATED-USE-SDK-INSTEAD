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
package com.google.dart.tools.core.internal.model;

import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.completion.CompletionRequestor;
import com.google.dart.tools.core.internal.buffer.BufferManager;
import com.google.dart.tools.core.internal.model.info.CompilationUnitInfo;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.problem.CategorizedProblem;
import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.internal.workingcopy.DefaultWorkingCopyOwner;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.problem.ProblemRequestor;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instances of the class <code>CompilationUnitImpl</code> implement the representation of files
 * containing Dart source code that needs to be compiled.
 * 
 * @coverage dart.tools.core.model
 */
public class CompilationUnitImpl extends SourceFileElementImpl<CompilationUnit> implements
    CompilationUnit {
  /**
   * An empty array of compilation units.
   */
  public static final CompilationUnitImpl[] EMPTY_ARRAY = new CompilationUnitImpl[0];

  /**
   * Return the file associated with the given URI.
   * 
   * @param uri the uri of the file represented by the compilation unit
   * @return the file associated with the given URI
   */
  private static IFile getFile(URI uri) {
    IFile file = ResourceUtil.getFile(uri);
    if (file == null) {
      throw new IllegalArgumentException("The URI \"" + uri
          + "\" does not map to an existing resource.");
    }
    return file;
  }

  /**
   * Initialize a newly created compilation unit to be an element in the given container.
   * 
   * @param container the library or library folder containing the compilation unit
   * @param file the file represented by the compilation unit
   * @param owner the working copy owner
   */
  public CompilationUnitImpl(CompilationUnitContainer container, IFile file, WorkingCopyOwner owner) {
    super(container, file, owner);
  }

  /**
   * Initialize a newly created compilation unit to be an element in the given container.
   * 
   * @param container the library or library folder containing the compilation unit
   * @param uri the uri of the file represented by the compilation unit
   * @param owner the working copy owner
   */
  public CompilationUnitImpl(CompilationUnitContainer container, URI uri, WorkingCopyOwner owner) {
    super(container, getFile(uri), owner);
  }

  @Override
  public void becomeWorkingCopy(ProblemRequestor problemRequestor, IProgressMonitor monitor)
      throws DartModelException {

  }

  @Override
  public boolean canBeRemovedFromCache() {

    return super.canBeRemovedFromCache();
  }

  @Override
  public boolean canBufferBeRemovedFromCache(Buffer buffer) {

    return super.canBufferBeRemovedFromCache(buffer);
  }

  /**
   * Clone this handle so that it caches its contents in memory.
   * <p>
   * DO NOT PASS TO CLIENTS
   */
  public CompilationUnitImpl cloneCachingContents() {
    return new CompilationUnitImpl((DartLibraryImpl) getParent(), getFile(), this.owner) {
      private char[] cachedContents;

      @Override
      public char[] getContents() {
        if (this.cachedContents == null) {
          this.cachedContents = CompilationUnitImpl.this.getContents();
        }
        return this.cachedContents;
      }

//      public CompilationUnit originalFromClone() {
//        return CompilationUnitImpl.this;
//      }
    };
  }

  @Override
  public void close() throws DartModelException {

    super.close();
  }

  @Override
  public void codeComplete(int offset, CompletionRequestor requestor) throws DartModelException {
    codeComplete(offset, requestor, DefaultWorkingCopyOwner.getInstance());
  }

  @Override
  public void codeComplete(int offset, CompletionRequestor requestor, IProgressMonitor monitor)
      throws DartModelException {
    codeComplete(offset, requestor, DefaultWorkingCopyOwner.getInstance(), monitor);
  }

  @Override
  public void codeComplete(int offset, CompletionRequestor requestor,
      WorkingCopyOwner workingCopyOwner) throws DartModelException {
    codeComplete(offset, requestor, workingCopyOwner, null);
  }

  @Override
  public void codeComplete(int offset, CompletionRequestor requestor,
      WorkingCopyOwner workingCopyOwner, IProgressMonitor monitor) throws DartModelException {
    CompilationUnit orig = isWorkingCopy() ? (CompilationUnit) getOriginalElement() : this;
    codeComplete(this, orig, offset, requestor, workingCopyOwner, monitor);
  }

  @Override
  public DartElement[] codeSelect(DartUnit ast, int offset, int length,
      WorkingCopyOwner workingCopyOwner) throws DartModelException {
    //TODO (pquitslund): remove
    return null;
  }

  @Override
  public DartElement[] codeSelect(int offset, int length) throws DartModelException {
    return codeSelect(offset, length, DefaultWorkingCopyOwner.getInstance());
  }

  @Override
  public DartElement[] codeSelect(int offset, int length, WorkingCopyOwner workingCopyOwner)
      throws DartModelException {
    return codeSelect(null, offset, length, workingCopyOwner);
  }

  @Override
  public void commitWorkingCopy(boolean force, IProgressMonitor monitor) throws DartModelException {

  }

  @Override
  public void copy(DartElement container, DartElement sibling, String rename, boolean force,
      IProgressMonitor monitor) throws DartModelException {
    if (container == null) {
      throw new IllegalArgumentException(Messages.operation_nullContainer);
    }
    DartElement[] elements = new DartElement[] {this};
    DartElement[] containers = new DartElement[] {container};
    String[] renamings = null;
    if (rename != null) {
      renamings = new String[] {rename};
    }
    getDartModel().copy(elements, containers, null, renamings, force, monitor);
  }

  @Override
  public DartElementInfo createElementInfo() {
    return new CompilationUnitInfo();
  }

  @Override
  public Type createType(String content, DartElement sibling, boolean force,
      IProgressMonitor monitor) throws DartModelException {
    DartCore.notYetImplemented();
    return null;
    // if (!exists()) {
    // // autogenerate this compilation unit
    // IPackageFragment pkg = (IPackageFragment) getParent();
    //      String source = ""; //$NON-NLS-1$
    // if (!pkg.isDefaultPackage()) {
    // // not the default package...add the package declaration
    // String lineSeparator = Util.getLineSeparator(
    // null/* no existing source */, getDartProject());
    //        source = "package " + pkg.getElementName() + ";" + lineSeparator + lineSeparator; //$NON-NLS-1$ //$NON-NLS-2$
    // }
    // CreateCompilationUnitOperation op = new CreateCompilationUnitOperation(
    // pkg, name, source, force);
    // op.runOperation(monitor);
    // }
    // CreateTypeOperation op = new CreateTypeOperation(this, content, force);
    // if (sibling != null) {
    // op.createBefore(sibling);
    // }
    // op.runOperation(monitor);
    // return (Type) op.getResultElements()[0];
  }

  @Override
  public boolean definesLibrary() {
    try {
      return ((CompilationUnitInfo) getElementInfo()).getDefinesLibrary();
    } catch (DartModelException exception) {
      return false;
    }
  }

  @Override
  public void delete(boolean force, IProgressMonitor monitor) throws DartModelException {
    DartElement[] elements = new DartElement[] {this};
    getDartModel().delete(elements, force, monitor);
  }

  @Override
  public void discardWorkingCopy() throws DartModelException {

  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof CompilationUnitImpl)) {
      return false;
    }

    return super.equals(obj) && getPath().equals(((CompilationUnitImpl) obj).getPath());

  }

  /**
   * Finds the elements in this Dart file that correspond to the given element. An element A
   * corresponds to an element B if:
   * <ul>
   * <li>A has the same element name as B.
   * <li>If A is a method, A must have the same number of arguments as B and the simple names of the
   * argument types must be equal, if known.
   * <li>The parent of A corresponds to the parent of B recursively up to their respective Dart
   * files.
   * <li>A exists.
   * </ul>
   * Returns <code>null</code> if no such Dart elements can be found or if the given element is not
   * included in a Dart file.
   * 
   * @param element the given element
   * @return the found elements in this Dart file that correspond to the given element
   */
  @Override
  public DartElement[] findElements(DartElement element) {
    List<DartElement> children = Lists.newArrayList();
    while (element != null && element.getElementType() != DartElement.COMPILATION_UNIT) {
      children.add(element);
      element = element.getParent();
    }
    if (element == null) {
      return null;
    }
    DartElement currentElement = this;
    for (int i = children.size() - 1; i >= 0; i--) {
      DartCore.notYetImplemented();
      // SourceRefElement child = (SourceRefElement) children.get(i);
      // switch (child.getElementType()) {
      // case DartElement.PACKAGE_DECLARATION:
      // currentElement = ((CompilationUnit)
      // currentElement).getPackageDeclaration(child.getElementName());
      // break;
      // case DartElement.IMPORT_CONTAINER:
      // currentElement = ((CompilationUnit)
      // currentElement).getImportContainer();
      // break;
      // case DartElement.IMPORT_DECLARATION:
      // currentElement = ((ImportContainer)
      // currentElement).getImport(child.getElementName());
      // break;
      // case DartElement.TYPE:
      // switch (currentElement.getElementType()) {
      // case DartElement.COMPILATION_UNIT:
      // currentElement = ((CompilationUnit)
      // currentElement).getType(child.getElementName());
      // break;
      // case DartElement.TYPE:
      // currentElement = ((Type)
      // currentElement).getType(child.getElementName());
      // break;
      // case DartElement.FIELD:
      // case DartElement.INITIALIZER:
      // case DartElement.METHOD:
      // currentElement = ((Member) currentElement).getType(
      // child.getElementName(), child.occurrenceCount);
      // break;
      // }
      // break;
      // case DartElement.INITIALIZER:
      // currentElement = ((Type)
      // currentElement).getInitializer(child.occurrenceCount);
      // break;
      // case DartElement.FIELD:
      // currentElement = ((Type)
      // currentElement).getField(child.getElementName());
      // break;
      // case DartElement.METHOD:
      // currentElement = ((Type) currentElement).getMethod(
      // child.getElementName(), ((Method) child).getParameterTypes());
      // break;
      // }
    }
    if (currentElement != null && currentElement.exists()) {
      return new DartElement[] {currentElement};
    } else {
      return null;
    }
  }

  @Override
  public CompilationUnit findWorkingCopy(WorkingCopyOwner workingCopyOwner) {
    CompilationUnitImpl cu = new CompilationUnitImpl(
        (DartLibraryImpl) getParent(),
        getFile(),
        workingCopyOwner);
    return cu;
  }

  @Override
  public com.google.dart.tools.core.model.DartClassTypeAlias[] getClassTypeAliases()
      throws DartModelException {
    List<com.google.dart.tools.core.model.DartClassTypeAlias> typeList = getChildrenOfType(com.google.dart.tools.core.model.DartClassTypeAlias.class);
    return typeList.toArray(new com.google.dart.tools.core.model.DartClassTypeAlias[typeList.size()]);
  }

  @Override
  public CompilationUnit getCompilationUnit() {
    return this;
  }

  public char[] getContents() {
    Buffer buffer = getBufferManager().getBuffer(this);
    if (buffer == null) {
      // no need to force opening of CU to get the content
      // also this cannot be a working copy, as its buffer is never closed while
      // the working copy is alive
      IFile file = (IFile) getResource();
      // Get encoding from file
      String encoding;
      try {
        encoding = file.getCharset();
      } catch (CoreException ce) {
        // do not use any encoding
        encoding = null;
      }
      try {
        return Util.getResourceContentsAsCharArray(file, encoding);
      } catch (DartModelException e) {
        // if (DartModelManager.getInstance().abortOnMissingSource.get() ==
        // Boolean.TRUE) {
        // IOException ioException = e.getDartModelStatus().getCode() ==
        // DartModelStatusConstants.IO_EXCEPTION
        // ? (IOException) e.getException()
        // : new IOException(e.getMessage());
        // throw new AbortCompilationUnit(null, ioException, encoding);
        // } else {
        // Util.log(e, Messages.bind(Messages.file_notFound,
        // file.getFullPath().toString()));
        // }
        DartCore.notYetImplemented();
        return CharOperation.NO_CHAR;
      }
    }
    char[] contents = buffer.getCharacters();
    if (contents == null) {
      // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=129814
      // if (DartModelManager.getInstance().abortOnMissingSource.get() ==
      // Boolean.TRUE) {
      // IOException ioException = new IOException(Messages.buffer_closed);
      // IFile file = (IFile) getResource();
      // // Get encoding from file
      // String encoding;
      // try {
      // encoding = file.getCharset();
      // } catch (CoreException ce) {
      // // do not use any encoding
      // encoding = null;
      // }
      // throw new AbortCompilationUnit(null, ioException, encoding);
      // }
      DartCore.notYetImplemented();
      return CharOperation.NO_CHAR;
    }
    return contents;
  }

  @Override
  public DartElement getElementAt(int position) throws DartModelException {
    DartElement element = getSourceElementAt(position);
    if (element == this) {
      return null;
    } else {
      return element;
    }
  }

  @Override
  public int getElementType() {
    return DartElement.COMPILATION_UNIT;
  }

  public char[] getFileName() {
    return getPath().toString().toCharArray();
  }

  @Override
  public DartFunctionTypeAlias[] getFunctionTypeAliases() throws DartModelException {
    List<DartFunctionTypeAlias> typeList = getChildrenOfType(DartFunctionTypeAlias.class);
    return typeList.toArray(new DartFunctionTypeAlias[typeList.size()]);
  }

  @Override
  public DartVariableDeclaration[] getGlobalVariables() throws DartModelException {
    List<DartVariableDeclaration> variableList = getChildrenOfType(DartVariableDeclaration.class);
    return variableList.toArray(new DartVariableDeclaration[variableList.size()]);
  }

  @Override
  public DartLibrary getLibrary() {
    return getAncestor(DartLibrary.class);
  }

  @Override
  public DartElement getPrimaryElement(boolean checkOwner) {
    if (checkOwner && isPrimary()) {
      return this;
    }
    return new CompilationUnitImpl(
        (DartLibraryImpl) getParent(),
        getFile(),
        DefaultWorkingCopyOwner.getInstance());
  }

  @Override
  public Type getType(String typeName) {
    return new DartTypeImpl(this, typeName);
  }

  @Override
  public Type[] getTypes() throws DartModelException {
    List<Type> typeList = getChildrenOfType(Type.class);
    return typeList.toArray(new Type[typeList.size()]);
  }

  @Override
  public IResource getUnderlyingResource() throws DartModelException {
    if (isWorkingCopy() && !isPrimary()) {
      return null;
    }
    return super.getUnderlyingResource();
  }

  @Override
  public int hashCode() {
    if (getPath() != null) {
      return Util.combineHashCodes(getPath().hashCode(), super.hashCode());
    }
    return super.hashCode();
  }

  @Override
  public boolean hasMain() throws DartModelException {
    List<com.google.dart.tools.core.model.DartFunction> functions = getChildrenOfType(com.google.dart.tools.core.model.DartFunction.class);

    for (com.google.dart.tools.core.model.DartFunction function : functions) {
      if (function.isMain()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean hasResourceChanged() {

    return false;
  }

  /**
   * Return <code>true</code> if the element is consistent with its underlying resource or buffer.
   * The element is consistent when opened, and is consistent if the underlying resource or buffer
   * has not been modified since it was last consistent.
   * <p>
   * NOTE: Child consistency is not considered. For example, a package fragment responds
   * <code>true</code> when it knows about all of its compilation units present in its underlying
   * folder. However, one or more of the compilation units could be inconsistent.
   * 
   * @return <code>true</code> if the element is consistent with its underlying resource or buffer
   */
  @Override
  public boolean isConsistent() {
    return false;
  }

  public DartUnit makeConsistent(boolean resolveBindings, boolean forceProblemDetection,
      Map<String, CategorizedProblem[]> problems, IProgressMonitor monitor)
      throws DartModelException {

    return null;

  }

  @Override
  public void makeConsistent(IProgressMonitor monitor) throws DartModelException {
    makeConsistent(false, false, null, monitor);
  }

  @Override
  public void move(DartElement container, DartElement sibling, String rename, boolean force,
      IProgressMonitor monitor) throws DartModelException {
    if (container == null) {
      throw new IllegalArgumentException(Messages.operation_nullContainer);
    }
    DartElement[] elements = new DartElement[] {this};
    DartElement[] containers = new DartElement[] {container};

    String[] renamings = null;
    if (rename != null) {
      renamings = new String[] {rename};
    }
    getDartModel().move(elements, containers, null, renamings, force, monitor);
  }

  public void reconcile(boolean forceProblemDetection, IProgressMonitor monitor)
      throws DartModelException {
    reconcile(forceProblemDetection, null, monitor);
  }

  public DartUnit reconcile(boolean forceProblemDetection, WorkingCopyOwner workingCopyOwner,
      IProgressMonitor monitor) throws DartModelException {

    return null;

  }

  @Override
  public void rename(String newName, boolean force, IProgressMonitor monitor)
      throws DartModelException {
    if (newName == null) {
      throw new IllegalArgumentException(Messages.operation_nullName);
    }
    DartElement[] elements = new DartElement[] {this};
    DartElement[] dests = new DartElement[] {getParent()};
    String[] renamings = new String[] {newName};
    getDartModel().rename(elements, dests, renamings, force, monitor);
  }

  @Override
  public void restore() throws DartModelException {
    if (!isWorkingCopy()) {
      return;
    }
    CompilationUnitImpl original = (CompilationUnitImpl) getOriginalElement();
    Buffer buffer = getBuffer();
    if (buffer == null) {
      return;
    }
    buffer.setContents(original.getContents());
    updateTimeStamp(original);
    makeConsistent(null);
  }

  @Override
  public void save(IProgressMonitor monitor, boolean force) throws DartModelException {
    if (isWorkingCopy()) {
      // No need to save the buffer for a working copy (this is a noop). Not
      // simply makeConsistent, also computes fine-grain deltas in case the
      // working copy is being reconciled already (if not it would miss one
      // iteration of deltas).
      reconcile(false, null, null);
    } else {
      super.save(monitor, force);
    }
  }

  public void updateTimeStamp(CompilationUnitImpl original) throws DartModelException {
    if (original == null) {
      return;
    }
    IFile originalResource = (IFile) original.getResource();
    if (originalResource == null) {
      return;
    }
    long timeStamp = originalResource.getModificationStamp();
    if (timeStamp == IResource.NULL_STAMP) {
      throw new DartModelException(new DartModelStatusImpl(
          DartModelStatusConstants.INVALID_RESOURCE));
    }
    ((CompilationUnitInfo) getElementInfo()).setTimestamp(timeStamp);
  }

  @Override
  protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm,
      Map<DartElement, DartElementInfo> newElements, IResource underlyingResource)
      throws DartModelException {
    CompilationUnitInfo unitInfo = (CompilationUnitInfo) info;
    //
    // Ensure that the buffer is opened so that it can be accessed indirectly
    // later.
    //
    Buffer buffer = getBufferManager().getBuffer(this);
    if (buffer == null) {
      // Open the buffer independently from the info, since we are building the
      // info
      openBuffer(pm, unitInfo);
    }
    return false;
  }

  @Override
  protected void closing(DartElementInfo info) throws DartModelException {

    // else the buffer of a working copy must remain open for the lifetime of
    // the working copy
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {

    return null;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return MEMENTO_DELIMITER_COMPILATION_UNIT;
  }

  @Override
  protected String getHandleMementoName() {
    // Because the compilation unit can be anywhere relative to the library or
    // application that contains it we need to specify the full path.
    return getFile().getProjectRelativePath().toPortableString();
  }

  @Override
  protected CompilationUnit getWorkingCopy(WorkingCopyOwner workingCopyOwner,
      ProblemRequestor problemRequestor, IProgressMonitor monitor) throws DartModelException {

    return this;

  }

  @Override
  protected boolean hasBuffer() {
    return true;
  }

  @Override
  protected boolean isSourceElement() {
    return true;
  }

  @Override
  protected void openAncestors(HashMap<DartElement, DartElementInfo> newElements,
      IProgressMonitor monitor) throws DartModelException {
    if (!isWorkingCopy()) {
      super.openAncestors(newElements, monitor);
    }
    // else don't open ancestors for a working copy to speed up the first
    // becomeWorkingCopy
    // (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=89411)
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
    CompilationUnitImpl original = null;
    boolean mustSetToOriginalContent = false;
    if (isWorkingCopy) {
      // ensure that isOpen() is called outside the bufManager synchronized
      // block see https://bugs.eclipse.org/bugs/show_bug.cgi?id=237772
      mustSetToOriginalContent = !isPrimary()
          && (original = new CompilationUnitImpl(
              (DartLibraryImpl) getParent(),
              getFile(),
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

  /**
   * Debugging purposes
   */
  @Override
  protected void toStringInfo(int tab, StringBuilder builder, DartElementInfo info,
      boolean showResolvedInfo) {
    if (!isPrimary()) {
      builder.append(tabString(tab));
      builder.append("[Working copy] "); //$NON-NLS-1$
      toStringName(builder);
    } else {
      if (isWorkingCopy()) {
        builder.append(tabString(tab));
        builder.append("[Working copy] "); //$NON-NLS-1$
        toStringName(builder);
        if (info == null) {
          builder.append(" (not open)"); //$NON-NLS-1$
        }
      } else {
        super.toStringInfo(tab, builder, info, showResolvedInfo);
      }
    }
  }

  protected IStatus validateCompilationUnit(IResource resource) {
    DartCore.notYetImplemented();
    // IPackageFragmentRoot root = getPackageFragmentRoot();
    // // root never null as validation is not done for working copies
    // try {
    // if (root.getKind() != IPackageFragmentRoot.K_SOURCE)
    // return new
    // DartModelStatusImpl(DartModelStatusConstants.INVALID_ELEMENT_TYPES,
    // root);
    // } catch (DartModelException e) {
    // return e.getDartModelStatus();
    // }
    if (resource != null) {
      // char[][] inclusionPatterns =
      // ((PackageFragmentRoot)root).fullInclusionPatternChars();
      // char[][] exclusionPatterns =
      // ((PackageFragmentRoot)root).fullExclusionPatternChars();
      // if (Util.isExcluded(resource, inclusionPatterns, exclusionPatterns))
      // return new
      // DartModelStatusImpl(DartModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH,
      // this);
      if (!resource.isAccessible()) {
        return new DartModelStatusImpl(DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST, this);
      }
    }
    return DartConventions.validateCompilationUnitName(getElementName());
  }

  @Override
  protected IStatus validateExistence(IResource underlyingResource) {
    // check if this compilation unit can be opened
    if (!isWorkingCopy()) { // no check is done on root kind or exclusion
                            // pattern for working copies
      IStatus status = validateCompilationUnit(underlyingResource);
      if (!status.isOK()) {
        return status;
      }
    }
    // prevents reopening of non-primary working copies (they are closed when
    // they are discarded and should not be reopened)
    if (!isPrimary()) {
      return newDoesNotExistStatus();
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  private DartElement getOriginalElement() {

    return getPrimaryElement();
  }
}
