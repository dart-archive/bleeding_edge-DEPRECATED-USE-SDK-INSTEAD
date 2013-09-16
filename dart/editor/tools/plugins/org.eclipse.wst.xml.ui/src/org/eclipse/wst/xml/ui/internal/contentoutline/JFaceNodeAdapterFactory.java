/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentoutline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.sse.core.internal.provisional.AbstractAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.util.Assert;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapterFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManagerListener;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMDocumentCache;
import org.eclipse.wst.xml.core.internal.ssemodelquery.ModelQueryAdapter;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

/**
 * An adapter factory to create JFaceNodeAdapters. Use this adapter factory with a
 * JFaceAdapterContentProvider to display DOM nodes in a tree.
 */
public class JFaceNodeAdapterFactory extends AbstractAdapterFactory implements
    IJFaceNodeAdapterFactory {
  public class CMDocumentManagerListenerImpl implements CMDocumentManagerListener {
    private static final int UPDATE_DELAY = 200;

    public void cacheCleared(CMDocumentCache cache) {
      // nothing to do
    }

    public void cacheUpdated(CMDocumentCache cache, final String uri, int oldStatus, int newStatus,
        CMDocument cmDocument) {
      if ((newStatus == CMDocumentCache.STATUS_LOADED)
          || (newStatus == CMDocumentCache.STATUS_ERROR)) {
        refreshViewers();
      }
    }

    public void propertyChanged(CMDocumentManager cmDocumentManager, String propertyName) {
      if (cmDocumentManager.getPropertyEnabled(CMDocumentManager.PROPERTY_AUTO_LOAD)) {
        refreshViewers();
      }
    }

    private void refreshViewers() {
      Object[] listeners = getListeners().toArray();
      for (int i = 0; i < listeners.length; i++) {
        if (listeners[i] instanceof StructuredViewer) {
          final StructuredViewer viewer = (StructuredViewer) listeners[i];
          Job refresh = new UIJob(XMLUIMessages.refreshoutline_0) {
            public IStatus runInUIThread(IProgressMonitor monitor) {
              Control refreshControl = viewer.getControl();
              if ((refreshControl != null) && !refreshControl.isDisposed()) {
                viewer.refresh(true);
              }
              return Status.OK_STATUS;
            }
          };
          refresh.setSystem(true);
          refresh.setPriority(Job.SHORT);
          refresh.schedule(UPDATE_DELAY);
        } else if (listeners[i] instanceof Viewer) {
          final Viewer viewer = (Viewer) listeners[i];
          Job refresh = new UIJob(XMLUIMessages.refreshoutline_0) {
            public IStatus runInUIThread(IProgressMonitor monitor) {
              Control refreshControl = viewer.getControl();
              if ((refreshControl != null) && !refreshControl.isDisposed()) {
                viewer.refresh();
              }
              return Status.OK_STATUS;
            }
          };
          refresh.setSystem(true);
          refresh.setPriority(Job.SHORT);
          refresh.schedule(UPDATE_DELAY);
        }
      }
    }
  }

  private CMDocumentManager cmDocumentManager;
  private CMDocumentManagerListenerImpl fCMDocumentManagerListener = null;
  /**
   * This keeps track of all the listeners.
   */
  private Set fListeners = new HashSet();

  protected INodeAdapter singletonAdapter;

  public JFaceNodeAdapterFactory() {
    this(IJFaceNodeAdapter.class, true);
  }

  public JFaceNodeAdapterFactory(Object adapterKey, boolean registerAdapters) {
    super(adapterKey, registerAdapters);
  }

  public synchronized void addListener(Object listener) {
    fListeners.add(listener);
  }

  public INodeAdapterFactory copy() {
    return new JFaceNodeAdapterFactory(getAdapterKey(), isShouldRegisterAdapter());
  }

  /**
   * Create a new JFace adapter for the DOM node passed in
   */
  protected INodeAdapter createAdapter(INodeNotifier node) {
    if (singletonAdapter == null) {
      // create the JFaceNodeAdapter
      singletonAdapter = new JFaceNodeAdapter(this);
      initAdapter(singletonAdapter, node);
    }
    return singletonAdapter;
  }

  /**
   * returns "copy" so no one can modify our list. It is a shallow copy.
   */
  public synchronized Collection getListeners() {
    return new ArrayList(fListeners);
  }

  protected void initAdapter(INodeAdapter adapter, INodeNotifier node) {
    Assert.isTrue(cmDocumentManager == null);
    Assert.isTrue(fCMDocumentManagerListener == null);

    // register for CMDocumentManager events
    ModelQueryAdapter mqadapter = (ModelQueryAdapter) node.getAdapterFor(ModelQueryAdapter.class);
    if (mqadapter != null) {
      ModelQuery mquery = mqadapter.getModelQuery();
      if ((mquery != null) && (mquery.getCMDocumentManager() != null)) {
        cmDocumentManager = mquery.getCMDocumentManager();
        fCMDocumentManagerListener = new CMDocumentManagerListenerImpl();
        cmDocumentManager.addListener(fCMDocumentManagerListener);
      }
    }
  }

  public void release() {
    // deregister from CMDocumentManager events
    if ((cmDocumentManager != null) && (fCMDocumentManagerListener != null)) {
      cmDocumentManager.removeListener(fCMDocumentManagerListener);
    }
    fListeners.clear();
    if (singletonAdapter != null && singletonAdapter instanceof JFaceNodeAdapter) {
      RefreshStructureJob refreshJob = ((JFaceNodeAdapter) singletonAdapter).fRefreshJob;
      if (refreshJob != null) {
        refreshJob.cancel();
      }
    }
  }

  public synchronized void removeListener(Object listener) {
    fListeners.remove(listener);
  }
}
