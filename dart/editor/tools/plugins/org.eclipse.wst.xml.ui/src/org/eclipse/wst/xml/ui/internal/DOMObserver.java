/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.modelqueryimpl.CMDocumentLoader;
import org.eclipse.wst.xml.core.internal.contentmodel.modelqueryimpl.InferredGrammarBuildingCMDocumentLoader;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class is used to observe changes in the DOM and perform occasional'scans' to deduce
 * information. We use a delay timer mechanism to ensure scans are made every couple of seconds to
 * avoid performance problems. Currently this class is used to keep track of referenced grammar
 * uri's within the document ensure that they are loaded by the CMDocumentManager. We might want to
 * generalize this class to perform other suplimental information gathering that is suitable for
 * 'time delayed' computation (error hints etc.).
 */
// TODO: Where should this class go?
public class DOMObserver {

  // An abstract adapter that ensures that the children of a new Node are
  // also adapted
  //
  abstract class DocumentAdapter implements INodeAdapter {
    public DocumentAdapter() {
    }

    public void connect(Document document) {
      ((INodeNotifier) document).addAdapter(this);
      adapt(document.getDocumentElement());
    }

    public void dicconnect(Document document) {
      ((INodeNotifier) document).removeAdapter(this);
    }

    public void adapt(Element element) {
      if (element != null) {
        if (((INodeNotifier) element).getExistingAdapter(this) == null) {
          ((INodeNotifier) element).addAdapter(this);

          for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
              adapt((Element) child);
            }
          }
        }
      }
    }

    public boolean isAdapterForType(Object type) {
      return type == this;
    }

    abstract public void notifyChanged(INodeNotifier notifier, int eventType, Object feature,
        Object oldValue, Object newValue, int index);
  }

  /**
   * This class listens to the changes in the CMDocument and triggers a CMDocument load
   */
  class MyDocumentAdapter extends DocumentAdapter {

    public void notifyChanged(INodeNotifier notifier, int eventType, Object feature,
        Object oldValue, Object newValue, int index) {
      switch (eventType) {
        case INodeNotifier.ADD: {
          if (newValue instanceof Element) {
            // System.out.println("ADD (to " +
            // ((Node)notifier).getNodeName() + ") " +
            // ((Element)newValue).getNodeName() + " old " +
            // oldValue);
            adapt((Element) newValue);
          }
          break;
        }
        // case INodeNotifier.REMOVE:
        case INodeNotifier.CHANGE:
        case INodeNotifier.STRUCTURE_CHANGED:
        case INodeNotifier.CONTENT_CHANGED: {
          Node node = (Node) notifier;
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            switch (eventType) {
              case INodeNotifier.CHANGE:
              case INodeNotifier.STRUCTURE_CHANGED: {
                // structure change
                invokeDelayedCMDocumentLoad();
                break;
              }
              case INodeNotifier.CONTENT_CHANGED: {
                // some content changed
                break;
              }
            }
          } else if (node.getNodeType() == Node.DOCUMENT_NODE) {
            invokeDelayedCMDocumentLoad();
          }
          break;
        }
      }
    }
  }

  /**
   * Intentionally left visible to the user
   */
  class TimerJob extends Job {
    public TimerJob() {
      super(SSEUIMessages.LoadingReferencedGrammars);
    }

    public IStatus run(IProgressMonitor monitor) {
      monitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      invokeCMDocumentLoad();
      monitor.done();
      return Status.OK_STATUS;
    }
  }

  private Job timer = new TimerJob();
  protected Document fDocument;
  protected boolean isGrammarInferenceEnabled;
  /**
   * If true, DOMObserver is currently disabled and not loading the content model
   */
  private boolean fIsDisabled = false;
  /**
   * If true, DOMObserver is currently trying to load the content model
   */
  private boolean fIsLoading = false;

  public DOMObserver(IStructuredModel model) {
    fDocument = (model instanceof IDOMModel) ? ((IDOMModel) model).getDocument() : null;

    if (fDocument != null) {
      // here we create and init an adapter that will listen to
      // changes to the document and contained elements
      Collection adapters = ((INodeNotifier) fDocument).getAdapters();
      Iterator iterator = adapters.iterator();
      while (iterator.hasNext()) {
        INodeAdapter adapter = (INodeAdapter) iterator.next();
        if (adapter instanceof MyDocumentAdapter) {
          return;
        }
      }
      MyDocumentAdapter adapter = new MyDocumentAdapter();
      adapter.connect(fDocument);

      ModelQuery modelQuery = ModelQueryUtil.getModelQuery(fDocument);
      if ((modelQuery != null) && (modelQuery.getCMDocumentManager() != null)) {
        CMDocumentManager cmDocumentManager = modelQuery.getCMDocumentManager();
        cmDocumentManager.setPropertyEnabled(CMDocumentManager.PROPERTY_AUTO_LOAD, false);
      }

      // attach a dom observer adapter to the document so others have access to
      // domobserver if needed
      INodeAdapter domObserverAdapter = ((INodeNotifier) fDocument).getExistingAdapter(DOMObserverAdapter.class);
      if (domObserverAdapter == null) {
        domObserverAdapter = new DOMObserverAdapter();
        ((INodeNotifier) fDocument).addAdapter(domObserverAdapter);
      }
      ((DOMObserverAdapter) domObserverAdapter).setDOMObserver(this);
    }
  }

  public void init() {
    // CS: we seem to expose an XSD initialization problem when we do this
    // immediately
    // very nasty... I need to revist this problem with Ed Merks
    //
    timer.schedule();
  }

  public void invokeCMDocumentLoad() {
    if (fIsDisabled)
      return;
    try {
      fIsLoading = true;

      ModelQuery modelQuery = ModelQueryUtil.getModelQuery(fDocument);
      if ((modelQuery != null) && (modelQuery.getCMDocumentManager() != null)) {
        CMDocumentLoader loader = isGrammarInferenceEnabled
            ? new InferredGrammarBuildingCMDocumentLoader(fDocument, modelQuery)
            : new CMDocumentLoader(fDocument, modelQuery);
        loader.loadCMDocuments();
      }
    } finally {
      fIsLoading = false;
    }
  }

  public void invokeDelayedCMDocumentLoad() {
    if (fIsDisabled)
      return;
    timer.schedule(2000);
  }

  public void setGrammarInferenceEnabled(boolean isEnabled) {
    isGrammarInferenceEnabled = isEnabled;
  }

  boolean setDisabled(boolean isDisabled, boolean forced) {
    boolean success = true;

    if (fIsDisabled != isDisabled) {
      fIsDisabled = isDisabled;
      if (forced) {
        if (isDisabled)
          success = timer.cancel();
        else
          invokeCMDocumentLoad();
      }
    }
    return success;
  }

  boolean isLoading() {
    return fIsLoading;
  }
}
