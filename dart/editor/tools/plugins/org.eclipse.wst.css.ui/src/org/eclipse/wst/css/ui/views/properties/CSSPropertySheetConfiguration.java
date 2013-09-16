/*****************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 ****************************************************************************/

package org.eclipse.wst.css.ui.views.properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.ui.internal.properties.CSSPropertySource;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.ui.views.properties.PropertySheetConfiguration;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration for property sheet page which shows CSS content.
 * 
 * @see org.eclipse.wst.sse.ui.views.properties.PropertySheetConfiguration
 * @since 1.0
 */
public class CSSPropertySheetConfiguration extends PropertySheetConfiguration {
  private class CSSPropertySheetRefreshAdapter implements INodeAdapter {
    public boolean isAdapterForType(Object type) {
      return false;
    }

    public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature,
        Object oldValue, Object newValue, int pos) {
      if (fPropertySheetPage != null) {
        getPropertiesRefreshJob().addPropertySheetPage(fPropertySheetPage);
      }
    }
  }

  private class CSSPropertySourceProvider implements IPropertySourceProvider {
    private IPropertySource fPropertySource = null;
    private ICSSNode fSource = null;

    public IPropertySource getPropertySource(Object object) {
      if (fSource != null && object.equals(fSource)) {
        return fPropertySource;
      }

      if (object instanceof ICSSNode) {
        fSource = (ICSSNode) object;
        fPropertySource = new CSSPropertySource(fSource);
      } else {
        fSource = null;
        fPropertySource = null;
      }
      return fPropertySource;
    }
  }

  private class PropertiesRefreshJob extends UIJob {
    public static final int UPDATE_DELAY = 200;

    private Set propertySheetPages = null;

    public PropertiesRefreshJob() {
      super(XMLUIMessages.JFaceNodeAdapter_1);
      setSystem(true);
      setPriority(Job.SHORT);
      propertySheetPages = new HashSet(1);
    }

    void addPropertySheetPage(IPropertySheetPage page) {
      propertySheetPages.add(page);
      schedule(UPDATE_DELAY);
    }

    public IStatus runInUIThread(IProgressMonitor monitor) {
      Object[] pages = propertySheetPages.toArray();
      propertySheetPages.clear();

      for (int i = 0; i < pages.length; i++) {
        PropertySheetPage page = (PropertySheetPage) pages[i];
        if (page.getControl() != null && !page.getControl().isDisposed()) {
          page.refresh();
        }
      }

      return Status.OK_STATUS;
    }
  }

  private PropertiesRefreshJob fPropertiesRefreshJob = null;

  IPropertySheetPage fPropertySheetPage;

  private IPropertySourceProvider fPropertySourceProvider = null;

  private INodeAdapter fRefreshAdapter = new CSSPropertySheetRefreshAdapter();

  private INodeNotifier[] fSelectedNotifiers;

  /**
   * Create new instance of CSSPropertySheetConfiguration
   */
  public CSSPropertySheetConfiguration() {
    // Must have empty constructor to createExecutableExtension
    super();
  }

  public ISelection getInputSelection(IWorkbenchPart selectingPart, ISelection selection) {
    // remove UI refresh adapters
    if (fSelectedNotifiers != null) {
      for (int i = 0; i < fSelectedNotifiers.length; i++) {
        fSelectedNotifiers[i].removeAdapter(fRefreshAdapter);
      }
      fSelectedNotifiers = null;
    }

    ISelection preferredSelection = super.getInputSelection(selectingPart, selection);
    if (preferredSelection instanceof IStructuredSelection) {
      Object[] selectedObjects = new Object[((IStructuredSelection) selection).size()];
      System.arraycopy(((IStructuredSelection) selection).toArray(), 0, selectedObjects, 0,
          selectedObjects.length);
      for (int i = 0; i < selectedObjects.length; i++) {
        if (selectedObjects[i] instanceof ICSSNode) {
          ICSSNode node = (ICSSNode) selectedObjects[i];
          while (node.getNodeType() == ICSSNode.PRIMITIVEVALUE_NODE
              || node.getNodeType() == ICSSNode.STYLEDECLITEM_NODE) {
            node = node.getParentNode();
            selectedObjects[i] = node;
          }
        }
      }

      /*
       * Add UI refresh adapters and remember notifiers for later removal
       */
      if (selectedObjects.length > 0) {
        List selectedNotifiers = new ArrayList(1);
        for (int i = 0; i < selectedObjects.length; i++) {
          if (selectedObjects[i] instanceof INodeNotifier) {
            selectedNotifiers.add(selectedObjects[i]);
            ((INodeNotifier) selectedObjects[i]).addAdapter(fRefreshAdapter);
          }
        }
        fSelectedNotifiers = (INodeNotifier[]) selectedNotifiers.toArray(new INodeNotifier[selectedNotifiers.size()]);
      }
      preferredSelection = new StructuredSelection(selectedObjects);
    }
    return preferredSelection;
  }

  PropertiesRefreshJob getPropertiesRefreshJob() {
    if (fPropertiesRefreshJob == null) {
      fPropertiesRefreshJob = new PropertiesRefreshJob();
    }
    return fPropertiesRefreshJob;
  }

  public IPropertySourceProvider getPropertySourceProvider(IPropertySheetPage page) {
    if (fPropertySourceProvider == null) {
      fPropertySourceProvider = new CSSPropertySourceProvider();
      fPropertySheetPage = page;
    }
    return fPropertySourceProvider;
  }

  public void unconfigure() {
    super.unconfigure();
    if (fSelectedNotifiers != null) {
      for (int i = 0; i < fSelectedNotifiers.length; i++) {
        fSelectedNotifiers[i].removeAdapter(fRefreshAdapter);
      }
      fSelectedNotifiers = null;
    }
  }
}
