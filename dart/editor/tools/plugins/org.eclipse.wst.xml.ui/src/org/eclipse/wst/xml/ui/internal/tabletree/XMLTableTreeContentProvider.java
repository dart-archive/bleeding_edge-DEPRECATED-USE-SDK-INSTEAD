/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.tabletree;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapterFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManagerListener;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMDescriptionBuilder;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMDocumentCache;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.util.SharedXMLEditorPluginImageHelper;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

public class XMLTableTreeContentProvider implements ITreeContentProvider, ITableLabelProvider,
    ITableColorProvider, ILabelProvider, CMDocumentManagerListener {

  protected CMDocumentManager documentManager;

  StructuredViewer fViewer = null;

  private TreeContentHelper treeContentHelper = new TreeContentHelper();

  private CMDescriptionBuilder descriptionBuilder = new CMDescriptionBuilder();

  private Color fCMColor = null;

  public XMLTableTreeContentProvider() {
    super();
  }

  //
  // ILabelProvider stuff
  //
  public void addListener(ILabelProviderListener listener) {
    // since we always return 'false' for "isLabelProperty",
    // not need to listen. Maybe that should change some day?
  }

  // CMDocumentManagerListener
  //
  public void cacheCleared(CMDocumentCache cache) {
    doDelayedRefreshForViewers();
  }

  public void cacheUpdated(CMDocumentCache cache, final String uri, int oldStatus, int newStatus,
      CMDocument cmDocument) {
    if ((newStatus == CMDocumentCache.STATUS_LOADED) || (newStatus == CMDocumentCache.STATUS_ERROR)) {
      doDelayedRefreshForViewers();
    }
  }

  public void dispose() {
    if (documentManager != null) {
      documentManager.removeListener(this);
    }
    if (fViewer != null && fViewer.getInput() != null) {
      if (fViewer.getInput() instanceof IDOMNode) {
        IJFaceNodeAdapterFactory factory = (IJFaceNodeAdapterFactory) ((IDOMNode) fViewer.getInput()).getModel().getFactoryRegistry().getFactoryFor(
            IJFaceNodeAdapter.class);
        if (factory != null) {
          factory.removeListener(fViewer);
        }
      }
    }
    if (fCMColor != null) {
      fCMColor.dispose();
    }
  }

  private void doDelayedRefreshForViewers() {
    if ((fViewer != null) && !fViewer.getControl().isDisposed()) {
      fViewer.getControl().getDisplay().asyncExec(new Runnable() {
        public void run() {
          if ((fViewer != null) && !fViewer.getControl().isDisposed()) {
            fViewer.refresh(true);
          }
        }
      });
    }
  }

  public Object[] getChildren(Object element) {
    if (element instanceof INodeNotifier) {
      ((INodeNotifier) element).getAdapterFor(IJFaceNodeAdapter.class);
    }
    return treeContentHelper.getChildren(element);
  }

  public Image getColumnImage(Object object, int columnIndex) {
    return (columnIndex == 0) ? getImage(object) : null;
  }

  //
  // ITableLabelProvider stuff
  //
  public String getColumnText(Object object, int column) {
    String result = null;
    if (column == 0) {
      result = getText(object);
    } else if ((column == 1) && (object instanceof Node)) {
      result = treeContentHelper.getNodeValue((Node) object);
      if (result == null)
        result = getElementValueHelper((Element) object);

    }
    result = TextProcessor.process(result);
    return result != null ? result : ""; //$NON-NLS-1$
  }

  private String getElementValueHelper(Element element) {
    String result = null;

    ModelQuery mq = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
    if ((result == null) && (mq != null)) {
      CMElementDeclaration ed = mq.getCMElementDeclaration(element);
      if ((ed != null) && !Boolean.TRUE.equals(ed.getProperty("isInferred"))) { //$NON-NLS-1$
        result = descriptionBuilder.buildDescription(ed);
      }
    }
    return result != null ? result : ""; //$NON-NLS-1$
  }

  public Object[] getElements(Object element) {
    return getChildren(element);
  }

  public Image getImage(Object object) {
    if (object instanceof INodeNotifier) {
      ((INodeNotifier) object).getAdapterFor(IJFaceNodeAdapter.class);
    }

    Image image = null;
    if (object instanceof Node) {
      Node node = (Node) object;
      switch (node.getNodeType()) {
        case Node.ATTRIBUTE_NODE: {
          image = SharedXMLEditorPluginImageHelper.getImage(SharedXMLEditorPluginImageHelper.IMG_OBJ_ATTRIBUTE);
          break;
        }
        case Node.CDATA_SECTION_NODE: {
          image = SharedXMLEditorPluginImageHelper.getImage(SharedXMLEditorPluginImageHelper.IMG_OBJ_CDATASECTION);
          break;
        }
        case Node.COMMENT_NODE: {
          image = SharedXMLEditorPluginImageHelper.getImage(SharedXMLEditorPluginImageHelper.IMG_OBJ_COMMENT);
          break;
        }
        case Node.DOCUMENT_TYPE_NODE: {
          image = SharedXMLEditorPluginImageHelper.getImage(SharedXMLEditorPluginImageHelper.IMG_OBJ_DOCTYPE);
          break;
        }
        case Node.ELEMENT_NODE: {
          image = SharedXMLEditorPluginImageHelper.getImage(SharedXMLEditorPluginImageHelper.IMG_OBJ_ELEMENT);
          break;
        }
        case Node.PROCESSING_INSTRUCTION_NODE: {
          image = SharedXMLEditorPluginImageHelper.getImage(SharedXMLEditorPluginImageHelper.IMG_OBJ_PROCESSINGINSTRUCTION);
          break;
        }
        case Node.TEXT_NODE: {
          image = SharedXMLEditorPluginImageHelper.getImage(SharedXMLEditorPluginImageHelper.IMG_OBJ_TXTEXT);
          break;
        }
        case Node.ENTITY_REFERENCE_NODE: {
          image = SharedXMLEditorPluginImageHelper.getImage(SharedXMLEditorPluginImageHelper.IMG_OBJ_ENTITY_REFERENCE);
          break;
        }
      }

      // if (image != null) {
      // Image markerOverlayImage =
      // overlayIconManager.getOverlayImageForObject(node);
      // if (markerOverlayImage != null) {
      // image = imageFactory.createCompositeImage(image,
      // markerOverlayImage, ImageFactory.BOTTOM_LEFT);
      // }
      // }
    }
    return image;
  }

  public Object getParent(Object o) {
    if (o instanceof INodeNotifier) {
      ((INodeNotifier) o).getAdapterFor(IJFaceNodeAdapter.class);
    }

    Object result = null;
    if (o instanceof Node) {
      Node node = (Node) o;
      if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
        result = ((Attr) node).getOwnerElement();
      } else {
        result = node.getParentNode();
      }
    }
    return result;
  }

  public Element getRootElement(Document document) {
    Element rootElement = null;

    for (Node childNode = document.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        rootElement = (Element) childNode;
        break;
      }
    }
    return rootElement;
  }

  public String getText(Object object) {
    if (object instanceof INodeNotifier) {
      ((INodeNotifier) object).getAdapterFor(IJFaceNodeAdapter.class);
    }

    String result = null;
    if (object instanceof Node) {
      Node node = (Node) object;
      switch (node.getNodeType()) {
        case Node.ATTRIBUTE_NODE: {
          result = node.getNodeName();
          break;
        }
        case Node.DOCUMENT_TYPE_NODE: {
          result = "DOCTYPE"; //$NON-NLS-1$
          break;
        }
        case Node.ELEMENT_NODE: {
          result = node.getNodeName();
          break;
        }
        case Node.PROCESSING_INSTRUCTION_NODE: {
          result = ((ProcessingInstruction) node).getTarget();
          break;
        }
      }
    }
    result = TextProcessor.process(result);
    return result != null ? result : ""; //$NON-NLS-1$
  }

  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // remove our listeners to the old state
    if (oldInput != null) {
      Document domDoc = (Document) oldInput;
      ModelQuery mq = ModelQueryUtil.getModelQuery(domDoc);
      if (mq != null) {
        documentManager = mq.getCMDocumentManager();
        if (documentManager != null) {
          documentManager.removeListener(this);
        }
      }
    }

    if ((oldInput != null) && (oldInput instanceof IDOMNode)) {
      IJFaceNodeAdapterFactory factory = (IJFaceNodeAdapterFactory) ((IDOMNode) oldInput).getModel().getFactoryRegistry().getFactoryFor(
          IJFaceNodeAdapter.class);
      if (factory != null) {
        factory.removeListener(viewer);
      }
    }

    if (viewer instanceof StructuredViewer) {
      fViewer = null;
    } else {
      fViewer = (StructuredViewer) viewer;
    }

    if ((newInput != null) && (newInput instanceof IDOMNode)) {
      IJFaceNodeAdapterFactory factory = (IJFaceNodeAdapterFactory) ((IDOMNode) newInput).getModel().getFactoryRegistry().getFactoryFor(
          IJFaceNodeAdapter.class);
      if (factory != null) {
        factory.addListener(viewer);
      }
    }

    if (newInput != null) {
      Document domDoc = (Document) newInput;
      ModelQuery mq = ModelQueryUtil.getModelQuery(domDoc);

      if (mq != null) {
        documentManager = mq.getCMDocumentManager();
        if (documentManager != null) {
          documentManager.setPropertyEnabled(CMDocumentManager.PROPERTY_ASYNC_LOAD, true);
          documentManager.addListener(this);
        }
      }
    }
  }

  public boolean isDeleted(Object element) {
    return element != null;
  }

  // the following methods handle filtering aspects of the viewer
  //
  //
  public boolean isIgnorableText(Node node) {
    boolean result = false;
    try {
      if (node.getNodeType() == Node.TEXT_NODE) {
        String data = ((Text) node).getData();
        result = ((data == null) || (data.trim().length() == 0));
      }
    } catch (Exception e) {
      Logger.logException(e);
    }
    return result;
  }

  public boolean isLabelProperty(Object object, String property) {
    return false;
  }

  public void propertyChanged(CMDocumentManager cmDocumentManager, String propertyName) {
    if (cmDocumentManager.getPropertyEnabled(CMDocumentManager.PROPERTY_AUTO_LOAD)) {
      doDelayedRefreshForViewers();
    }
  }

  public void removeListener(ILabelProviderListener listener) {
    // since we always return 'false' for "isLabelProperty",
    // not need to listen. Maybe that should change some day?
  }

  public Color getBackground(Object element, int columnIndex) {
    return null;
  }

  public Color getForeground(Object element, int columnIndex) {
    if (columnIndex == 1 && treeContentHelper.getNodeValue((Node) element) == null)
      return getCMColor();
    return null;
  }

  private Color getCMColor() {
    if (fCMColor == null) {
      Color background = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
      int r = Math.abs(background.getRed() - 125);
      int g = Math.abs(background.getGreen() - 85);
      int b = Math.abs(background.getBlue() - 105);
      fCMColor = new Color(Display.getCurrent(), r, g, b);
    }
    return fCMColor;
  }
}
