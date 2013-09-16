/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal;

import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;

/**
 * This is just an adapter that sits on a document node to allow clients limited access to the
 * DOMObserver. Clients who handle their own content model document loading (as opposed to letting
 * the DOMObserver do it) may use this class to stop the DOMObserver from loading and possibly
 * interfering with the client's document loading.
 */
public class DOMObserverAdapter implements INodeAdapter {
  private DOMObserver fObserver = null;

  public boolean isAdapterForType(Object type) {
    return type == DOMObserverAdapter.class;
  }

  public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature,
      Object oldValue, Object newValue, int pos) {
    // do nothing
  }

  /**
   * The DOMObserver is the one that adds the adapter to the document node, so it'll set itself up
   * here as well.
   * 
   * @param observer
   */
  void setDOMObserver(DOMObserver observer) {
    fObserver = observer;
  }

  /**
   * Disable the DOMObserver to prevent it from future content model loading for this document.
   * 
   * @param disable true if caller wants DOMObserver disabled. false if caller wants DOMObserver
   *          enabled.
   * @param force if true, DOMObserver will forcibly be disabled (if the DOMObserver model loading
   *          job is scheduled, it will be cancelled)
   * @return true if DOMObserver was successfully disabled. false if DOMObserver was already in the
   *         process of loading and was unable to stop
   */
  public boolean disableObserver(boolean disable, boolean force) {
    return fObserver.setDisabled(disable, force);
  }

  /**
   * Returns whether or not DOMObserver is currently attempting to load the content model.
   * 
   * @return true if DOMObserver is currently in the process of loading the content models. false
   *         otherwise.
   */
  public boolean isObserverLoading() {
    return fObserver.isLoading();
  }
}
