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
package com.google.dart.tools.core.internal.model.delta;

import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartPartOfDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.DartProjectImpl;
import com.google.dart.tools.core.internal.model.DartProjectNature;
import com.google.dart.tools.core.internal.model.ModelUpdater;
import com.google.dart.tools.core.internal.model.OpenableElementImpl;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.internal.model.info.OpenableElementInfo;
import com.google.dart.tools.core.internal.util.LibraryReferenceFinder;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.ElementChangedEvent;
import com.google.dart.tools.core.model.ElementChangedListener;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.core.utilities.resource.IFileUtilities;
import com.google.dart.tools.core.utilities.resource.IResourceUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Instances of the class <code>DeltaProcessor</code> are used by <code>DartModelManager</code> to
 * convert <code>IResourceDelta</code>s into <code>DartElementDelta</code>s. It also does some
 * processing on the <code>DartElementImpl</code>s involved (e.g. setting or removing of parents
 * when elements are added or deleted from the model).
 * <p>
 * High level summary of what the delta processor does:
 * <ul>
 * <li>reacts to resource deltas</li>
 * <li>fires corresponding Dart element deltas</li>
 * <li>deltas also contain non-Dart resources changes</li>
 * <li>updates the model to reflect the Dart element changes</li>
 * <li>refresh external archives (delta, model update, indexing)</li>
 * <li>is thread safe (one delta processor instance per thread, see
 * DeltaProcessingState#resourceChanged(...))</li>
 * </ul>
 * <p>
 * TODO(jwren) Remove the DEBUG flag and replace with Eclipse-tracing
 */
public class DeltaProcessor implements IDeltaProcessor {
  // must not collide with ElementChangedEvent event masks
  public static final int DEFAULT_CHANGE_EVENT = 0;

  public static boolean DEBUG = false;
  public static boolean VERBOSE = false;

  private final static int NON_DART_RESOURCE = -1;

  /**
   * The global state of delta processing.
   */
  private DeltaProcessingState state;

  /**
   * The Dart model manager
   */
  private DartModelManager manager;

  /**
   * The <code>DartElementDeltaImpl</code> corresponding to the <code>IResourceDelta</code> being
   * translated.
   */
  private DartElementDeltaImpl currentDelta;

  /**
   * The Dart element that was last created (see createElement(IResource)). This is used as a stack
   * of Dart elements (using getParent() to pop it, and using the various get*(...) to push it.
   */
  private OpenableElementImpl currentElement;

  /**
   * Queue of deltas created explicitly by the Dart Model that have yet to be fired.
   */
  private List<DartElementDelta> dartModelDeltas = new ArrayList<DartElementDelta>();

  /**
   * Queue of reconcile deltas on working copies that have yet to be fired. This is a mapping from
   * IWorkingCopy to DartElementDelta
   */
  private HashMap<CompilationUnit, DartElementDelta> reconcileDeltas = new HashMap<CompilationUnit, DartElementDelta>();

  /**
   * Turns delta firing on/off. By default it is on.
   * 
   * @see #startDeltas()
   * @see #stopDeltas()
   */
  private boolean isFiring = true;

  /**
   * For each call to {@link #resourceChanged(IResourceChangeEvent)}, each project should call
   * {@link DartProjectImpl#recomputeLibrarySet()} only once. This is a set of the project names for
   * which such a call was made, and thus is used to determine if the call shouldn't be made a
   * second time.
   */
  private Set<String> projectHasRecomputedLibrarySet;

  /**
   * Used to update the DartModel for <code>DartElementDelta</code>s.
   */
  private final ModelUpdater modelUpdater = new ModelUpdater();

  /**
   * A set of DartProjects whose caches need to be reset
   */
  public HashSet<DartProjectImpl> projectCachesToReset = new HashSet<DartProjectImpl>();

  /**
   * The only constructor for this class.
   */
  public DeltaProcessor(DeltaProcessingState state, DartModelManager manager) {
    this.state = state;
    this.manager = manager;
  }

  /**
   * Fire Dart Model delta, flushing them after the fact after post_change notification. If the
   * firing mode has been turned off, this has no effect.
   */
  @Override
  public void fire(DartElementDelta customDelta, int eventType) {
    if (!isFiring) {
      return;
    }

    if (VERBOSE) {
      System.out.println("-----------------------------------------------------------------------------------------------------------------------");//$NON-NLS-1$
    }

    DartElementDelta deltaToNotify;
    if (customDelta == null) {
      deltaToNotify = mergeDeltas(dartModelDeltas);
    } else {
      deltaToNotify = customDelta;
    }

    // Notification

    // Important: if any listener reacts to notification by updating the
    // listeners list or mask, these lists will
    // be duplicated, so it is necessary to remember original lists in a
    // variable (since field values may change under us)
    ElementChangedListener[] listeners;
    int[] listenerMask;
    int listenerCount;
    synchronized (state) {
      listeners = state.elementChangedListeners;
      listenerMask = state.elementChangedListenerMasks;
      listenerCount = state.elementChangedListenerCount;
    }

    switch (eventType) {
      case DEFAULT_CHANGE_EVENT:
      case ElementChangedEvent.POST_CHANGE:
        firePostChangeDelta(deltaToNotify, listeners, listenerMask, listenerCount);
        fireReconcileDelta(listeners, listenerMask, listenerCount);
        break;
    }
  }

  @Override
  public List<DartElementDelta> getDartModelDeltas() {
    return dartModelDeltas;
  }

  @Override
  public HashMap<CompilationUnit, DartElementDelta> getReconcileDeltas() {
    return reconcileDeltas;
  }

  /**
   * Registers the given delta with this delta processor.
   */
  @Override
  public void registerDartModelDelta(DartElementDelta delta) {
    dartModelDeltas.add(delta);
  }

  /**
   * Traverse the set of projects which have changed namespace, and reset their caches and their
   * dependents
   */
  @Override
  public void resetProjectCaches() {
    if (projectCachesToReset.isEmpty()) {
      return;
    }
    for (DartProjectImpl dartProjectImpl : projectCachesToReset) {
      dartProjectImpl.resetCaches();
    }
    projectCachesToReset.clear();
  }

  /**
   * Update Dart Model given some delta
   */
  @Override
  public void updateDartModel(DartElementDelta customDelta) {
    if (customDelta == null) {
      for (int i = 0, length = dartModelDeltas.size(); i < length; i++) {
        DartElementDelta delta = dartModelDeltas.get(i);
        modelUpdater.processDartDelta(delta);
      }
    } else {
      modelUpdater.processDartDelta(customDelta);
    }
  }

  /**
   * Flushes all deltas without firing them.
   */
  protected void flush() {
    dartModelDeltas = new ArrayList<DartElementDelta>();
  }

  /**
   * Notification that some resource changes have happened on the platform, and that the Dart Model
   * should update any required internal structures such that its elements remain consistent.
   * Translates <code>IResourceDeltas</code> into <code>DartElementDeltas</code>.
   * <p>
   * This method is only called from {@link DeltaProcessingState}
   * 
   * @see DeltaProcessingState
   */
  protected void handleResourceChanged(int eventType, IResource resource, DeltaProcessorDelta delta) {
    // reset the contents projectHasRecomputedLibrarySet set
    projectHasRecomputedLibrarySet = new HashSet<String>(1);

    switch (eventType) {
      case IResourceChangeEvent.PRE_CLOSE:
        if (VERBOSE) {
          System.out.println("DeltaProcessor.resourceChanged() " + "CLOSE");
        }

        if (resource.getType() == IResource.PROJECT) {
          IProject project = (IProject) resource;
          File projDir = project.getLocation().toFile();
          PackageLibraryManagerProvider.getDefaultAnalysisServer().discard(projDir);
          DartProjectImpl dartProject = (DartProjectImpl) DartCore.create(project);
          dartProject.clearLibraryInfo();
        }
        return;

      case IResourceChangeEvent.PRE_DELETE:
        if (VERBOSE) {
          System.out.println("DeltaProcessor.resourceChanged() " + "PRE_DELETE");
        }
        try {
          if (resource.getType() == IResource.PROJECT) {
            IProject project = (IProject) resource;
            File projDir = project.getLocation().toFile();
            PackageLibraryManagerProvider.getDefaultAnalysisServer().discard(projDir);
            DartProjectImpl dartProject = (DartProjectImpl) DartCore.create(project);
            dartProject.close();
            removeFromParentInfo(dartProject);
          }
        } catch (CoreException ce) {
          // project doesn't exist or is not open: ignore
        }
        return;

      case IResourceChangeEvent.PRE_REFRESH:
        if (VERBOSE) {
          System.out.println("DeltaProcessor.resourceChanged() " + "PRE_REFRESH");
        }
        return;

      case IResourceChangeEvent.POST_CHANGE:
        if (VERBOSE) {
          System.out.println("DeltaProcessor.resourceChanged() " + "POST_CHANGE");
        }

        try {
          // by calling stopDeltas, isFiring is set to false which prevents the firing of Dart model deltas
          stopDeltas();
          DartElementDelta translatedDelta = processResourceDelta(delta);
          if (translatedDelta != null) {
            registerDartModelDelta(translatedDelta);
          }
        } finally {
          // call startDeltas to allow the firing of Dart model deltas
          startDeltas();
        }

        // fire the delta change events to the listeners
        fire(null, ElementChangedEvent.POST_CHANGE);

        return;

      case IResourceChangeEvent.PRE_BUILD:
        if (VERBOSE) {
          System.out.println("DeltaProcessor.resourceChanged() " + "PRE_BUILD");
        }
        return;

      case IResourceChangeEvent.POST_BUILD:
        if (VERBOSE) {
          System.out.println("DeltaProcessor.resourceChanged() " + "POST_BUILD");
        }

        return;
    }
  }

  /**
   * Adds the given child handle to its parent's cache of children.
   * 
   * @see #removeFromParentInfo(OpenableElementImpl)
   */
  private void addToParentInfo(OpenableElementImpl child) {
    OpenableElementImpl parent = (OpenableElementImpl) child.getParent();
    if (parent != null && parent.isOpen()) {
      try {
        OpenableElementInfo info = (OpenableElementInfo) parent.getElementInfo();
        info.addChild(child);
      } catch (DartModelException e) {
        // do nothing - we already checked if open
      }
    }
  }

  /**
   * Check if contents of packages directory has changed, if so update project info
   */
  private void checkForPackageChanges(IResource deltaRes) {
    if (deltaRes instanceof IFolder && DartCore.isPackagesDirectory((IFolder) deltaRes)) {
      ((DartProjectImpl) DartCore.create(deltaRes.getProject())).recomputePackageInfo();
    }
  }

  /**
   * Closes the given element, which removes it from the cache of open elements.
   */
  private void close(DartElementImpl element) {
    try {
      element.close();
    } catch (DartModelException e) {
      // do nothing
    }
  }

  /**
   * Called by {@link #updateCurrentDelta(DeltaProcessorDelta, int)}, the {@link DartElement}
   * generated by this method is used when the creating the {@link DartElementDelta} elements.
   * <p>
   * Creates the {@link DartElement} openable corresponding to this resource. Returns
   * <code>null</code> if none was found.
   */
  private OpenableElementImpl createElement(IResource resource, int elementType) {
    if (resource == null) {
      return null;
    }

    //IPath path = resource.getFullPath();
    DartElement element = null;
    switch (elementType) {
      case DartElement.DART_PROJECT:

        // note that non-Dart resources rooted at the project level will also
        // enter this code with
        // an elementType DART_PROJECT (see #elementType(...)).
        if (resource instanceof IProject) {
          if (currentElement != null && currentElement.getElementType() == DartElement.DART_PROJECT
              && ((DartProject) currentElement).getProject().equals(resource)) {
            return currentElement;
          }
          IProject proj = (IProject) resource;
          // The following, commented out code, checks that the project has a Dart nature, since all
          // projects in the DartEditor are DartProjects, this check has been removed for the time being.
          //if (DartProjectNature.hasDartNature(proj)) {
          element = DartCore.create(proj);
          //} else {
          // Dart project may have been been closed or removed (look for
          // element amongst old Dart projects list).
          //  element = state.findDartProject(proj.getName());
          //}
        }
        break;
      case DartElement.COMPILATION_UNIT:
        // Note: this element could be a compilation unit or library (if it is a defining compilation unit)
        element = DartCore.create(resource);
        if (element instanceof DartLibrary) {
          try {
            element = ((DartLibrary) element).getDefiningCompilationUnit();
          } catch (DartModelException exception) {
            element = null;
          }
        }

        // if the element is null, then this must be a new dart file, create a new DartLibrary
        if (element == null && resource instanceof IFile) {
          DartProjectImpl dartProject = (DartProjectImpl) DartCore.create(resource.getProject());
          element = new DartLibraryImpl(dartProject, (IFile) resource);
        }

        break;
      case DartElement.HTML_FILE:
        element = DartCore.create(resource);
        break;
    }
    if (element == null) {
      return null;
    }
    currentElement = (OpenableElementImpl) element;
    return currentElement;
  }

  private DartElementDeltaImpl currentDelta() {
    if (currentDelta == null) {
      currentDelta = new DartElementDeltaImpl(manager.getDartModel());
    }
    return currentDelta;
  }

  /**
   * Processing for an element that has been added:
   * <ul>
   * <li>If the element is a project, do nothing, and do not process children, as when a project is
   * created it does not yet have any natures - specifically a Dart nature.
   * <li>If the element is not a project, process it as added (see <code>basicElementAdded</code>.
   * </ul>
   */
  private void elementAdded(OpenableElementImpl element, DeltaProcessorDelta delta) {
    int elementType = element.getElementType();
    // if a project element
    if (elementType == DartElement.DART_PROJECT) {
      // project add is handled by DartProjectNature.configure() because
      // when a project is created, it does not yet have a Dart nature
      IProject project = (IProject) delta.getResource();
      // if this project is a Dart project
      if (delta != null && project != null && DartProjectNature.hasDartNature(project)) {
        //////////
        //try {
        //project.create(project.getDescription(), new NullProgressMonitor());
        //project.open(IResource.BACKGROUND_REFRESH, new NullProgressMonitor());
        //} catch (CoreException e) {
        //  e.printStackTrace();
        //}
        //////////
        addToParentInfo(element);
        if ((delta.getFlags() & DeltaProcessorDelta.MOVED_FROM) != 0) {
          DartElementImpl movedFromElement = (DartElementImpl) element.getDartModel().getDartProject(
              delta.getMovedFromPath().lastSegment());
          currentDelta().movedTo(element, movedFromElement);
        } else {
          // Force the project to be closed as it might have been opened
          // before the resource modification came in and it might have a new
          // child
          // For example, in an IWorkspaceRunnable:
          // 1. create a Dart project P (where P=src)
          // 2. open project P
          // 3. add folder f in P's pkg fragment root
          // When the resource delta comes in, only the addition of P is
          // notified,
          // but the pkg fragment root of project P is already opened, thus its
          // children are not recomputed
          // and it appears to contain only the default package.
          close(element);

          currentDelta().added(element);
        }
        // remember that the project's cache must be reset
        resetThisProjectCache((DartProjectImpl) element);
      }
    } else {
      // else, not a project
      // if a regular, (non-move) add
      if (delta == null || (delta.getFlags() & DeltaProcessorDelta.MOVED_FROM) == 0) {
        // regular element addition
        if (isPrimaryWorkingCopy(element, elementType)) {
          // filter out changes to primary compilation unit in working copy mode
          // just report a change to the resource (see
          // https://bugs.eclipse.org/bugs/show_bug.cgi?id=59500)
          currentDelta().changed(element, DartElementDelta.F_PRIMARY_RESOURCE);
        } else {
          addToParentInfo(element);

          // Force the element to be closed as it might have been opened
          // before the resource modification came in and it might have a new
          // child
          // For example, in an IWorkspaceRunnable:
          // 1. create a package fragment p using a Dart model operation
          // 2. open package p
          // 3. add file X.dart in folder p
          // When the resource delta comes in, only the addition of p is
          // notified,
          // but the package p is already opened, thus its children are not
          // recomputed
          // and it appears empty.
          close(element);

          currentDelta().added(element);
        }
      } else {
        // element is moved
        // TODO This case is not yet supported.
//        addToParentInfo(element);
//        close(element);
//
//        IPath movedFromPath = delta.getMovedFromPath();
//        IResource res = delta.getResource();
//        IResource movedFromRes;
//        if (res instanceof IFile) {
//          movedFromRes = res.getWorkspace().getRoot().getFile(movedFromPath);
//        } else {
//          movedFromRes = res.getWorkspace().getRoot().getFolder(movedFromPath);
//        }
//
//        // find the element type of the moved from element
//        IPath rootPath = externalPath(movedFromRes);
//        RootInfo movedFromInfo = enclosingRootInfo(rootPath, IResourceDelta.REMOVED);
//        int movedFromType = elementType(movedFromRes, IResourceDelta.REMOVED,
//            element.getParent().getElementType(), movedFromInfo);
//
//        // reset current element as it might be inside a nested root
//        // (popUntilPrefixOf() may use the outer root)
//        currentElement = null;
//
//        // create the moved from element
//        DartElementImpl movedFromElement = elementType != DartElement.DART_PROJECT
//            && movedFromType == DartElement.DART_PROJECT ? null : // outside
//                                                                  // classpath
//            createElement(movedFromRes, movedFromType, movedFromInfo);
//        if (movedFromElement == null) {
//          // moved from outside classpath
//          currentDelta().added(element);
//        } else {
//          currentDelta().movedTo(element, movedFromElement);
//        }
      }
    }
  }

  /**
   * Generic processing for a removed element:
   * <ul>
   * <li>Close the element, removing its structure from the cache
   * <li>Remove the element from its parent's cache of children
   * <li>Add a REMOVED entry in the delta
   * </ul>
   * Delta argument could be null if processing an external JAR change
   */
  private void elementRemoved(OpenableElementImpl element, DeltaProcessorDelta delta) {
    int elementType = element.getElementType();
    if (delta == null || (delta.getFlags() & DeltaProcessorDelta.MOVED_TO) == 0) {
      // regular element removal
      if (isPrimaryWorkingCopy(element, elementType)) {
        // filter out changes to primary compilation unit in working copy mode
        // just report a change to the resource (see
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=59500)
        currentDelta().changed(element, DartElementDelta.F_PRIMARY_RESOURCE);
      } else if (elementType == DartElement.COMPILATION_UNIT
          && ((CompilationUnit) element).definesLibrary()) {
        close(element);
        removeFromParentInfo(element);
        currentDelta().removed(element.getParent());
        currentDelta().removed(element);
      } else {
        if (elementType == DartElement.HTML_FILE) {
          try {
            element.getDartProject().updateHtmlMapping(
                element.getCorrespondingResource().getLocation().toPortableString(),
                null,
                false);
          } catch (DartModelException e) {
            DartCore.logError(e);
          }
        }
        close(element);
        removeFromParentInfo(element);
        currentDelta().removed(element);
      }
    } else {
      // element is moved
      // TODO This case is not yet supported.
      // See the JDT code to get started on the various cases
    }
    // remember that the project's cache must be reset
    resetThisProjectCache((DartProjectImpl) element.getDartProject());
  }

  /**
   * Returns the type of the Dart element the given delta matches to. Returns NON_DART_RESOURCE if
   * unknown (e.g. a non-dart resource or excluded .dart file)
   */
  private int elementType(IResource res, int kind, int parentType) {
    switch (parentType) {
      case DartElement.DART_MODEL:
        // case of a movedTo or movedFrom project (other cases are handled in processResourceDelta(...)
        return DartElement.DART_PROJECT;

      case NON_DART_RESOURCE:
      case DartElement.DART_PROJECT:
      default:
        if (res instanceof IFolder) {
          return NON_DART_RESOURCE;
        } else {
          if (DartCore.isDartLikeFileName(res.getName())) {
            return DartElement.COMPILATION_UNIT;
          } else if (DartCore.isHTMLLikeFileName(res.getName())) {
            return DartElement.HTML_FILE;
          }
        }
        return NON_DART_RESOURCE;
    }
  }

  private void firePostChangeDelta(DartElementDelta deltaToNotify,
      ElementChangedListener[] listeners, int[] listenerMask, int listenerCount) {

    // post change deltas
    if (VERBOSE) {
      System.out.println("FIRING POST_CHANGE Delta [" + Thread.currentThread() + "]:"); //$NON-NLS-1$//$NON-NLS-2$
      System.out.println(deltaToNotify == null ? "<NONE>" : deltaToNotify.toString()); //$NON-NLS-1$
    }
    if (deltaToNotify != null) {
      // flush now so as to keep listener reactions to post their own deltas for
      // subsequent iteration
      flush();

      // mark the operation stack has not modifying resources since resource
      // deltas are being fired
      // DartModelOperation.setAttribute(
      // DartModelOperation.HAS_MODIFIED_RESOURCE_ATTR, null);

      notifyListeners(
          deltaToNotify,
          ElementChangedEvent.POST_CHANGE,
          listeners,
          listenerMask,
          listenerCount);
    }
  }

  private void fireReconcileDelta(ElementChangedListener[] listeners, int[] listenerMask,
      int listenerCount) {

    DartElementDelta deltaToNotify = mergeDeltas(reconcileDeltas.values());
    if (VERBOSE) {
      System.out.println("FIRING POST_RECONCILE Delta [" + Thread.currentThread() + "]:"); //$NON-NLS-1$//$NON-NLS-2$
      System.out.println(deltaToNotify == null ? "<NONE>" : deltaToNotify.toString()); //$NON-NLS-1$
    }
    if (deltaToNotify != null) {
      // flush now so as to keep listener reactions to post their own deltas for
      // subsequent iteration
      reconcileDeltas = new HashMap<CompilationUnit, DartElementDelta>();

      notifyListeners(
          deltaToNotify,
          ElementChangedEvent.POST_RECONCILE,
          listeners,
          listenerMask,
          listenerCount);
    }
  }

  /**
   * This method is used by the JDT, it is left here commented out, for possible future work in this
   * file regarding non-Dart resource change events.
   * <p>
   * Generic processing for elements with changed contents:
   * <ul>
   * <li>The element is closed such that any subsequent accesses will re-open the element reflecting
   * its new structure.
   * <li>An entry is made in the delta reporting a content change (K_CHANGE with F_CONTENT flag
   * set).
   * </ul>
   */
//  private void nonDartResourcesChanged(DartElementImpl element, IResourceDelta delta)
//      throws DartModelException {
//    switch (element.getElementType()) {
//      case DartElement.DART_PROJECT:
//        currentDelta().addResourceDelta(delta);
//        return;
//    }
//    DartElementDeltaImpl current = currentDelta();
//    DartElementDeltaImpl elementDelta = current.find(element);
//    if (elementDelta == null) {
//      // don't use find after creating the delta as it can be null (see
//      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=63434)
//      elementDelta = current.changed(element, DartElementDelta.F_CONTENT);
//    }
//  }

  /**
   * Returns whether the given element is a primary compilation unit in working copy mode.
   */
  private boolean isPrimaryWorkingCopy(DartElement element, int elementType) {
    if (elementType == DartElement.COMPILATION_UNIT) {
      CompilationUnit cu = (CompilationUnit) element;
      return cu.isPrimary() && cu.isWorkingCopy();
    }
    return false;
  }

  /**
   * Merges all awaiting deltas, and returns the merged {@link DartElementDelta}.
   */
  private DartElementDelta mergeDeltas(Collection<DartElementDelta> deltas) {
    if (deltas.size() == 0) {
      return null;
    }
    if (deltas.size() == 1) {
      return deltas.iterator().next();
    }

    if (VERBOSE) {
      System.out.println("MERGING " + deltas.size() + " DELTAS [" + Thread.currentThread() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    Iterator<DartElementDelta> iterator = deltas.iterator();
    DartElementDeltaImpl rootDelta = new DartElementDeltaImpl(manager.getDartModel());
    boolean insertedTree = false;
    while (iterator.hasNext()) {
      DartElementDeltaImpl delta = (DartElementDeltaImpl) iterator.next();
      if (VERBOSE) {
        System.out.println(delta.toString());
      }
      DartElement element = delta.getElement();
      if (manager.getDartModel().equals(element)) {
        DartElementDelta[] children = delta.getAffectedChildren();
        for (int j = 0; j < children.length; j++) {
          DartElementDeltaImpl projectDelta = (DartElementDeltaImpl) children[j];
          rootDelta.insertDeltaTree(projectDelta.getElement(), projectDelta);
          insertedTree = true;
        }
        IResourceDelta[] resourceDeltas = delta.getResourceDeltas();
        if (resourceDeltas != null) {
          for (int i = 0, length = resourceDeltas.length; i < length; i++) {
            rootDelta.addResourceDelta(resourceDeltas[i]);
            insertedTree = true;
          }
        }
      } else {
        rootDelta.insertDeltaTree(element, delta);
        insertedTree = true;
      }
    }
    if (insertedTree) {
      return rootDelta;
    }
    return null;
  }

  /**
   * Notifies the list of {@link ElementChangedListener}s. The list of listeners is passed from
   * {@link DeltaProcessingState}.
   * 
   * @see DeltaProcessingState#elementChangedListeners
   * @see DeltaProcessingState#elementChangedListenerMasks
   * @see DeltaProcessingState#elementChangedListenerCount
   */
  private void notifyListeners(DartElementDelta deltaToNotify, int eventType,
      ElementChangedListener[] listeners, int[] listenerMask, int listenerCount) {
    final ElementChangedEvent elementChangeEvent = new ElementChangedEvent(deltaToNotify, eventType);
    for (int i = 0; i < listenerCount; i++) {
      if ((listenerMask[i] & eventType) != 0) {
        final ElementChangedListener listener = listeners[i];
        long start = -1;
        if (VERBOSE) {
          System.out.print("Listener #" + (i + 1) + "=" + listener.toString());//$NON-NLS-1$//$NON-NLS-2$
          start = System.currentTimeMillis();
        }
        // wrap callbacks with Safe runnable for subsequent listeners to be
        // called when some are causing grief
        SafeRunner.run(new ISafeRunnable() {
          @Override
          public void handleException(Throwable exception) {
            DartCore.logError("Exception occurred in listener of Dart element change notification", //$NON-NLS-1$
                exception);
          }

          @Override
          public void run() throws Exception {
            listener.elementChanged(elementChangeEvent);
          }
        });
        if (VERBOSE) {
          System.out.println(" -> " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
    }
  }

  /**
   * Converts a <code>IResourceDelta</code> rooted in a <code>Workspace</code> into the
   * corresponding set of <code>DartElementDelta</code>, rooted in the relevant
   * <code>DartModelImpl</code>s.
   */
  private DartElementDelta processResourceDelta(DeltaProcessorDelta parentDelta) {
    try {
      currentElement = null;

      // get the workspace delta, and start processing there.
      DeltaProcessorDelta[] children = parentDelta.getAffectedChildren();

      // traverse each delta
      for (DeltaProcessorDelta child : children) {
        traverseDelta(child, DartElement.DART_PROJECT);
      }
      resetProjectCaches();

      return currentDelta;
    } finally {
      currentDelta = null;
    }
  }

  private void recomputeLibrarySet(DartElement dartElement) {
    DartProjectImpl dartProject = (DartProjectImpl) dartElement.getDartProject();
    if (!projectHasRecomputedLibrarySet.contains(dartProject.getElementName())) {
      if (dartProject.isOpen()) {
        dartProject.recomputeLibrarySet();
        projectHasRecomputedLibrarySet.add(dartProject.getElementName());
      }
    }
  }

  /**
   * Removes the given element from its parents cache of children. If the element does not have a
   * parent, or the parent is not currently open, this has no effect.
   * 
   * @see #addToParentInfo(OpenableElementImpl)
   */
  private void removeFromParentInfo(OpenableElementImpl child) {
    OpenableElementImpl parent = (OpenableElementImpl) child.getParent();
    if (parent != null && parent.isOpen()) {
      try {
        OpenableElementInfo info = (OpenableElementInfo) parent.getElementInfo();
        info.removeChild(child);
      } catch (DartModelException e) {
        // do nothing - we already checked if open
      }
    }
  }

  /**
   * Checks the source for changes that affect the model in such a way that the project cache needs
   * to be reset
   * 
   * @param dartElement the compilation unit with the change
   * @return true if project cache reset, false otherwise
   */
  private boolean requiresProjectCacheReset(DartElement dartElement) {
    try {
      CompilationUnitImpl cu = (CompilationUnitImpl) dartElement;
      DartUnit unit = DartCompilerUtilities.parseSource(cu.getElementName(), cu.getSource());

      if (unit != null) {
        List<DartDirective> directives = unit.getDirectives();
        if (cu.definesLibrary()) {
          // check if directives have changed
          DartLibrary library = cu.getLibrary();
          CompilationUnit[] libraryParts = library.getCompilationUnits();
          if (libraryParts.length > 1 && directives.isEmpty()) {
            return true;
          } else {
            List<String> names = new ArrayList<String>();
            for (CompilationUnit part : libraryParts) {
              names.add(part.getElementName());
            }
            int noOfSources = 1;
            // check if any of the part directives have changed
            for (DartDirective directive : directives) {
              if (directive instanceof DartSourceDirective) {
                noOfSources++;
                String importUri = ((DartSourceDirective) directive).getSourceUri().getValue();
                if (!names.contains(importUri)) {
                  return true;
                }
              }
            }
            // check for sources removed
            if (libraryParts.length != noOfSources) {
              return true;
            }
          }

        } else {
          // change from non lib -> lib 
          if (directives.isEmpty()) {
            return false;
          }
          if (directives.size() == 1 && directives.get(0) instanceof DartPartOfDirective) {
            return false;
          }
          return true;
        }
      }

    } catch (DartModelException e) {
      return true;
    }
    return false;
  }

  /**
   * This is called by the {@link DeltaProcessor} when some Dart project has been changed.
   * <p>
   * Since the user cannot directly delete, open or close the dart projects, this is currently only
   * ever called when the user creates (or opens) a new dart library.
   * <p>
   * By enforcing all callers of <code>projectCachesToReset.add(..)</code> to use this method, this
   * method can be used easily for debugging of the project cache story.
   * 
   * @see DeltaProcessor#resetProjectCaches()
   * @param dartProjectImpl some non-<code>null</code> dart project
   * @return <code>true</code> if this set did not already contain the specified element
   */
  private boolean resetThisProjectCache(DartProjectImpl dartProjectImpl) {
    return projectCachesToReset.add(dartProjectImpl);
  }

  /**
   * Turns the firing mode to on. That is, deltas that are/have been registered will be fired.
   */
  private void startDeltas() {
    isFiring = true;
  }

  /**
   * Turns the firing mode to off. That is, deltas that are/have been registered will not be fired
   * until deltas are started again.
   */
  private void stopDeltas() {
    isFiring = false;
  }

  /**
   * Converts an <code>IResourceDelta</code> and its children into the corresponding
   * <code>DartElementDelta</code>s.
   */
  private void traverseDelta(DeltaProcessorDelta delta, int elementType) {
    if (DEBUG) {
      System.out.println("DeltaProcessor.traverseDelta() type = " + delta.getResource().getClass()
          + ", delta.getResource().getName() = \"" + delta.getResource().getFullPath().toOSString()
          + "\"");
    }
    // process current delta
    boolean processChildren = updateCurrentDelta(delta, elementType);

    // process children if needed
    if (processChildren) {
      DeltaProcessorDelta[] children = delta.getAffectedChildren();

      // for each of the children, also update the current delta, by calling this method recursively
      for (DeltaProcessorDelta child : children) {
        IResource childRes = child.getResource();
        ////////
        // Optimization: if a generated dart file, then don't process delta
        if (childRes instanceof IFile && DartCore.isDartGeneratedFile(childRes.getFileExtension())) {
          if (VERBOSE) {
            System.out.println("Not traversing over the following file since it is generated by Dart: "
                + childRes.getFullPath().toOSString());
          }
          continue;
        }
        ////////
        int childKind = child.getKind();
        int childType = elementType(childRes, childKind, elementType);
        traverseDelta(child, childType);
//        if (childType == NON_DART_RESOURCE) {
//          if (parent == null && elementType == DartElement.DART_PROJECT) {
//            parent = createElement(res, elementType);
//          }
//          if (parent == null) {
//            continue;
//          }
//          try {
//            nonDartResourcesChanged(parent, child);
//          } catch (DartModelException e) {
//          }
//        }
      }
    } // else resource delta will be added by parent
  }

  /**
   * Update the current delta (i.e. add/remove/change the given element) and update the
   * corresponding index. Returns whether the children of the given delta must be processed.
   * 
   * @return <code>true</code> if the children of the given delta must be processed.
   * @throws a DartModelException if the delta doesn't correspond to a Dart element of the given
   *           type.
   */
  private boolean updateCurrentDelta(DeltaProcessorDelta delta, int elementType) {
    IResource deltaRes = delta.getResource();
    boolean buildStructure = false;
    if (DEBUG) {
      String kindStr;
      if (delta.getKind() == DeltaProcessorDelta.ADDED) {
        kindStr = "ADD";
      } else if (delta.getKind() == DeltaProcessorDelta.REMOVED) {
        kindStr = "REMOVE";
      } else if (delta.getKind() == DeltaProcessorDelta.CHANGED) {
        kindStr = "CHANGED";
      } else {
        kindStr = "OTHER delta.getKind() = " + Integer.toString(delta.getKind());
      }
      System.out.println("DeltaProcessor.updateCurrentDelta() called for this resource: \""
          + deltaRes.getFullPath().toOSString() + "\" - " + kindStr);
    }
    OpenableElementImpl element;

    checkForPackageChanges(deltaRes);

    switch (delta.getKind()) {
      case DeltaProcessorDelta.ADDED:
        element = createElement(deltaRes, elementType);
        if (DartCore.isHTMLLikeFileName(deltaRes.getName())) {
          buildStructure = updateHtmlMapping(deltaRes);

        }
        if (element == null) {
          if (buildStructure) {
            resetThisProjectCache((DartProjectImpl) DartCore.create(deltaRes.getProject()));
          }
          return true;
        }
        elementAdded(element, delta);
        recomputeLibrarySet(element);
//        // if this element is a CompilationUnit that defines a library, make sure that we specify that the
//        // DartLibrary is being added
//        if (elementType == DartElement.COMPILATION_UNIT) {
//          // if the element is a LibraryConfigurationFile, then we need to post an add of it's parent
//          if (((CompilationUnit) element).definesLibrary()) {
//            elementAdded((DartLibraryImpl) element.getParent(), delta);
//          }
//        }
        return false;
      case DeltaProcessorDelta.REMOVED:
        element = createElement(deltaRes, elementType);
        if (element == null) {
          return true;
        }
        // If the element being removed is a DartProject, do NOT have its' children visited
        // recursively, return false.
        if (element instanceof DartProject) {
          return false;
        }
        elementRemoved(element, delta);
        recomputeLibrarySet(element);
//        // if this element is a CompilationUnit that defines a library, make sure that we specify that the
//        // DartLibrary is being removed
//        if (elementType == DartElement.COMPILATION_UNIT) {
//          if (((CompilationUnit) element).definesLibrary()) {
//            elementRemoved((DartLibraryImpl) element.getParent(), delta);
//          }
//        }
        // Note: the JDT has a special case for projects, we may need some special case as well
        // later on, the DartModelManager currently doesn't have the equivalent methods
        //if (deltaRes.getType() == IResource.PROJECT) {
        //// reset the corresponding project built state, since cannot reuse if added back
        //...
        //}
        return false;

      case DeltaProcessorDelta.CHANGED:
        int flags = delta.getFlags();
        if ((flags & DeltaProcessorDelta.CONTENT) != 0
            || (flags & DeltaProcessorDelta.ENCODING) != 0) {
          // content or encoding has changed
          if (DartCore.isHTMLLikeFileName(deltaRes.getName())) {
            buildStructure = updateHtmlMapping(deltaRes);
          }
          element = createElement(delta.getResource(), elementType);
          if (element == null) {
            if (buildStructure) {
              resetThisProjectCache((DartProjectImpl) DartCore.create(deltaRes.getProject()));
            }
            return true;
          }

          if (element instanceof CompilationUnitImpl) {
            if (requiresProjectCacheReset(element)) {
              recomputeLibrarySet(element);
              resetThisProjectCache((DartProjectImpl) element.getDartProject());
            } else {
              try {
                if (((CompilationUnitImpl) element).definesLibrary()) {
                  element = (DartLibraryImpl) ((CompilationUnitImpl) element).getLibrary();
                }
                DartModelManager.getInstance().removeInfoAndChildren(element);
              } catch (DartModelException e) {
                DartCore.logError(e);
              }
            }
          } else {
            recomputeLibrarySet(element);
            resetThisProjectCache((DartProjectImpl) element.getDartProject());
          }

          // This has been replaced by the call to recomputeLibrarySet, more a *hammer* approach to
          // get the Libraries view working ASAP, this could be re-visited in the future to make the
          // delta processing a faster process.
          //contentChanged(element, delta);
        }
        // The following has all been commented out as DartProjects cannot be opened or closed in
        // the current UX (adding and removing libraries is different than closing a project).
//        else if (elementType == DartElement.DART_PROJECT) {
//          if ((flags & IResourceDelta.OPEN) != 0) {
//            // project has been opened or closed
//            IProject res = (IProject) delta.getResource();
//            element = createElement(res, elementType);
//            if (element == null) {
//              // resource might be containing shared roots (see bug 19058)
//              //state.updateRoots(res.getFullPath(), delta, this);
//              return false;
//            }
//            if (res.isOpen()) {
//              if (DartProjectNature.hasDartNature(res)) {
//                addToParentInfo(element);
//                currentDelta().opened(element);
//
//                // remember that the project's cache must be reset
//                projectCachesToReset.add((DartProjectImpl) element);
//              }
//            } else {
//              boolean wasDartProject = state.findDartProject(res.getName()) != null;
//              if (wasDartProject) {
//                close(element);
//                removeFromParentInfo(element);
//                currentDelta().closed(element);
//              }
//            }
//            // when a project is opened/closed don't process children
//            return false;
//          }
//          if ((flags & IResourceDelta.DESCRIPTION) != 0) {
//            IProject res = (IProject) delta.getResource();
//            boolean wasDartProject = state.findDartProject(res.getName()) != null;
//            boolean isDartProject = DartProjectNature.hasDartNature(res);
//            if (wasDartProject != isDartProject) {
//              // project's nature has been added or removed
//              element = createElement(res, elementType);
//              if (element == null) {
//                // note its resources are still visible as roots to other projects
//                return false;
//              }
//              if (isDartProject) {
//                elementAdded(element, delta);
//              } else {
//                elementRemoved(element, delta);
//              }
//              // when a project's nature is added/removed don't process children
//              return false;
//            }
//          }
//        }
        return true;
    }
    return true;
  }

  private boolean updateHtmlMapping(IResource htmlFile) {
    try {
      if (htmlFile.exists()) {
        String contents = IFileUtilities.getContents((IFile) htmlFile);
        List<String> libraryNames = LibraryReferenceFinder.findInHTML(contents);

        if (!libraryNames.isEmpty()) {
          List<String> libraryPaths = IResourceUtilities.getResolvedFilePaths(
              htmlFile,
              libraryNames);
          String key = htmlFile.getLocation().toPortableString();
          DartCore.create(htmlFile.getProject()).updateHtmlMapping(key, libraryPaths, true);
          return true;
        }
      }
    } catch (DartModelException e) {
      DartCore.logError(e);
      resetThisProjectCache((DartProjectImpl) DartCore.create(htmlFile.getProject()));
    } catch (Exception e) {
      DartCore.logError(e);
    }

    return false;
  }

}
