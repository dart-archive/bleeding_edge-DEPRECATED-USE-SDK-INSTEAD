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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.buffer.BufferChangedEvent;
import com.google.dart.tools.core.buffer.BufferChangedListener;
import com.google.dart.tools.core.completion.CompletionRequestor;
import com.google.dart.tools.core.internal.buffer.BufferManager;
import com.google.dart.tools.core.internal.buffer.NullBuffer;
import com.google.dart.tools.core.internal.completion.CompletionEngine;
import com.google.dart.tools.core.internal.completion.CompletionEnvironment;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.util.Util;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.OpenableElement;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The abstract class <code>OpenableElementImpl</code> implements behavior common to Dart elements
 * that are openable.
 */
public abstract class OpenableElementImpl extends DartElementImpl implements OpenableElement,
    BufferChangedListener {
  protected OpenableElementImpl(DartElementImpl parent) {
    super(parent);
  }

  /**
   * The buffer associated with this element has changed. Registers this element as being out of
   * synch with its buffer's contents. If the buffer has been closed, this element is set as NOT out
   * of synch with the contents.
   * 
   * @see IBufferChangedListener
   */
  @Override
  public void bufferChanged(BufferChangedEvent event) {
    if (event.getBuffer().isClosed()) {
      DartModelManager.getInstance().getElementsOutOfSynchWithBuffers().remove(this);
      getBufferManager().removeBuffer(event.getBuffer());
    } else {
      DartModelManager.getInstance().getElementsOutOfSynchWithBuffers().add(this);
    }
  }

  /**
   * Return <code>true</code> if this element can be removed from the Dart model cache to make
   * space.
   */
  @Override
  public boolean canBeRemovedFromCache() {
    try {
      return !hasUnsavedChanges();
    } catch (DartModelException e) {
      return false;
    }
  }

  /**
   * Return <code>true</code> if the buffer of this element can be removed from the Dart model cache
   * to make space.
   */
  public boolean canBufferBeRemovedFromCache(Buffer buffer) {
    return !buffer.hasUnsavedChanges();
  }

  @Override
  public boolean exists() {
    if (DartModelManager.getInstance().getInfo(this) != null) {
      return true;
    }
    DartCore.notYetImplemented();
    // switch (getElementType()) {
    // case PACKAGE_FRAGMENT:
    // PackageFragmentRoot root = getPackageFragmentRoot();
    // if (root.isArchive()) {
    // // pkg in a jar -> need to open root to know if this pkg exists
    // JarPackageFragmentRootInfo rootInfo;
    // try {
    // rootInfo = (JarPackageFragmentRootInfo) root.getElementInfo();
    // } catch (DartModelException e) {
    // return false;
    // }
    // return rootInfo.rawPackageInfo.containsKey(((PackageFragment)
    // this).names);
    // }
    // break;
    // case CLASS_FILE:
    // if (getPackageFragmentRoot().isArchive()) {
    // // class file in a jar -> need to open this class file to know if it
    // // exists
    // return super.exists();
    // }
    // break;
    // }
    return validateExistence(resource()).isOK();
  }

  @Override
  public String findRecommendedLineSeparator() throws DartModelException {
    Buffer buffer = getBuffer();
    String source = buffer == null ? null : buffer.getContents();
    return Util.getLineSeparator(source, getDartProject());
  }

  /**
   * Note: a buffer with no unsaved changes can be closed by the Dart Model since it has a finite
   * number of buffers allowed open at one time. If this is the first time a request is being made
   * for the buffer, an attempt is made to create and fill this element's buffer. If the buffer has
   * been closed since it was first opened, the buffer is re-created.
   * 
   * @see OpenableElement
   */
  @Override
  public Buffer getBuffer() throws DartModelException {
    if (hasBuffer()) {
      // ensure element is open
      Buffer buffer = getBufferManager().getBuffer(this);
      if (buffer == null) {
        // computing info in this branch allows save to finish even when the
        // source cannot be parsed due to syntax errors, but it may cause
        // other complications
        DartElementInfo info = getElementInfo();
        // try to (re)open a buffer
        buffer = openBuffer(null, info);
      }
      if (buffer instanceof NullBuffer) {
        return null;
      }
      return buffer;
    } else {
      return null;
    }
  }

  /**
   * Return the underlying resource for this element. Elements that may not have a corresponding
   * resource must override this method.
   */
  @Override
  public IResource getCorrespondingResource() throws DartModelException {
    return getUnderlyingResource();
  }

  @Override
  public OpenableElement getOpenable() {
    return this;
  }

  // /**
  // * Find the enclosing package fragment root if any.
  // */
  // public PackageFragmentRoot getPackageFragmentRoot() {
  // return (PackageFragmentRoot)
  // getAncestor(DartElement.PACKAGE_FRAGMENT_ROOT);
  // }

  @Override
  public IResource getResource() {
    IResource resource = resource();
    if (resource == null) {
      DartElement parent = getParent();
      if (parent != null) {
        return parent.getResource();
      }
    }
    return resource;
  }

  @Override
  public IResource getUnderlyingResource() throws DartModelException {
    IResource parentResource = getParent().getUnderlyingResource();
    if (parentResource == null) {
      return null;
    }
    int type = parentResource.getType();
    if (type == IResource.FOLDER || type == IResource.PROJECT) {
      IContainer folder = (IContainer) parentResource;
      IResource resource = folder.findMember(getElementName());
      if (resource == null) {
        throw newNotPresentException();
      } else {
        return resource;
      }
    } else {
      return parentResource;
    }
  }

  @Override
  public boolean hasUnsavedChanges() throws DartModelException {
    if (isReadOnly() || !isOpen()) {
      return false;
    }
    Buffer buf = getBuffer();
    if (buf != null && buf.hasUnsavedChanges()) {
      return true;
    }
    // for package fragments, package fragment roots, and projects must check
    // open buffers
    // to see if they have an child with unsaved changes
    int elementType = getElementType();
    if (/*
         * elementType == PACKAGE_FRAGMENT || elementType == PACKAGE_FRAGMENT_ROOT
         */
    elementType == LIBRARY || elementType == DART_PROJECT || elementType == DART_MODEL) {
      // fix for 1FWNMHH
      Iterator<Buffer> openBuffers = getBufferManager().getOpenBuffers();
      while (openBuffers.hasNext()) {
        Buffer buffer = openBuffers.next();
        if (buffer.hasUnsavedChanges()) {
          DartElement owner = buffer.getOwner();
          if (isAncestorOf(owner)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Subclasses must override as required.
   * 
   * @see OpenableElement
   */
  @Override
  public boolean isConsistent() {
    return true;
  }

  @Override
  public boolean isOpen() {
    return DartModelManager.getInstance().getInfo(this) != null;
  }

  @Override
  public boolean isStructureKnown() throws DartModelException {
    return ((OpenableElementInfo) getElementInfo()).isStructureKnown();
  }

  @Override
  public void makeConsistent(IProgressMonitor monitor) throws DartModelException {
    // only compilation units can be inconsistent
    // other openables cannot be inconsistent so default is to do nothing
  }

  @Override
  public void open(IProgressMonitor pm) throws DartModelException {
    getElementInfo(pm);
  }

  @Override
  public IResource resource() {
    // PackageFragmentRoot root = getPackageFragmentRoot();
    // if (root != null && root.isArchive())
    // return root.resource(root);
    // return resource(root);
    DartCore.notYetImplemented();
    return null;
  }

  @Override
  public void save(IProgressMonitor pm, boolean force) throws DartModelException {
    if (isReadOnly()) {
      throw new DartModelException(
          new DartModelStatusImpl(DartModelStatusConstants.READ_ONLY, this));
    }
    Buffer buf = getBuffer();
    if (buf != null) {
      // some OpenableElements (like a DartProject) don't have a buffer
      buf.save(pm, force);
      // update the element info of this element
      makeConsistent(pm);
    }
  }

  /**
   * Builds this element's structure and properties in the given info object, based on this
   * element's current contents (reuse buffer contents if this element has an open buffer, or
   * resource contents if this element does not have an open buffer). Children are placed in the
   * given newElements table (note, this element has already been placed in the newElements table).
   * 
   * @return <code>true</code> if successful, or <code>false</code> if an error is encountered while
   *         determining the structure of this element
   */
  protected abstract boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm,
      Map<DartElement, DartElementInfo> newElements, IResource underlyingResource)
      throws DartModelException;

  /**
   * Close the buffer associated with this element, if any.
   */
  protected void closeBuffer() {
    if (!hasBuffer()) {
      return; // nothing to do
    }
    Buffer buffer = getBufferManager().getBuffer(this);
    if (buffer != null) {
      buffer.close();
      buffer.removeBufferChangedListener(this);
    }
  }

  /**
   * This element is being closed. Do any necessary cleanup.
   * 
   * @param info the info for this element
   */
  @Override
  protected void closing(DartElementInfo info) throws DartModelException {
    closeBuffer();
  }

  protected void codeComplete(CompilationUnit cu, CompilationUnit unitToSkip, int position,
      CompletionRequestor requestor, WorkingCopyOwner owner, IProgressMonitor monitor)
      throws DartModelException {
    if (requestor == null) {
      throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
    }
//    PerformanceStats performanceStats = CompletionEngine.PERF
//        ? PerformanceStats.getStats(DartModelManager.COMPLETION_PERF, this)
//        : null;
//    if (performanceStats != null) {
//      performanceStats.startRun(new String(cu.getFileName())
//          + " at " + position); //$NON-NLS-1$
//    }
    Buffer buffer = getBuffer();
    if (buffer == null) {
      return;
    }
    if (position < -1 || position > buffer.getLength()) {
      throw new DartModelException(new DartModelStatusImpl(
          DartModelStatusConstants.INDEX_OUT_OF_BOUNDS));
    }
    DartProjectImpl project = (DartProjectImpl) getDartProject();
    SearchScope scope = SearchScopeFactory.createWorkspaceScope();
    CompletionEnvironment environment = new CompletionEnvironment(project, owner, unitToSkip);
//    SearchableEnvironment environment = project.newSearchableNameEnvironment(owner);
    // set unit to skip
//    environment.unitToSkip = unitToSkip;

    // code complete
    CompletionEngine engine = new CompletionEngine(
        environment,
        requestor,
        project.getOptions(true),
        project,
        owner,
        monitor);
    engine.complete(cu, position, 0);
//    if (performanceStats != null) {
//      performanceStats.endRun();
//    }
//    if (NameLookup.VERBOSE) {
//      System.out.println(Thread.currentThread()
//          + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
//      System.out.println(Thread.currentThread()
//          + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
//    }
  }

//  protected DartElement[] codeSelect(org.eclipse.jdt.internal.compiler.env.ICompilationUnit cu, int offset, int length, WorkingCopyOwner owner) throws DartModelException {
////    PerformanceStats performanceStats = SelectionEngine.PERF ? PerformanceStats.getStats(DartModelManager.SELECTION_PERF, this) : null;
////    if (performanceStats != null) {
////      performanceStats.startRun(new String(cu.getFileName()) + " at [" + offset + "," + length + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
////    }
//
//    DartProjectImpl project = (DartProjectImpl) getDartProject();
//    SearchableEnvironment environment = project.newSearchableNameEnvironment(owner);
//
//    SelectionRequestor requestor = new SelectionRequestor(environment.nameLookup, this);
//    Buffer buffer = getBuffer();
//    if (buffer == null) {
//      return requestor.getElements();
//    }
//    int end = buffer.getLength();
//    if (offset < 0 || length < 0 || offset + length > end) {
//      throw new DartModelException(new DartModelStatusImpl(DartModelStatusConstants.INDEX_OUT_OF_BOUNDS));
//    }
//
//    SelectionEngine engine = new SelectionEngine(environment, requestor, project.getOptions(true), owner);
//    engine.select(cu, offset, offset + length - 1);
//
////    if (performanceStats != null) {
////      performanceStats.endRun();
////    }
////    if (NameLookup.VERBOSE) {
////      System.out.println(Thread.currentThread()
////            + " TIME SPENT in NameLoopkup#seekTypesInSourcePackage: " + environment.nameLookup.timeSpentInSeekTypesInSourcePackage + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
////      System.out.println(Thread.currentThread()
////            + " TIME SPENT in NameLoopkup#seekTypesInBinaryPackage: " + environment.nameLookup.timeSpentInSeekTypesInBinaryPackage + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
////    }
//    return requestor.getElements();
//  }

  @Override
  protected DartElementInfo createElementInfo() {
    return new OpenableElementInfo();
  }

  @Override
  protected void generateInfos(DartElementInfo info,
      HashMap<DartElement, DartElementInfo> newElements, IProgressMonitor monitor)
      throws DartModelException {
    if (DartModelCache.VERBOSE) {
      String element;
      switch (getElementType()) {
        case DART_PROJECT:
          element = "project"; //$NON-NLS-1$
          break;
        case LIBRARY:
          element = "library"; //$NON-NLS-1$
          break;
        // case PACKAGE_FRAGMENT_ROOT:
        //          element = "root"; //$NON-NLS-1$
        // break;
        // case PACKAGE_FRAGMENT:
        //          element = "package"; //$NON-NLS-1$
        // break;
        // case CLASS_FILE:
        //          element = "class file"; //$NON-NLS-1$
        // break;
        case COMPILATION_UNIT:
          element = "compilation unit"; //$NON-NLS-1$
          break;
        default:
          element = "element"; //$NON-NLS-1$
      }
      System.out.println(Thread.currentThread()
          + " OPENING " + element + " " + toStringWithAncestors()); //$NON-NLS-1$//$NON-NLS-2$
    }

    // open its ancestors if needed
    openAncestors(newElements, monitor);

    // validate existence
    IResource underlResource = resource();
    IStatus status = validateExistence(underlResource);
    if (!status.isOK()) {
      throw newDartModelException(status);
    }
    if (monitor != null && monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
    // puts the info before building the structure so that questions to the
    // handle behave as if the element existed
    // (case of compilation units becoming working copies)
    newElements.put(this, info);

    // build the structure of the openable (this will open the buffer if needed)
    try {
      OpenableElementInfo openableElementInfo = (OpenableElementInfo) info;
      boolean isStructureKnown = buildStructure(
          openableElementInfo,
          monitor,
          newElements,
          underlResource);
      openableElementInfo.setIsStructureKnown(isStructureKnown);
    } catch (DartModelException e) {
      newElements.remove(this);
      throw e;
    }

    // remove out of sync buffer for this element
    DartModelManager.getInstance().getElementsOutOfSynchWithBuffers().remove(this);

    // if (DartModelCache.VERBOSE) {
    //      System.out.println(DartModelManager.getInstance().cacheToString("-> ")); //$NON-NLS-1$
    // }
  }

  // /**
  // * Return the buffer factory to use for creating new buffers
  // *
  // * @deprecated
  // */
  // public BufferFactory getBufferFactory(){
  // return getBufferManager().getDefaultBufferFactory();
  // }

  /**
   * Return the buffer manager for this element.
   */
  protected BufferManager getBufferManager() {
    return BufferManager.getInstance();
  }

  /**
   * Return <code>true</code> if this element may have an associated source buffer, otherwise false.
   * Subclasses must override as required.
   */
  protected boolean hasBuffer() {
    return false;
  }

  /**
   * Return <code>true</code> if this represents a source element. Openable source elements have an
   * associated buffer created when they are opened.
   */
  protected boolean isSourceElement() {
    return false;
  }

  /**
   * Open the ancestors of this openable that are not yet opened, validating their existence.
   */
  protected void openAncestors(HashMap<DartElement, DartElementInfo> newElements,
      IProgressMonitor monitor) throws DartModelException {
    OpenableElementImpl openableParent = (OpenableElementImpl) getOpenableParent();
    if (openableParent != null && !openableParent.isOpen()) {
      openableParent.generateInfos(openableParent.createElementInfo(), newElements, monitor);
    }
  }

  /**
   * Opens a buffer on the contents of this element, and returns the buffer, or returns
   * <code>null</code> if opening fails. By default, do nothing - subclasses that have buffers must
   * override as required.
   * 
   * @param pm
   * @param info
   * @return the buffer that was opened
   * @throws DartModelException if the buffer could not be opened
   */
  protected Buffer openBuffer(IProgressMonitor pm, DartElementInfo info) throws DartModelException {
    return null;
  }

  // protected abstract IResource resource(PackageFragmentRoot root);

  /**
   * Return <code>true</code> if the corresponding resource or associated file exists
   */
  protected boolean resourceExists(IResource underlyingResource) {
    return underlyingResource.isAccessible();
  }

  /**
   * Validate the existence of this openable. Returns a non ok status if it doesn't exist.
   */
  protected abstract IStatus validateExistence(IResource underlyingResource);
}
