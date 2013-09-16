/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentoutline;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.wst.css.core.internal.document.CSSStructuredDocumentRegionContainer;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSDocument;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSMediaRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSPrimitiveValue;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleDeclItem;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleDeclaration;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleSheet;
import org.eclipse.wst.css.ui.internal.image.CSSImageHelper;
import org.eclipse.wst.css.ui.internal.image.CSSImageType;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapterFactory;
import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.stylesheets.MediaList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Adapts the CSS DOM node to a JFace viewer.
 */
class CSSNodeAdapter implements IJFaceNodeAdapter, INodeAdapter, Runnable {
  /**
   * debug .option
   */
  private static final boolean DEBUG = getDebugValue();

  private static boolean getDebugValue() {
    String value = Platform.getDebugOption("org.eclipse.wst.sse.ui/debug/outline"); //$NON-NLS-1$
    boolean result = (value != null) && value.equalsIgnoreCase("true"); //$NON-NLS-1$
    return result;
  }

  class NotifyContext {
    NotifyContext(INodeNotifier notifier, int eventType, Object changedFeature, Object oldValue,
        Object newValue, int pos) {
      this.notifier = notifier;
      this.eventType = eventType;
      this.changedFeature = changedFeature;
      this.oldValue = oldValue;
      this.newValue = newValue;
      this.pos = pos;
    }

    void fire() {
      internalNotifyChanged(notifier, eventType, changedFeature, oldValue, newValue, pos);
    }

    INodeNotifier notifier;
    int eventType;
    Object changedFeature;
    Object oldValue;
    Object newValue;
    int pos;
  }

  class StyleViewUpdater implements Runnable {
    public void run() {
      if (lastUpdater == this) {
        internalActionPerformed();
        lastUpdater = null;
      }
    }
  }

  protected INodeAdapterFactory adapterFactory;
  private Vector notifyQueue;
  StyleViewUpdater lastUpdater;
  protected int delayMSecs = 500;
  RefreshStructureJob fRefreshJob = null;
  final static Class ADAPTER_KEY = IJFaceNodeAdapter.class;

  public CSSNodeAdapter(INodeAdapterFactory adapterFactory) {
    super();
    this.adapterFactory = adapterFactory;
  }

  private synchronized RefreshStructureJob getRefreshJob() {
    if (fRefreshJob == null) {
      fRefreshJob = new RefreshStructureJob();
    }
    return fRefreshJob;
  }

  /**
   * Insert the method's description here.
   */
  protected void internalActionPerformed() {
    if (notifyQueue == null) {
      return;
    }
    boolean refresh_all = false;
    boolean refresh_rule = false;
    int pos_all = 0;
    List targets = new ArrayList();
    for (int i = 0; i < notifyQueue.size(); i++) {
      NotifyContext context = (NotifyContext) notifyQueue.get(i);
      if (context.notifier instanceof ICSSStyleSheet) {
        refresh_all = true;
        pos_all = i;
      }
      if (context.notifier instanceof ICSSStyleDeclaration) {
        refresh_rule = true;
        targets.add(context);
        // pos_rule = i;
      }
      // ((NotifyContext) notifyQueue.get(i)).fire();
    }
    if (refresh_all) {
      ((NotifyContext) notifyQueue.get(pos_all)).fire();
    } else if (refresh_rule) {
      Iterator i = targets.iterator();
      while (i.hasNext()) {
        ((NotifyContext) i.next()).fire();
      }
      // else if (refresh_rule) internalRefreshAll();
    } else {
      for (int i = 0; i < notifyQueue.size(); i++) {
        ((NotifyContext) notifyQueue.get(i)).fire();
      }
    }
    notifyQueue.clear();
  }

  /**
   * Called by the object being adapter (the notifier) when something has changed.
   */
  public void internalNotifyChanged(INodeNotifier notifier, int eventType, Object changedFeature,
      Object oldValue, Object newValue, int pos) {
    Iterator iterator = ((IJFaceNodeAdapterFactory) adapterFactory).getListeners().iterator();
    while (iterator.hasNext()) {
      Object listener = iterator.next();
      if (listener instanceof StructuredViewer) {
        notifyChangedForStructuredViewer((StructuredViewer) listener, notifier, eventType,
            changedFeature, oldValue, newValue, pos);
      } else if (listener instanceof PropertySheetPage) {
        notifyChangedForPropertySheetPage((PropertySheetPage) listener, notifier, eventType,
            changedFeature, oldValue, newValue, pos);
      }
    }
  }

  private void notifyChangedForPropertySheetPage(PropertySheetPage page, INodeNotifier notifier,
      int eventType, Object changedFeature, Object oldValue, Object newValue, int pos) {
    if (page.getControl() == null || page.getControl().isDisposed()) {
      return;
    }
    if (eventType == INodeNotifier.CHANGE || eventType == INodeNotifier.ADD
        || eventType == INodeNotifier.REMOVE) {
      page.refresh();
    }
  }

  private void notifyChangedForStructuredViewer(StructuredViewer viewer, INodeNotifier notifier,
      int eventType, Object changedFeature, Object oldValue, Object newValue, int pos) {
    if (viewer.getControl() == null || viewer.getControl().isDisposed()) {
      return;
    }
    if (eventType == INodeNotifier.CHANGE) {
      if (notifier instanceof ICSSStyleSheet) {
        ICSSNode temp = (changedFeature != null) ? (ICSSNode) changedFeature : (ICSSNode) newValue;
        if (temp instanceof ICSSStyleRule) {
          viewer.refresh();
        } else {
          for (;;) {
            if (temp instanceof ICSSStyleRule) {
              break;
            }
            temp = temp.getParentNode();
            if (temp == null) {
              break;
            }
          }
          if (temp == null || temp instanceof ICSSStyleSheet) {
            viewer.refresh();
          } else {
            viewer.refresh(temp);
          }
        }
      } else {
        ICSSNode temp = (ICSSNode) notifier;
        if (temp != null) {
          temp = temp.getParentNode();
        }
        if (temp == null || temp instanceof ICSSStyleSheet) {
          viewer.refresh();
        } else {
          viewer.refresh(temp);
        }
      }
    }
    if (eventType == INodeNotifier.ADD) {
      if (notifier instanceof ICSSStyleSheet) {
        ICSSNode temp = (changedFeature != null) ? (ICSSNode) changedFeature : (ICSSNode) newValue;
        if (temp instanceof ICSSStyleRule) {
          viewer.refresh();
        } else {
          for (;;) {
            if (temp instanceof ICSSStyleRule) {
              break;
            }
            temp = temp.getParentNode();
            if (temp == null) {
              break;
            }
          }
        }
        if (temp == null || (temp instanceof ICSSStyleSheet)) {
          viewer.refresh();
        } else {
          viewer.refresh(temp);
        }
      } else {
        if (newValue != null && (newValue instanceof ICSSStyleDeclItem)) {
          viewer.refresh(((ICSSNode) newValue).getParentNode());
        } else {
          ICSSNode temp = (ICSSNode) notifier;
          if (temp != null) {
            temp = temp.getParentNode();
          }
          if (temp == null || (temp instanceof ICSSStyleSheet)) {
            viewer.refresh();
          } else {
            viewer.refresh(temp);
          }
        }
      }
    } else if (eventType == INodeNotifier.REMOVE) {
      if (notifier instanceof ICSSStyleSheet) {
        ICSSNode temp = (changedFeature != null) ? (ICSSNode) changedFeature : (ICSSNode) newValue;
        if (temp instanceof ICSSStyleRule) {
          viewer.refresh();
        } else {
          for (;;) {
            if (temp instanceof ICSSStyleRule) {
              break;
            }
            temp = temp.getParentNode();
            if (temp == null) {
              break;
            }
          }
          if (temp == null || (temp instanceof ICSSStyleSheet)) {
            viewer.refresh();
          } else {
            viewer.refresh(temp);
          }
        }
      } else {
        // viewer.refresh(notifier);
        ICSSNode temp = (ICSSNode) notifier;
        if (temp != null) {
          temp = temp.getParentNode();
        }
        if (temp == null || (temp instanceof ICSSStyleSheet)) {
          viewer.refresh();
        } else {
          viewer.refresh(temp);
        }
      }
    }
    // }
  }

  /**
	 * 
	 */
  public void internalRefreshAll() {
    Collection listeners = ((JFaceNodeAdapterFactoryCSS) adapterFactory).getListeners();
    Iterator iterator = listeners.iterator();
    while (iterator.hasNext()) {
      Object listener = iterator.next();
      if (listener instanceof StructuredViewer) {
        StructuredViewer viewer = (StructuredViewer) listener;
        if (viewer.getControl() != null && !viewer.getControl().isDisposed()) {
          viewer.refresh();
        }
      } else if (listener instanceof PropertySheetPage) {
        PropertySheetPage page = (PropertySheetPage) listener;
        if (page.getControl() != null && !page.getControl().isDisposed()) {
          page.refresh();
        }
      }
    }
  }

  /**
   * Allowing the INodeAdapter to compare itself against the type allows it to return true in more
   * than one case.
   */
  public boolean isAdapterForType(Object type) {
    return type.equals(ADAPTER_KEY);
  }

  /**
   * Called by the object being adapter (the notifier) when something has changed.
   */
  public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature,
      Object oldValue, Object newValue, int pos) {
    if (notifyQueue == null)
      notifyQueue = new Vector();
//		notifyQueue.add(new NotifyContext(notifier, eventType, changedFeature, oldValue, newValue, pos));
    // TODO-future: there's probably a better way than relying on async
    // exec
    if (notifier instanceof ICSSNode) {
      Collection listeners = ((JFaceNodeAdapterFactoryCSS) adapterFactory).getListeners();
      Iterator iterator = listeners.iterator();

      while (iterator.hasNext()) {
        Object listener = iterator.next();
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=90637
        // if (notifier instanceof Node && (listener instanceof
        // StructuredViewer) && (eventType ==
        // INodeNotifier.STRUCTURE_CHANGED || (eventType ==
        // INodeNotifier.CHANGE && changedFeature == null))) {
        if ((listener instanceof StructuredViewer)
            && ((eventType == INodeNotifier.STRUCTURE_CHANGED)
                || (eventType == INodeNotifier.CONTENT_CHANGED) || (eventType == INodeNotifier.CHANGE
                || (eventType == INodeNotifier.ADD) || (eventType == INodeNotifier.REMOVE)))) {
          if (DEBUG) {
            System.out.println("JFaceNodeAdapter notified on event type > " + eventType); //$NON-NLS-1$
          }

          // refresh on structural and "unknown" changes
          StructuredViewer structuredViewer = (StructuredViewer) listener;
          // https://w3.opensource.ibm.com/bugzilla/show_bug.cgi?id=5230
          if (structuredViewer.getControl() != null) {
            getRefreshJob().refresh(structuredViewer, (ICSSNode) notifier);
          }
        }
      }
    }
  }

  Display getDisplay() {
    return PlatformUI.getWorkbench().getDisplay();
  }

  /**
   * this method is intended only for timerExec()
   */
  public void run() {
    lastUpdater = new StyleViewUpdater();
    getDisplay().asyncExec(lastUpdater);
  }

  /**
   * Returns an enumeration containing all child nodes of the given element, which represents a node
   * in a tree. The difference to <code>IStructuredContentProvider.getElements(Object)</code> is as
   * follows: <code>getElements</code> is called to obtain the tree viewer's root elements. Method
   * <code>getChildren</code> is used to obtain the children of a given node in the tree, which can
   * can be a root node, too.
   */
  public Object[] getChildren(Object object) {
    if (object instanceof ICSSNode) {
      ICSSNode node = (ICSSNode) object;

      short nodeType = node.getNodeType();
      if (nodeType == ICSSNode.STYLERULE_NODE || nodeType == ICSSNode.PAGERULE_NODE
          || nodeType == ICSSNode.FONTFACERULE_NODE) {
        for (node = node.getFirstChild(); node != null && !(node instanceof ICSSStyleDeclaration); node.getNextSibling()) {
          // nop
        }
      }
      List children = new ArrayList();
      ICSSNode child = (node != null) ? node.getFirstChild() : null;
      while (child != null) {
        if (!(child instanceof ICSSPrimitiveValue) && !(child instanceof MediaList)) {
          children.add(child);
        }
        /*
         * Required to correctly connect the refreshing behavior to the tree
         */
        if (child instanceof INodeNotifier) {
          ((INodeNotifier) child).getAdapterFor(IJFaceNodeAdapter.class);
        }
        child = child.getNextSibling();
      }
      return children.toArray();
    }
    return new Object[0];
  }

  /**
   * Returns an enumeration with the elements belonging to the passed element. These elements can be
   * presented as rows in a table, items in a list etc.
   */
  public Object[] getElements(Object object) {
    // The root is usually an instance of an XMLStructuredModel in
    // which case we want to extract the document.

    if (object instanceof ICSSModel) {
      ArrayList v = new ArrayList();
      // internalGetElements(object, v);
      addElements(object, v);
      Object[] elements = v.toArray();

      for (int i = 0; i < elements.length; i++) {
        /*
         * Required to correctly connect the refreshing behavior to the tree
         */
        if (elements[i] instanceof INodeNotifier) {
          ((INodeNotifier) elements[i]).getAdapterFor(IJFaceNodeAdapter.class);
        }
      }

      return elements;
    }
    return new Object[0];

  }

  /**
   * Returns the image for the label of the given element, for use in the given viewer.
   * 
   * @param viewer The viewer that displays the element.
   * @param element The element for which to provide the label image. Element can be
   *          <code>null</code> indicating no input object is set to the viewer.
   */
  public Image getLabelImage(Object element) {
    if (element instanceof ICSSNode) {
      CSSImageHelper helper = CSSImageHelper.getInstance();
      return helper.getImage(CSSImageType.getImageType((ICSSNode) element));
      // Image image = getCSSNodeImage(element);
      // return image;
      // return getAdapter(element).getLabelImage((ICSSNode) element);
    }
    return null;
  }

  /**
   * Returns the text for the label of the given element, for use in the given viewer.
   * 
   * @param viewer The viewer that displays the element.
   * @param element The element for which to provide the label text. Element can be
   *          <code>null</code> indicating no input object is set to the viewer.
   */
  public String getLabelText(Object element) {
    // This was returning null, on occasion ... probably should not be,
    // but
    // took the quick and easy way out for now. (dmw 3/8/01)

    String result = "";//$NON-NLS-1$
    String mediaText;
    if (element instanceof ICSSNode) {
      switch (((ICSSNode) element).getNodeType()) {
        case ICSSNode.STYLERULE_NODE:
          result = ((ICSSStyleRule) element).getSelectors().getString();
          break;
        case ICSSNode.FONTFACERULE_NODE:
          result = "@font-face";//$NON-NLS-1$
          break;
        case ICSSNode.IMPORTRULE_NODE:
          result = ((CSSImportRule) element).getHref();
          mediaText = getMediaText((CSSImportRule) element);
          if (mediaText != null && 0 < mediaText.length()) {
            result += " (" + mediaText + ")"; //$NON-NLS-1$ //$NON-NLS-2$
          }
          break;
        case ICSSNode.PAGERULE_NODE:
          result = "@page";//$NON-NLS-1$
          break;
        case ICSSNode.STYLEDECLARATION_NODE:
          result = "properties";//$NON-NLS-1$
          break;
        case ICSSNode.STYLEDECLITEM_NODE:
          result = ((ICSSStyleDeclItem) element).getPropertyName();
          break;
        case ICSSNode.PRIMITIVEVALUE_NODE:
          result = ((ICSSPrimitiveValue) element).getStringValue();
          break;
        case ICSSNode.MEDIARULE_NODE:
          result = "@media";//$NON-NLS-1$
          mediaText = getMediaText((ICSSMediaRule) element);
          if (mediaText != null && 0 < mediaText.length()) {
            result += " (" + mediaText + ")"; //$NON-NLS-1$ //$NON-NLS-2$
          }
          break;
        case ICSSNode.CHARSETRULE_NODE:
          result = "@charset";//$NON-NLS-1$
          break;
        case ICSSNode.MEDIALIST_NODE:
          result = ((MediaList) element).getMediaText();
          break;
        default:
          break;
      }
    }

    // if (element instanceof ICSSNode) {
    // ICSSNode node = ((ICSSNode)element);
    // result = getAdapter(element).getLabelText((ICSSNode) element);
    // }
    return result;
  }

  /**
   * Returns the parent for the given element. This method can return <code>null</code> indicating
   * that the parent can't be computed. In this case the tree viewer can't expand a given node
   * correctly if requested.
   */
  public Object getParent(Object object) {
    if (object instanceof ICSSNode) {
      ICSSNode node = ((ICSSNode) object).getParentNode();
      if (node != null && node.getNodeType() == ICSSNode.STYLEDECLARATION_NODE) {
        /*
         * Required to also correctly connect style declaration to the refreshing behavior in the
         * tree
         */
        if (node instanceof INodeNotifier) {
          ((INodeNotifier) node).getAdapterFor(IJFaceNodeAdapter.class);
        }
        node = node.getParentNode();
      }
      return node;
    }
    return null;
  }

  /**
   * Returns <code>true</code> if the given element has children. Otherwise <code>false</code> is
   * returned.
   */
  public boolean hasChildren(Object object) {
    // return getAdapter(object).hasChildren((ICSSNode) object);
    if (object instanceof ICSSNode) {
      /*
       * Required to correctly connect the refreshing behavior to the tree
       */
      if (object instanceof INodeNotifier) {
        ((INodeNotifier) object).getAdapterFor(IJFaceNodeAdapter.class);
      }

      if (object instanceof ICSSStyleDeclItem)
        return false;
      else {
        if (((ICSSNode) object).hasChildNodes()) {
          ICSSNode child = ((ICSSNode) object).getFirstChild();

          while (child != null) {
            if (child instanceof CSSStructuredDocumentRegionContainer) {
              String childText = ((CSSStructuredDocumentRegionContainer) child).getCssText();
              if (childText != null && childText.length() > 0)
                return true;
            }
            child = child.getNextSibling();
          }
        }
      }
    }
    return false;
  }

  private void addElements(Object element, ArrayList v) {

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

  private String getMediaText(CSSRule rule) {
    String result = ""; //$NON-NLS-1$
    ICSSNode child = (rule != null) ? ((ICSSNode) rule).getFirstChild() : null;
    while (child != null) {
      if (child.getNodeType() == ICSSNode.MEDIALIST_NODE) {
        result = ((MediaList) child).getMediaText();
        break;
      }
      child = child.getNextSibling();
    }
    return result;
  }
}
