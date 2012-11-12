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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.ElementChangedEvent;
import com.google.dart.tools.core.model.ElementChangedListener;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.StandardDartElementContentProvider;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.progress.UIJob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A subclass of {@link StandardDartElementContentProvider} which implements
 * {@link ElementChangedListener} to update itself when changes are made to the Dart model.
 * <p>
 * This content provider is used by the {@link DartOutlinePage}.
 * 
 * @see DartOutlinePage
 */
public class DartOutlinePageContentProvider extends StandardDartElementContentProvider implements
    ElementChangedListener {

  protected static final int ORIGINAL = 0;
  protected static final int PARENT = 1 << 0;
  protected static final int GRAND_PARENT = 1 << 1;
  protected static final int PROJECT = 1 << 2;

  private TreeViewer viewer;

  private Object input;

  private Collection<Runnable> pendingUpdates;

  private UIJob updateJob;

  /**
   * Creates a new content provider for Dart elements.
   * 
   * @param provideMembers if <code>true</code>, members below compilation units files are provided
   */
  public DartOutlinePageContentProvider(boolean provideMembers) {
    this(provideMembers, true);
  }

  /**
   * Creates a new content provider for Dart elements.
   * 
   * @param provideMembers if <code>true</code>, members below compilation units files are provided
   * @param libsTopLevel of <code>true</code>, then the children of a workspace will be the
   *          libraries in any contained project, not the set of projects
   */
  public DartOutlinePageContentProvider(boolean provideMembers, boolean libsTopLevel) {
    super(provideMembers, libsTopLevel);
    //DartToolsPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
    pendingUpdates = null;
    updateJob = null;
  }

  /**
   * Method declared on {@link IContentProvider}.
   */
  @Override
  public void dispose() {
    super.dispose();
    DartCore.removeElementChangedListener(this);
    //DartToolsPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
  }

  /**
   * @see {@link ElementChangedListener#elementChanged(ElementChangedEvent)}.
   */
  @Override
  public void elementChanged(ElementChangedEvent event) {

    final ArrayList<Runnable> runnables = new ArrayList<Runnable>();
    try {
      // 58952 delete project does not update Package Explorer [package explorer]
      // if the input to the viewer is deleted then refresh to avoid the display of stale elements
      if (inputDeleted(runnables)) {
        return;
      }
      // processDelta(..) is recursively called by this class to walk down the passed
      // DartElementDelta tree, the list of Runnables, see above, is appended to a list to be
      // executed later in this method.
      processDelta(event.getDelta(), runnables);
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    } finally {
      // finally, execute the set of runnables gathered from the call to processDelta(..) above
      executeRunnables(runnables);
    }
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    super.inputChanged(viewer, oldInput, newInput);
    this.viewer = (TreeViewer) viewer;
    if (oldInput == null && newInput != null) {
      DartCore.addElementChangedListener(this);
    } else if (oldInput != null && newInput == null) {
      DartCore.removeElementChangedListener(this);
    }
    input = newInput;
  }

  /**
   * Run all of the runnables that are the widget updates. Must be called in the display thread.
   */
  public void runPendingUpdates() {
    Collection<Runnable> pendingUpdates;
    synchronized (this) {
      pendingUpdates = this.pendingUpdates;
      this.pendingUpdates = null;
    }
    if (pendingUpdates != null && viewer != null) {
      Control control = viewer.getControl();
      if (control != null && !control.isDisposed()) {
        runUpdates(pendingUpdates);
      }
    }
  }

  /**
   * Can be implemented by subclasses to add additional elements to refresh.
   * 
   * @param toRefresh the elements to refresh
   * @param relation the relation to the affected element ({@link #GRAND_PARENT}, {@link #PARENT},
   *          {@link #ORIGINAL}, {@link #PROJECT})
   * @param affectedElement the affected element
   */
  protected void augmentElementToRefresh(List<Object> toRefresh, int relation,
      Object affectedElement) {
  }

  /**
   * This method is responsible for ensuring that it is safe to execute the runnables.
   */
  protected final void executeRunnables(final Collection<Runnable> runnables) {
    // now post all collected runnables
    Control ctrl = viewer.getControl();
    if (ctrl != null && !ctrl.isDisposed()) {
      final boolean hasPendingUpdates;
      synchronized (this) {
        hasPendingUpdates = pendingUpdates != null && !pendingUpdates.isEmpty();
      }
      //Are we in the UIThread? If so spin it until we are done
      if (!hasPendingUpdates && ctrl.getDisplay().getThread() == Thread.currentThread()
          && !viewer.isBusy()) {
        runUpdates(runnables);
      } else {
        synchronized (this) {
          if (pendingUpdates == null) {
            pendingUpdates = runnables;
          } else {
            pendingUpdates.addAll(runnables);
          }
        }
        postAsyncUpdate(ctrl.getDisplay());
      }
    }
  }

  protected Object getViewerInput() {
    return input;
  }

  /**
   * Called at the end of {@link #processDelta(DartElementDelta, Collection)} to process the
   * resource deltas of the passed {@link DartElementDelta}.
   */
  private void handleAffectedChildren(DartElementDelta delta, DartElement element,
      Collection<Runnable> runnables) throws DartModelException {
    int count = 0;

    IResourceDelta[] resourceDeltas = delta.getResourceDeltas();
    if (resourceDeltas != null) {
      for (int i = 0; i < resourceDeltas.length; i++) {
        int kind = resourceDeltas[i].getKind();
        if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
          count++;
        }
      }
    }
    DartElementDelta[] affectedChildren = delta.getAffectedChildren();
    for (int i = 0; i < affectedChildren.length; i++) {
      int kind = affectedChildren[i].getKind();
      if (kind == DartElementDelta.ADDED || kind == DartElementDelta.REMOVED) {
        count++;
      }
    }

    if (count > 1) {
      // more than one child changed, refresh from here downwards
      postRefresh(element, ORIGINAL, element, runnables);
      return;
    }
    if (resourceDeltas != null) {
      for (int i = 0; i < resourceDeltas.length; i++) {
        if (processResourceDelta(resourceDeltas[i], element, runnables)) {
          return; // early return, element got refreshed
        }
      }
    }
    for (int i = 0; i < affectedChildren.length; i++) {
      if (processDelta(affectedChildren[i], runnables)) {
        return; // early return, element got refreshed
      }
    }
  }

  private boolean inputDeleted(Collection<Runnable> runnables) {
    if (input == null) {
      return false;
    }
    if (input instanceof DartElement && ((DartElement) input).exists()) {
      return false;
    }
    if (input instanceof IResource && ((IResource) input).exists()) {
      return false;
    }
    if (input instanceof IWorkingSet) {
      return false;
    }
    postRefresh(input, ORIGINAL, input, runnables);
    return true;
  }

  private boolean isParent(Object root, Object child) {
    Object parent = getParent(child);
    if (parent == null) {
      return false;
    }
    if (parent.equals(root)) {
      return true;
    }
    return isParent(root, parent);
  }

  /**
   * This method adds a {@link Runnable} onto the passed {@link Collection} of runnables, to updated
   * the {@link #viewer} with the addition of the passed element.
   * 
   * @param parent the parent of the new element
   * @param element the new element to be shown in the viewer
   * @param runnables the list of {@link Runnable}s which this method will append an add and refresh
   *          process to
   */
  private void postAdd(final Object parent, final Object element, Collection<Runnable> runnables) {
    if (parent == null) {
      return;
    }

    runnables.add(new Runnable() {
      @Override
      public void run() {
        Widget[] items = viewer.testFindItems(element);
        for (int i = 0; i < items.length; i++) {
          Widget item = items[i];
          if (item instanceof TreeItem && !item.isDisposed()) {
            TreeItem parentItem = ((TreeItem) item).getParentItem();
            if (parentItem != null && !parentItem.isDisposed()
                && parent.equals(parentItem.getData())) {
              return; // no add, element already added (most likely by a refresh)
            }
          }
        }
        // Call the viewer to add this new element into the content provider, then call to refresh.
        viewer.add(parent, element);
        // This call to refresh is needed in the case where a library is being added, any calls to
        // refresh to try to be more specific (e.g refresh(Object element, boolean updateLabels),
        // does not have the new library element added into the tree viewer.
        // TODO a postRefresh(..) should be used instead of any call to viewer.refresh()
        viewer.refresh();
      }
    });
  }

  private void postAsyncUpdate(final Display display) {
    if (updateJob == null) {
      updateJob = new UIJob(display, "Update outline view"
      //PackagesMessages.PackageExplorerContentProvider_update_job_description
      ) {
        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
          TreeViewer viewer = DartOutlinePageContentProvider.this.viewer;
          if (viewer != null && viewer.isBusy()) {
            schedule(100); // reschedule when viewer is busy: bug 184991
          } else {
            runPendingUpdates();
          }
          return Status.OK_STATUS;
        }
      };
      updateJob.setSystem(true);
    }
    updateJob.schedule();
  }

  private void postProjectStateChanged(final Object root, Collection<Runnable> runnables) {
    runnables.add(new Runnable() {
      @Override
      public void run() {
        viewer.refresh(root, true);
        // trigger a synthetic selection change so that action refresh their
        // enable state.
        viewer.setSelection(viewer.getSelection());
      }
    });
  }

  /**
   * This method adds a {@link Runnable} onto the passed {@link Collection} of runnables, to updated
   * the {@link #viewer} to refresh the passed list of elements in the viewer
   * 
   * @param toRefresh a list of elements in the {@link #viewer} to be refreshed
   * @param updateLabels <code>true</code> if the labels should be updated with these calls to
   *          refresh the viewer
   * @param runnables the list of {@link Runnable}s which this method will append an add and refresh
   *          process to
   */
  private void postRefresh(final List<Object> toRefresh, final boolean updateLabels,
      Collection<Runnable> runnables) {
    runnables.add(new Runnable() {
      @Override
      public void run() {
        for (Iterator<Object> iter = toRefresh.iterator(); iter.hasNext();) {
          viewer.refresh(iter.next(), updateLabels);
        }
      }
    });
  }

  private void postRefresh(Object root, int relation, Object affectedElement,
      Collection<Runnable> runnables) {
    // JFace doesn't refresh when object isn't part of the viewer
    // Therefore move the refresh start down to the viewer's input
    if (isParent(root, input) || root instanceof DartModel) {
      root = input;
    }
    List<Object> toRefresh = new ArrayList<Object>(1);
    toRefresh.add(root);
    augmentElementToRefresh(toRefresh, relation, affectedElement);
    postRefresh(toRefresh, true, runnables);
  }

  /**
   * This method adds a Runnable onto the passed {@link Collection} of runnables, to updated the
   * {@link #viewer} with the removal of the passed element.
   * 
   * @param element the new element to be removed from the viewer
   * @param runnables the list of {@link Runnable}s which this method will append an add and refresh
   *          process to
   */
  private void postRemove(final Object element, Collection<Runnable> runnables) {
    runnables.add(new Runnable() {
      @Override
      public void run() {
        // Call the viewer to remove this element from the content provider, then call to refresh.
        viewer.remove(element);
        // This call to refresh is required in the case of removing a library, without this call,
        // tree is not updated.
        // TODO a postRefresh(..) should be used instead of any call to viewer.refresh()
        viewer.refresh(element, false);
      }
    });
  }

  /**
   * Processes a delta recursively. When more than two children are affected the tree is fully
   * refreshed starting at this node.
   * 
   * @param delta the delta to process
   * @param runnables the resulting view changes as runnables (type {@link Runnable})
   * @return true is returned if the conclusion is to refresh a parent of an element. In that case
   *         no siblings need to be processed
   * @throws DartModelException thrown when the access to an element failed
   */
  private boolean processDelta(DartElementDelta delta, Collection<Runnable> runnables)
      throws DartModelException {

    final int kind = delta.getKind();
    final int flags = delta.getFlags();
    final DartElement element = delta.getElement();
    final int elementType = element.getElementType();

    if (elementType != DartElement.DART_MODEL && elementType != DartElement.DART_PROJECT) {
      DartProject proj = element.getDartProject();
      if (proj == null || !proj.getProject().isOpen()) {
        return false;
      }
    }

    // This logic goes through reasons why we wouldn't need to process the passed delta,
    // this is an optimization which may can be evaluated at a later date.
//    if (elementType == DartElement.COMPILATION_UNIT) {
//      CompilationUnit cu = (CompilationUnit) element;
//      if (!DartModelUtil.isPrimary(cu)) {
//        return false;
//      }
//
//      if (!getProvideMembers() && cu.isWorkingCopy() && kind == DartElementDelta.CHANGED) {
//        return false;
//      }

//      if (kind == DartElementDelta.CHANGED
//      //&& !isStructuralCUChange(flags)
//      ) {
//        return false; // test moved ahead
//      }
//    }

    if (elementType == DartElement.DART_PROJECT) {
      // handle open and closing of a project
      if ((flags & (DartElementDelta.F_CLOSED | DartElementDelta.F_OPENED)) != 0) {
        postRefresh(element, ORIGINAL, element, runnables);
        return false;
      }
      // if added it could be that the corresponding IProject is already shown. Remove it first.
      // bug 184296
      if (kind == DartElementDelta.ADDED) {
        postRemove(element.getResource(), runnables);
        postAdd(element.getParent(), element, runnables);
        return false;
      }
    }

    // The following takes care of the cases if the element type is a compilation unit, library or
    // library configuration file
    if (elementType == DartElement.LIBRARY && (flags & DartElementDelta.F_TOP_LEVEL) > 0) {
      postRefresh(DartModelManager.getInstance().getDartModel(), ORIGINAL, element, runnables);
    } else if (elementType == DartElement.COMPILATION_UNIT || elementType == DartElement.LIBRARY) {
      if (kind == DartElementDelta.CHANGED) {
        postRefresh(element, ORIGINAL, element, runnables);
      } else if (kind == DartElementDelta.ADDED) {
        postAdd(element.getParent(), element, runnables);
      } else if (kind == DartElementDelta.REMOVED) {
        postRemove(element, runnables);
      }
      return false;
    }
    handleAffectedChildren(delta, element, runnables);
    return false;
  }

  /**
   * Called only recursively, and by
   * {@link #handleAffectedChildren(DartElementDelta, DartElement, Collection)} to process a
   * resource delta.
   * 
   * @param delta the delta to process
   * @param parent the parent
   * @param runnables the resulting view changes as runnables (type {@link Runnable})
   * @return true if the parent got refreshed
   */
  private boolean processResourceDelta(IResourceDelta delta, Object parent,
      Collection<Runnable> runnables) {
    int status = delta.getKind();
    int flags = delta.getFlags();

    IResource resource = delta.getResource();
    // filter out changes affecting the output folder
    if (resource == null) {
      return false;
    }

    // this could be optimized by handling all the added children in the parent
    if ((status & IResourceDelta.REMOVED) != 0) {
      postRemove(resource, runnables);
      return false;
    }
    if ((status & IResourceDelta.ADDED) != 0) {
      postAdd(parent, resource, runnables);
      return false;
    }
    if ((status & IResourceDelta.CHANGED) != 0) {
      if ((flags & IResourceDelta.TYPE) != 0) {
        postRefresh(parent, PARENT, resource, runnables);
        return true;
      }
    }
    // open/close state change of a project
    if ((flags & IResourceDelta.OPEN) != 0) {
      postProjectStateChanged(internalGetParent(parent), runnables);
      return true;
    }
    IResourceDelta[] resourceDeltas = delta.getAffectedChildren();

    int count = 0;
    for (int i = 0; i < resourceDeltas.length; i++) {
      int kind = resourceDeltas[i].getKind();
      if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
        count++;
        if (count > 1) {
          postRefresh(parent, PARENT, resource, runnables);
          return true;
        }
      }
    }
    for (int i = 0; i < resourceDeltas.length; i++) {
      if (processResourceDelta(resourceDeltas[i], resource, runnables)) {
        return false; // early return, element got refreshed
      }
    }
    return false;
  }

  /**
   * This method loops through the {@link Collection} of {@link Runnable} objects to execute each of
   * them.
   */
  private void runUpdates(Collection<Runnable> runnables) {
    Iterator<Runnable> runnableIterator = runnables.iterator();
    while (runnableIterator.hasNext()) {
      runnableIterator.next().run();
    }
  }
}
