/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.views.contentoutline;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSDocument;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapterFactory;
import org.w3c.dom.css.CSSRule;

import java.util.ArrayList;

/**
 * A Content provider for a JFace viewer used to display CSS nodes. This content provider does not
 * use adapters.
 */
class JFaceNodeContentProviderCSS implements ITreeContentProvider {

  public JFaceNodeContentProviderCSS() {
    super();
  }

  /**
   * @deprecated
   */
  protected void addElements(Object element, ArrayList v) {

    ICSSNode node;

    if (element instanceof ICSSModel) {
      ICSSModel model = (ICSSModel) element;
      ICSSDocument doc = model.getDocument();
      node = doc.getFirstChild();
    } else if (element instanceof ICSSNode) {
      node = ((ICSSNode) element).getFirstChild();
    } else
      return;

    while (node != null) {
      if (node instanceof CSSRule) {
        v.add(node);
      }

      node = node.getNextSibling();
    }

  }

  /**
   * The visual part that is using this content provider is about to be disposed. Deallocate all
   * allocated SWT resources.
   */
  public void dispose() {
  }

  /**
   * Returns an enumeration containing all child nodes of the given element, which represents a node
   * in a tree. The difference to <code>IStructuredContentProvider.getElements(Object)</code> is as
   * follows: <code>getElements</code> is called to obtain the tree viewer's root elements. Method
   * <code>getChildren</code> is used to obtain the children of a given node in the tree, which can
   * can be a root node, too.
   */
  public Object[] getChildren(Object object) {
    IJFaceNodeAdapter adapter = getAdapter(object);
    if (adapter != null)
      return adapter.getChildren(object);

    return new Object[0];
  }

  /**
   * Returns an enumeration with the elements belonging to the passed element. These elements can be
   * presented as rows in a table, items in a list etc.
   */
  public Object[] getElements(Object object) {
    IJFaceNodeAdapter adapter = getAdapter(object);
    if (adapter != null)
      return adapter.getElements(object);

    return new Object[0];
  }

  /**
   * Returns the parent for the given element. This method can return <code>null</code> indicating
   * that the parent can't be computed. In this case the tree viewer can't expand a given node
   * correctly if requested.
   */
  public Object getParent(Object object) {
    IJFaceNodeAdapter adapter = getAdapter(object);
    if (adapter != null)
      return adapter.getParent(object);

    return null;
  }

  /**
   * Returns <code>true</code> if the given element has children. Otherwise <code>false</code> is
   * returned.
   */
  public boolean hasChildren(Object object) {
    IJFaceNodeAdapter adapter = getAdapter(object);
    if (adapter != null)
      return adapter.hasChildren(object);

    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
   * java.lang.Object, java.lang.Object)
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (oldInput != null && oldInput instanceof IStructuredModel) {
      IJFaceNodeAdapterFactory factory = (IJFaceNodeAdapterFactory) ((IStructuredModel) oldInput).getFactoryRegistry().getFactoryFor(
          IJFaceNodeAdapter.class);
      if (factory != null) {
        factory.removeListener(viewer);
      }
    }
    if (newInput != null && newInput instanceof IStructuredModel) {
      IJFaceNodeAdapterFactory factory = (IJFaceNodeAdapterFactory) ((IStructuredModel) newInput).getFactoryRegistry().getFactoryFor(
          IJFaceNodeAdapter.class);
      if (factory != null) {
        factory.addListener(viewer);
      }
    }
  }

  /**
   * Checks whether the given element is deleted or not.
   * 
   * @deprecated
   */
  public boolean isDeleted(Object element) {
    return false;
  }

  /**
   * Returns the JFace adapter for the specified object.
   * 
   * @param adaptable java.lang.Object The object to get the adapter for
   */
  private IJFaceNodeAdapter getAdapter(Object adaptable) {
    IJFaceNodeAdapter adapter = null;
    if (adaptable instanceof ICSSModel) {
      adaptable = ((ICSSModel) adaptable).getDocument();
    }
    if (adaptable instanceof ICSSNode && adaptable instanceof INodeNotifier) {
      INodeAdapter nodeAdapter = ((INodeNotifier) adaptable).getAdapterFor(IJFaceNodeAdapter.class);
      if (nodeAdapter instanceof IJFaceNodeAdapter)
        adapter = (IJFaceNodeAdapter) nodeAdapter;
    }
    return adapter;
  }
}
