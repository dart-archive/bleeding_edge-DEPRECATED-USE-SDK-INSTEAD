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
package com.google.dart.tools.core.internal.operation;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.internal.buffer.DocumentAdapter;
import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaImpl;
import com.google.dart.tools.core.internal.model.delta.DeltaProcessor;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The class <code>DartModelOperation</code> defines the behavior common to all Dart Model
 * operations
 */
public abstract class DartModelOperation implements IWorkspaceRunnable, IProgressMonitor {
  protected interface IPostAction {
    /*
     * Returns the id of this action.
     * 
     * @see DartModelOperation#postAction
     */
    String getID();

    /*
     * Run this action.
     */
    void run() throws DartModelException;
  }

  /*
   * Constants controlling the insertion mode of an action.
   * 
   * @see DartModelOperation#postAction
   */
  // insert at the end
  protected static final int APPEND = 1;
  // remove all existing ones with same ID, and add new one at the end
  protected static final int REMOVEALL_APPEND = 2;
  // do not insert if already existing with same ID
  protected static final int KEEP_EXISTING = 3;

  /*
   * Whether tracing post actions is enabled.
   */
  protected static boolean POST_ACTION_VERBOSE;

  /**
   * Return the attribute registered at the given key with the top level operation. Returns
   * <code>null</code> if no such attribute is found.
   * 
   * @return the attribute registered at the given key with the top level operation
   */
  protected static Object getAttribute(Object key) {
    ArrayList<DartModelOperation> stack = getCurrentOperationStack();
    if (stack.size() == 0) {
      return null;
    }
    DartModelOperation topLevelOp = stack.get(0);
    if (topLevelOp.attributes == null) {
      return null;
    } else {
      return topLevelOp.attributes.get(key);
    }
  }

  /**
   * Return the stack of operations running in the current thread. Returns an empty stack if no
   * operations are currently running in this thread.
   * 
   * @return the stack of operations running in the current thread
   */
  protected static ArrayList<DartModelOperation> getCurrentOperationStack() {
    ArrayList<DartModelOperation> stack = OPERATION_STACKS.get();
    if (stack == null) {
      stack = new ArrayList<DartModelOperation>();
      OPERATION_STACKS.set(stack);
    }
    return stack;
  }

  /**
   * Register the given attribute at the given key with the top level operation.
   * 
   * @param key the key with which the attribute is to be registered
   * @param attribute the attribute to be registered
   */
  protected static void setAttribute(String key, String attribute) {
    ArrayList<DartModelOperation> operationStack = getCurrentOperationStack();
    if (operationStack.size() == 0) {
      return;
    }
    DartModelOperation topLevelOp = operationStack.get(0);
    if (topLevelOp.attributes == null) {
      topLevelOp.attributes = new HashMap<String, String>();
    }
    topLevelOp.attributes.put(key, attribute);
  }

  /*
   * A list of IPostActions.
   */
  protected IPostAction[] actions;

  protected int actionsStart = 0;
  protected int actionsEnd = -1;
  /*
   * A HashMap of attributes that can be used by operations.
   */
  // This used to be <Object, Object>
  protected HashMap<String, String> attributes;

  public static final String HAS_MODIFIED_RESOURCE_ATTR = "hasModifiedResource"; //$NON-NLS-1$

  public static final String TRUE = "true"; // DartModelManager.TRUE;

  // public static final String FALSE = "false";

  /**
   * The elements this operation operates on, or <code>null</code> if this operation does not
   * operate on specific elements.
   */
  protected DartElement[] elementsToProcess;

  /**
   * The parent elements this operation operates with or <code>null</code> if this operation does
   * not operate with specific parent elements.
   */
  protected DartElement[] parentElements;

  /**
   * An empty collection of <code>DartElement</code>s - the common empty result if no elements are
   * created, or if this operation is not actually executed.
   */
  protected static final DartElement[] NO_ELEMENTS = new DartElement[] {};

  /**
   * The elements created by this operation - empty until the operation actually creates elements.
   */
  protected DartElement[] resultElements = NO_ELEMENTS;

  /**
   * The progress monitor passed into this operation
   */
  public IProgressMonitor progressMonitor = null;

  /**
   * A flag indicating whether this operation is nested.
   */
  protected boolean isNested = false;

  /**
   * Conflict resolution policy - by default do not force (fail on a conflict).
   */
  protected boolean force = false;

  /*
   * A per thread stack of Dart model operations.
   */
  protected static final ThreadLocal<ArrayList<DartModelOperation>> OPERATION_STACKS = new ThreadLocal<ArrayList<DartModelOperation>>();

  protected DartModelOperation() {
    // default constructor used in subclasses
  }

  /**
   * Common constructor for all Dart Model operations.
   */
  protected DartModelOperation(DartElement element) {
    this.elementsToProcess = new DartElement[] {element};
  }

  /**
   * A common constructor for all Dart Model operations.
   */
  protected DartModelOperation(DartElement[] elements) {
    this.elementsToProcess = elements;
  }

  /**
   * A common constructor for all Dart Model operations.
   */
  protected DartModelOperation(DartElement[] elements, boolean force) {
    this.elementsToProcess = elements;
    this.force = force;
  }

  /**
   * Common constructor for all Dart Model operations.
   */
  protected DartModelOperation(DartElement[] elementsToProcess, DartElement[] parentElements) {
    this.elementsToProcess = elementsToProcess;
    this.parentElements = parentElements;
  }

  /**
   * A common constructor for all Dart Model operations.
   */
  protected DartModelOperation(DartElement[] elementsToProcess, DartElement[] parentElements,
      boolean force) {
    this.elementsToProcess = elementsToProcess;
    this.parentElements = parentElements;
    this.force = force;
  }

  @Override
  public void beginTask(String name, int totalWork) {
    if (progressMonitor != null) {
      progressMonitor.beginTask(name, totalWork);
    }
  }

  @Override
  public void done() {
    if (progressMonitor != null) {
      progressMonitor.done();
    }
  }

  /**
   * Convenience method to run an operation within this operation.
   */
  public void executeNestedOperation(DartModelOperation operation, int subWorkAmount)
      throws DartModelException {
    DartModelStatus status = operation.verify();
    if (!status.isOK()) {
      throw new DartModelException(status);
    }
    IProgressMonitor subProgressMonitor = getSubProgressMonitor(subWorkAmount);
    // fix for 1FW7IKC, part (1)
    try {
      operation.setNested(true);
      operation.run(subProgressMonitor);
    } catch (CoreException ce) {
      if (ce instanceof DartModelException) {
        throw (DartModelException) ce;
      } else {
        // translate the core exception to a Dart model exception
        if (ce.getStatus().getCode() == IResourceStatus.OPERATION_FAILED) {
          Throwable e = ce.getStatus().getException();
          if (e instanceof DartModelException) {
            throw (DartModelException) e;
          }
        }
        throw new DartModelException(ce);
      }
    }
  }

  /**
   * Return the Dart Model this operation is operating in.
   * 
   * @return the Dart Model this operation is operating in
   */
  public DartModel getDartModel() {
    return DartModelManager.getInstance().getDartModel();
  }

  /**
   * Return the elements created by this operation.
   * 
   * @return the elements created by this operation
   */
  public DartElement[] getResultElements() {
    return resultElements;
  }

  /**
   * Return <code>true</code> if this operation has performed any resource modifications. Return
   * <code>false</code> if this operation has not been executed yet.
   */
  public boolean hasModifiedResource() {
    return !isReadOnly() && getAttribute(HAS_MODIFIED_RESOURCE_ATTR) == TRUE;
  }

  @Override
  public void internalWorked(double work) {
    if (progressMonitor != null) {
      progressMonitor.internalWorked(work);
    }
  }

  @Override
  public boolean isCanceled() {
    if (progressMonitor != null) {
      return progressMonitor.isCanceled();
    }
    return false;
  }

  /**
   * Return <code>true</code> if this operation performs no resource modifications, otherwise
   * <code>false</code>. Subclasses must override.
   */
  public boolean isReadOnly() {
    return false;
  }

  /**
   * Create and return a new delta on the Dart Model.
   * 
   * @return the delta that was created
   */
  public DartElementDeltaImpl newDartElementDelta() {
    return new DartElementDeltaImpl(getDartModel());
  }

  /**
   * Run this operation and register any deltas created.
   * 
   * @throws CoreException if the operation fails
   */
  @Override
  public void run(IProgressMonitor monitor) throws CoreException {
    DartCore.notYetImplemented();
    DartModelManager manager = DartModelManager.getInstance();
    DeltaProcessor deltaProcessor = manager.getDeltaProcessor();
    int previousDeltaCount = deltaProcessor.dartModelDeltas.size();
    try {
      progressMonitor = monitor;
      pushOperation(this);
      try {
        if (canModifyRoots()) {
          // // computes the root infos before executing the operation
          // // noop if already initialized
          // DartModelManager.getInstance().getDeltaState().initializeRoots(false);
        }

        executeOperation();
      } finally {
        if (isTopLevelOperation()) {
          runPostActions();
        }
      }
    } finally {
      try {
        // re-acquire delta processor as it can have been reset during
        // executeOperation()
        deltaProcessor = manager.getDeltaProcessor();

        // update DartModel using deltas that were recorded during this
        // operation
        for (int i = previousDeltaCount, size = deltaProcessor.dartModelDeltas.size(); i < size; i++) {
          deltaProcessor.updateDartModel(deltaProcessor.dartModelDeltas.get(i));
        }

        // // close the parents of the created elements and reset their
        // // project's cache (in case we are in an IWorkspaceRunnable and the
        // // clients wants to use the created element's parent)
        // // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=83646
        // for (int i = 0, length = resultElements.length; i < length; i++)
        // {
        // DartElement element = resultElements[i];
        // Openable openable = (Openable) element.getOpenable();
        // if (!(openable instanceof CompilationUnit) || !((CompilationUnit)
        // openable).isWorkingCopy()) { // a working copy must remain a child of
        // its parent even after a move
        // ((DartElementImpl) openable.getParent()).close();
        // }
        // switch (element.getElementType()) {
        // case DartElement.PACKAGE_FRAGMENT_ROOT:
        // case DartElement.PACKAGE_FRAGMENT:
        // deltaProcessor.projectCachesToReset.add(element.getDartProject());
        // break;
        // }
        // }
        deltaProcessor.resetProjectCaches();

        // fire only iff:
        // - the operation is a top level operation
        // - the operation did produce some delta(s)
        // - but the operation has not modified any resource
        if (isTopLevelOperation()) {
          if ((deltaProcessor.dartModelDeltas.size() > previousDeltaCount || !deltaProcessor.reconcileDeltas.isEmpty())
              && !hasModifiedResource()) {
            deltaProcessor.fire(null, DeltaProcessor.DEFAULT_CHANGE_EVENT);
          } // else deltas are fired while processing the resource delta
        }
      } finally {
        popOperation();
      }
    }
  }

  /**
   * Main entry point for Dart Model operations. Runs a Dart Model Operation as an
   * IWorkspaceRunnable if not read-only.
   */
  public void runOperation(IProgressMonitor monitor) throws DartModelException {
    DartModelStatus status = verify();
    if (!status.isOK()) {
      throw new DartModelException(status);
    }
    try {
      if (isReadOnly()) {
        run(monitor);
      } else {
        // Use IWorkspace.run(...) to ensure that resource changes are batched
        // Note that if the tree is locked, this will throw a CoreException, but
        // this is ok as this operation is modifying the tree (not read-only)
        // and a CoreException will be thrown anyway.
        ResourcesPlugin.getWorkspace().run(
            this,
            getSchedulingRule(),
            IWorkspace.AVOID_UPDATE,
            monitor);
      }
    } catch (CoreException ce) {
      if (ce instanceof DartModelException) {
        throw (DartModelException) ce;
      } else {
        if (ce.getStatus().getCode() == IResourceStatus.OPERATION_FAILED) {
          Throwable e = ce.getStatus().getException();
          if (e instanceof DartModelException) {
            throw (DartModelException) e;
          }
        }
        throw new DartModelException(ce);
      }
    }
  }

  @Override
  public void setCanceled(boolean b) {
    if (progressMonitor != null) {
      progressMonitor.setCanceled(b);
    }
  }

  @Override
  public void setTaskName(String name) {
    if (progressMonitor != null) {
      progressMonitor.setTaskName(name);
    }
  }

  @Override
  public void subTask(String name) {
    if (progressMonitor != null) {
      progressMonitor.subTask(name);
    }
  }

  @Override
  public void worked(int work) {
    if (progressMonitor != null) {
      progressMonitor.worked(work);

      if (progressMonitor.isCanceled()) {
        // Throw an OperationCanceledException.
        checkCanceled();
      }
    }
  }

  /**
   * Register the given action at the end of the list of actions to run.
   */
  protected void addAction(IPostAction action) {
    int length = actions.length;
    if (length == ++actionsEnd) {
      System.arraycopy(actions, 0, actions = new IPostAction[length * 2], 0, length);
    }
    actions[actionsEnd] = action;
  }

  /**
   * Register the given delta with the Dart Model Manager.
   */
  protected void addDelta(DartElementDelta delta) {
    DartModelManager.getInstance().getDeltaProcessor().registerDartModelDelta(delta);
  }

  /**
   * Register the given reconcile delta with the Dart Model Manager.
   * 
   * @param workingCopy the working copy with which the delta is to be associated
   * @param delta the delta to be added
   */
  protected void addReconcileDelta(CompilationUnit workingCopy, DartElementDeltaImpl delta) {
    HashMap<CompilationUnit, DartElementDelta> reconcileDeltas = DartModelManager.getInstance().getDeltaProcessor().reconcileDeltas;
    DartElementDeltaImpl previousDelta = (DartElementDeltaImpl) reconcileDeltas.get(workingCopy);
    if (previousDelta != null) {
      DartElementDelta[] children = delta.getAffectedChildren();
      for (int i = 0, length = children.length; i < length; i++) {
        DartElementDeltaImpl child = (DartElementDeltaImpl) children[i];
        previousDelta.insertDeltaTree(child.getElement(), child);
      }
      // note that the last delta's AST always takes precedence over the
      // existing delta's AST since it is the result of the last reconcile
      // operation
      if ((delta.getFlags() & DartElementDelta.F_AST_AFFECTED) != 0) {
        previousDelta.changedAST(delta.getCompilationUnitAST());
      }
    } else {
      reconcileDeltas.put(workingCopy, delta);
    }
  }

  protected void applyTextEdit(CompilationUnit cu, TextEdit edits) throws DartModelException {
    try {
      edits.apply(getDocument(cu));
    } catch (BadLocationException e) {
      // content changed under us
      throw new DartModelException(e, DartModelStatusConstants.INVALID_CONTENTS);
    }
  }

  /**
   * Return <code>true</code> if this operation can modify the package fragment roots.
   * 
   * @return <code>true</code> if this operation can modify the package fragment roots
   */
  protected boolean canModifyRoots() {
    return false;
  }

  /**
   * Checks with the progress monitor to see whether this operation should be canceled. An operation
   * should regularly call this method during its operation so that the user can cancel it.
   * 
   * @throws OperationCanceledException if cancelling the operation has been requested
   */
  protected void checkCanceled() {
    if (isCanceled()) {
      throw new OperationCanceledException(Messages.operation_cancelled);
    }
  }

  /**
   * Common code used to verify the elements this operation is processing.
   */
  protected DartModelStatus commonVerify() {
    if (elementsToProcess == null || elementsToProcess.length == 0) {
      return new DartModelStatusImpl(DartModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
    }
    for (int i = 0; i < elementsToProcess.length; i++) {
      if (elementsToProcess[i] == null) {
        return new DartModelStatusImpl(DartModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
      }
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * Convenience method to copy resources.
   */
  protected void copyResources(IResource[] resources, IPath container) throws DartModelException {
    IProgressMonitor subProgressMonitor = getSubProgressMonitor(resources.length);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    try {
      for (int i = 0, length = resources.length; i < length; i++) {
        IResource resource = resources[i];
        IPath destination = container.append(resource.getName());
        if (root.findMember(destination) == null) {
          resource.copy(destination, false, subProgressMonitor);
        }
      }
      setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
    } catch (CoreException e) {
      throw new DartModelException(e);
    }
  }

  /**
   * Convenience method to create a file.
   */
  protected void createFile(IContainer folder, String name, InputStream contents, boolean forceFlag)
      throws DartModelException {
    IFile file = folder.getFile(new Path(name));
    try {
      file.create(contents, forceFlag ? IResource.FORCE | IResource.KEEP_HISTORY
          : IResource.KEEP_HISTORY, getSubProgressMonitor(1));
      setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
    } catch (CoreException e) {
      throw new DartModelException(e);
    }
  }

  /**
   * Convenience method to create a folder.
   */
  protected void createFolder(IContainer parentFolder, String name, boolean forceFlag)
      throws DartModelException {
    IFolder folder = parentFolder.getFolder(new Path(name));
    try {
      // we should use true to create the file locally. Only VCM should use
      // true/false
      folder.create(
          forceFlag ? IResource.FORCE | IResource.KEEP_HISTORY : IResource.KEEP_HISTORY,
          true, // local
          getSubProgressMonitor(1));
      setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
    } catch (CoreException e) {
      throw new DartModelException(e);
    }
  }

  /**
   * Convenience method to delete an empty package fragment.
   */
  // protected void deleteEmptyPackageFragment(
  // IPackageFragment fragment,
  // boolean forceFlag,
  // IResource rootResource)
  // throws DartModelException {
  // IContainer resource = (IContainer) ((DartElementImpl)fragment).resource();
  // try {
  // resource.delete(
  // forceFlag ? IResource.FORCE | IResource.KEEP_HISTORY :
  // IResource.KEEP_HISTORY,
  // getSubProgressMonitor(1));
  // setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
  // while (resource instanceof IFolder) {
  // // deleting a package: delete the parent if it is empty (e.g. deleting x.y
  // where folder x doesn't have resources but y)
  // // without deleting the package fragment root
  // resource = resource.getParent();
  // if (!resource.equals(rootResource) && resource.members().length == 0) {
  // resource.delete(
  // forceFlag ? IResource.FORCE | IResource.KEEP_HISTORY :
  // IResource.KEEP_HISTORY,
  // getSubProgressMonitor(1));
  // setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
  // }
  // }
  // } catch (CoreException e) {
  // throw new DartModelException(e);
  // }
  // }

  /**
   * Convenience method to delete a single resource.
   */
  protected void deleteResource(IResource resource, int flags) throws DartModelException {
    try {
      resource.delete(flags, getSubProgressMonitor(1));
      setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
    } catch (CoreException e) {
      throw new DartModelException(e);
    }
  }

  /**
   * Convenience method to delete resources.
   */
  protected void deleteResources(IResource[] resources, boolean forceFlag)
      throws DartModelException {
    if (resources == null || resources.length == 0) {
      return;
    }
    IProgressMonitor subProgressMonitor = getSubProgressMonitor(resources.length);
    IWorkspace workspace = resources[0].getWorkspace();
    try {
      workspace.delete(resources, forceFlag ? IResource.FORCE | IResource.KEEP_HISTORY
          : IResource.KEEP_HISTORY, subProgressMonitor);
      setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
    } catch (CoreException e) {
      throw new DartModelException(e);
    }
  }

  /**
   * Return <code>true</code> if the given path is equals to one of the given other paths.
   */
  protected boolean equalsOneOf(IPath path, IPath[] otherPaths) {
    for (int i = 0, length = otherPaths.length; i < length; i++) {
      if (path.equals(otherPaths[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Perform the operation specific behavior. Subclasses must override.
   */
  protected abstract void executeOperation() throws DartModelException;

  /**
   * Return the index of the first registered action with the given id, starting from a given
   * position, or -1 if not found.
   * 
   * @return the index of the first registered action with the given id
   */
  protected int firstActionWithID(String id, int start) {
    for (int i = start; i <= actionsEnd; i++) {
      if (actions[i].getID().equals(id)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Return the compilation unit the given element is contained in, or the element itself (if it is
   * a compilation unit), otherwise <code>null</code>.
   * 
   * @return the compilation unit the given element is contained in
   */
  protected CompilationUnit getCompilationUnitFor(DartElement element) {
    return ((DartElementImpl) element).getCompilationUnit();
  }

  /**
   * Return the existing document for the given compilation unit, or a DocumentAdapter if none.
   * 
   * @return the existing document for the given compilation unit
   */
  protected IDocument getDocument(CompilationUnit cu) throws DartModelException {
    Buffer buffer = cu.getBuffer();
    if (buffer instanceof IDocument) {
      return (IDocument) buffer;
    }
    return new DocumentAdapter(buffer);
  }

  /**
   * Return the element to which this operation applies, or <code>null</code> if not applicable.
   * 
   * @return the element to which this operation applies
   */
  protected DartElement getElementToProcess() {
    if (elementsToProcess == null || elementsToProcess.length == 0) {
      return null;
    }
    return elementsToProcess[0];
  }

  // protected IPath[] getNestedFolders(IPackageFragmentRoot root) throws
  // DartModelException {
  // IPath rootPath = root.getPath();
  // IClasspathEntry[] classpath = root.getDartProject().getRawClasspath();
  // int length = classpath.length;
  // IPath[] result = new IPath[length];
  // int index = 0;
  // for (int i = 0; i < length; i++) {
  // IPath path = classpath[i].getPath();
  // if (rootPath.isPrefixOf(path) && !rootPath.equals(path)) {
  // result[index++] = path;
  // }
  // }
  // if (index < length) {
  // System.arraycopy(result, 0, result = new IPath[index], 0, index);
  // }
  // return result;
  // }

  /**
   * Return the parent element to which this operation applies, or <code>null</code> if not
   * applicable.
   * 
   * @return the parent element to which this operation applies
   */
  protected DartElement getParentElement() {
    if (parentElements == null || parentElements.length == 0) {
      return null;
    }
    return parentElements[0];
  }

  /**
   * Return the parent elements to which this operation applies, or <code>null</code> if not
   * applicable.
   * 
   * @return the parent elements to which this operation applies
   */
  protected DartElement[] getParentElements() {
    return parentElements;
  }

  /**
   * Return the scheduling rule for this operation (i.e. the resource that needs to be locked while
   * this operation is running). Subclasses can override.
   * 
   * @return the scheduling rule for this operation
   */
  protected ISchedulingRule getSchedulingRule() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  /**
   * Create and return a sub-progress monitor if appropriate.
   * 
   * @return the sub-progress monitor that was created
   */
  protected IProgressMonitor getSubProgressMonitor(int workAmount) {
    IProgressMonitor sub = null;
    if (progressMonitor != null) {
      sub = new SubProgressMonitor(
          progressMonitor,
          workAmount,
          SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
    }
    return sub;
  }

  /**
   * Return <code>true</code> if this operation is the first operation to run in the current thread.
   * 
   * @return <code>true</code> if this operation is the first operation to run in the current thread
   */
  protected boolean isTopLevelOperation() {
    ArrayList<DartModelOperation> stack = getCurrentOperationStack();
    return stack.size() > 0 && stack.get(0) == this;
  }

  /**
   * Convenience method to move resources.
   */
  protected void moveResources(IResource[] resources, IPath container) throws DartModelException {
    IProgressMonitor subProgressMonitor = null;
    if (progressMonitor != null) {
      subProgressMonitor = new SubProgressMonitor(
          progressMonitor,
          resources.length,
          SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
    }
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    try {
      for (int i = 0, length = resources.length; i < length; i++) {
        IResource resource = resources[i];
        IPath destination = container.append(resource.getName());
        if (root.findMember(destination) == null) {
          resource.move(destination, false, subProgressMonitor);
        }
      }
      setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);
    } catch (CoreException exception) {
      throw new DartModelException(exception);
    }
  }

  /**
   * Remove the last pushed operation from the stack of running operations. Return the popped
   * operation, or <code>null<code> if the stack was empty.
   * 
   * @return the popped operation
   */
  protected DartModelOperation popOperation() {
    ArrayList<DartModelOperation> stack = getCurrentOperationStack();
    int size = stack.size();
    if (size > 0) {
      if (size == 1) { // top level operation
        // release reference (see
        // http://bugs.eclipse.org/bugs/show_bug.cgi?id=33927)
        OPERATION_STACKS.set(null);
      }
      return stack.remove(size - 1);
    } else {
      return null;
    }
  }

  /**
   * Register the given action to be run when the outer most Dart model operation has finished. The
   * insertion mode controls whether: - the action should discard all existing actions with the same
   * id, and be queued at the end (REMOVEALL_APPEND), - the action should be ignored if there is
   * already an action with the same id (KEEP_EXISTING), - the action should be queued at the end
   * without looking at existing actions (APPEND)
   */
  protected void postAction(IPostAction action, int insertionMode) {
    if (POST_ACTION_VERBOSE) {
      System.out.print("(" + Thread.currentThread() + ") [DartModelOperation.postAction(IPostAction, int)] Posting action " + action.getID()); //$NON-NLS-1$ //$NON-NLS-2$
      switch (insertionMode) {
        case REMOVEALL_APPEND:
          System.out.println(" (REMOVEALL_APPEND)"); //$NON-NLS-1$
          break;
        case KEEP_EXISTING:
          System.out.println(" (KEEP_EXISTING)"); //$NON-NLS-1$
          break;
        case APPEND:
          System.out.println(" (APPEND)"); //$NON-NLS-1$
          break;
      }
    }

    DartModelOperation topLevelOp = getCurrentOperationStack().get(0);
    IPostAction[] postActions = topLevelOp.actions;
    if (postActions == null) {
      topLevelOp.actions = postActions = new IPostAction[1];
      postActions[0] = action;
      topLevelOp.actionsEnd = 0;
    } else {
      String id = action.getID();
      switch (insertionMode) {
        case REMOVEALL_APPEND:
          int index = actionsStart - 1;
          while ((index = topLevelOp.firstActionWithID(id, index + 1)) >= 0) {
            // remove action[index]
            System.arraycopy(postActions, index + 1, postActions, index, topLevelOp.actionsEnd
                - index);
            postActions[topLevelOp.actionsEnd--] = null;
          }
          topLevelOp.addAction(action);
          break;
        case KEEP_EXISTING:
          if (topLevelOp.firstActionWithID(id, 0) < 0) {
            topLevelOp.addAction(action);
          }
          break;
        case APPEND:
          topLevelOp.addAction(action);
          break;
      }
    }
  }

  /**
   * Return <code>true</code> if the given path is the prefix of one of the given other paths.
   * 
   * @return <code>true</code> if the given path is the prefix of one of the given other paths
   */
  protected boolean prefixesOneOf(IPath path, IPath[] otherPaths) {
    for (int i = 0, length = otherPaths.length; i < length; i++) {
      if (path.isPrefixOf(otherPaths[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Push the given operation on the stack of operations currently running in this thread.
   * 
   * @param operation the operation to be pushed
   */
  protected void pushOperation(DartModelOperation operation) {
    getCurrentOperationStack().add(operation);
  }

  /**
   * Remove all actions with the given id from the queue of post actions. Does nothing if no such
   * action is in the queue.
   * 
   * @param actionID the id of the actions to be removed
   */
  protected void removeAllPostAction(String actionID) {
    if (POST_ACTION_VERBOSE) {
      System.out.println("(" + Thread.currentThread() + ") [DartModelOperation.removeAllPostAction(String)] Removing actions " + actionID); //$NON-NLS-1$ //$NON-NLS-2$
    }
    DartModelOperation topLevelOp = getCurrentOperationStack().get(0);
    IPostAction[] postActions = topLevelOp.actions;
    if (postActions == null) {
      return;
    }
    int index = actionsStart - 1;
    while ((index = topLevelOp.firstActionWithID(actionID, index + 1)) >= 0) {
      // remove action[index]
      System.arraycopy(postActions, index + 1, postActions, index, topLevelOp.actionsEnd - index);
      postActions[topLevelOp.actionsEnd--] = null;
    }
  }

  /**
   * Unregister the reconcile delta for the given working copy.
   * 
   * @param workingCopy the working copy whose delta is to be removed
   */
  protected void removeReconcileDelta(CompilationUnit workingCopy) {
    DartCore.notYetImplemented();
    // DartModelManager.getInstance().getDeltaProcessor().reconcileDeltas.remove(workingCopy);
  }

  protected void runPostActions() throws DartModelException {
    while (actionsStart <= actionsEnd) {
      IPostAction postAction = actions[actionsStart++];
      if (POST_ACTION_VERBOSE) {
        System.out.println("(" + Thread.currentThread() + ") [DartModelOperation.runPostActions()] Running action " + postAction.getID()); //$NON-NLS-1$ //$NON-NLS-2$
      }
      postAction.run();
    }
  }

  /**
   * Set whether this operation is nested or not.
   * 
   * @see CreateElementInCUOperation#checkCanceled
   */
  protected void setNested(boolean nested) {
    isNested = nested;
  }

  /**
   * Return a status indicating whether there is any known reason this operation will fail.
   * Operations are verified before they are run.
   * <p>
   * Subclasses must override if they have any conditions to verify before this operation executes.
   */
  protected DartModelStatus verify() {
    return commonVerify();
  }
}
