/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentoutline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;
import org.eclipse.wst.xml.ui.internal.editor.CMImageUtil;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;
import org.w3c.dom.Node;

/**
 * Adapts a DOM node to a JFace viewer.
 */
public class JFaceNodeAdapter implements IJFaceNodeAdapter {

  final static Class ADAPTER_KEY = IJFaceNodeAdapter.class;

  /**
   * debug .option
   */
  private static final boolean DEBUG = getDebugValue();

  private static boolean getDebugValue() {
    String value = Platform.getDebugOption("org.eclipse.wst.sse.ui/debug/outline"); //$NON-NLS-1$
    boolean result = (value != null) && value.equalsIgnoreCase("true"); //$NON-NLS-1$
    return result;
  }

  JFaceNodeAdapterFactory fAdapterFactory;
  RefreshStructureJob fRefreshJob = null;

  public JFaceNodeAdapter(JFaceNodeAdapterFactory adapterFactory) {
    super();
    this.fAdapterFactory = adapterFactory;
  }

  protected Image createImage(Object object) {
    Image image = null;
    Node node = (Node) object;
    switch (node.getNodeType()) {
      case Node.ELEMENT_NODE: {
        image = createXMLImageDescriptor(XMLEditorPluginImages.IMG_OBJ_ELEMENT);
        break;
      }
      case Node.ATTRIBUTE_NODE: {
        image = createXMLImageDescriptor(XMLEditorPluginImages.IMG_OBJ_ATTRIBUTE);
        break;
      }
      case Node.TEXT_NODE: { // actually, TEXT should never be seen in
        // the tree
        image = createXMLImageDescriptor(XMLEditorPluginImages.IMG_OBJ_TXTEXT);
        break;
      }
      case Node.CDATA_SECTION_NODE: {
        image = createXMLImageDescriptor(XMLEditorPluginImages.IMG_OBJ_CDATASECTION);
        break;
      }
      case Node.ENTITY_REFERENCE_NODE:
      case Node.ENTITY_NODE: {
        image = createXMLImageDescriptor(XMLEditorPluginImages.IMG_OBJ_ENTITY);
        break;
      }
      case Node.PROCESSING_INSTRUCTION_NODE: {
        image = createXMLImageDescriptor(XMLEditorPluginImages.IMG_OBJ_PROCESSINGINSTRUCTION);
        break;
      }
      case Node.COMMENT_NODE: {
        image = createXMLImageDescriptor(XMLEditorPluginImages.IMG_OBJ_COMMENT);
        break;
      }
      case Node.DOCUMENT_TYPE_NODE: {
        image = createXMLImageDescriptor(XMLEditorPluginImages.IMG_OBJ_DOCTYPE);
        break;
      }
      case Node.NOTATION_NODE: {
        image = createXMLImageDescriptor(XMLEditorPluginImages.IMG_OBJ_NOTATION);
        break;
      }
      default: {
        image = createXMLImageDescriptor(XMLEditorPluginImages.IMG_OBJ_TXTEXT);
        break;
      }
    }
    return image;
  }

  protected Image createXMLImageDescriptor(String imageResourceName) {
    return XMLEditorPluginImageHelper.getInstance().getImage(imageResourceName);
  }

  public Object[] getChildren(Object object) {

    // (pa) 20021217
    // cmvc defect 235554
    // performance enhancement: using child.getNextSibling() rather than
    // nodeList(item) for O(n) vs. O(n*n)
    //
    ArrayList v = new ArrayList();
    if (object instanceof Node) {
      Node node = (Node) object;
      for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
        Node n = child;
        if (n.getNodeType() != Node.TEXT_NODE) {
          v.add(n);
        }
      }
    }
    return v.toArray();
  }

  /**
   * Returns an enumeration with the elements belonging to the passed element. These are the top
   * level items in a list, tree, table, etc...
   */
  public Object[] getElements(Object node) {
    return getChildren(node);
  }

  /**
   * Fetches the label image specific to this object instance.
   */
  public Image getLabelImage(Object node) {
    Image image = null;
    if (node instanceof Node) {
      // check for an image from the content model
      image = CMImageUtil.getImage(CMImageUtil.getDeclaration((Node) node));
      if (image == null) {
        /*
         * Create/get image based on Node type. Images are cached transparently in this class,
         * subclasses must do this for themselves if they're going to return their own results.
         */
        image = createImage(node);
      }
    }
    return image;
  }

  /**
   * Fetches the label text specific to this object instance.
   */
  public String getLabelText(Object node) {
    return getNodeName(node);
  }

  private String getNodeName(Object object) {
    StringBuffer nodeName = new StringBuffer();
    if (object instanceof Node) {
      Node node = (Node) object;
      nodeName.append(node.getNodeName());

      if (node.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
        nodeName.insert(0, "DOCTYPE:"); //$NON-NLS-1$
      }

    }
    return nodeName.toString();
  }

  public Object getParent(Object object) {
    if (object instanceof Node) {
      Node node = (Node) object;
      return node.getParentNode();
    }
    return null;
  }

  private synchronized RefreshStructureJob getRefreshJob() {
    if (fRefreshJob == null) {
      fRefreshJob = new RefreshStructureJob();
    }
    return fRefreshJob;
  }

  public boolean hasChildren(Object object) {
    // (pa) 20021217
    // cmvc defect 235554 > use child.getNextSibling() instead of
    // nodeList(item) for O(n) vs. O(n*n)
    Node node = (Node) object;
    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child.getNodeType() != Node.TEXT_NODE) {
        return true;
      }
    }
    return false;
  }

  /**
   * Allowing the INodeAdapter to compare itself against the type allows it to return true in more
   * than one case.
   */
  public boolean isAdapterForType(Object type) {
    if (type == null) {
      return false;
    }
    return type.equals(ADAPTER_KEY);
  }

  /**
   * Called by the object being adapter (the notifier) when something has changed.
   */
  public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature,
      Object oldValue, Object newValue, int pos) {
    // future_TODO: the 'uijobs' used in this method were added to solve
    // threading problems when the dom
    // is updated in the background while the editor is open. They may be
    // a bit overkill and not that useful.
    // (That is, may be be worthy of job manager management). If they are
    // found to be important enough to leave in,
    // there's probably some optimization that can be done.
    if (notifier instanceof Node) {
      Collection listeners = fAdapterFactory.getListeners();
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
                || (eventType == INodeNotifier.CONTENT_CHANGED) || (eventType == INodeNotifier.CHANGE))) {
          if (DEBUG) {
            System.out.println("JFaceNodeAdapter notified on event type > " + eventType); //$NON-NLS-1$
          }

          // refresh on structural and "unknown" changes
          StructuredViewer structuredViewer = (StructuredViewer) listener;
          // https://w3.opensource.ibm.com/bugzilla/show_bug.cgi?id=5230
          if (structuredViewer.getControl() != null) {
            getRefreshJob().refresh(structuredViewer, (Node) notifier);
          }
        }
      }
    }
  }
}
