/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.tabletree;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMDescriptionBuilder;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @deprecated No longer used
 */
public class XMLTreeExtension extends TreeExtension {

  public final static String STRUCTURE_PROPERTY = XMLEditorMessages.XMLTreeExtension_0;
  public final static String VALUE_PROPERTY = XMLEditorMessages.XMLTreeExtension_1;

  protected Composite control;
  protected MyCellModifier modifier;
  protected XMLTableTreePropertyDescriptorFactory propertyDescriptorFactory;
  protected CMDescriptionBuilder decriptionBuilder = new CMDescriptionBuilder();
  protected TreeContentHelper treeContentHelper = new TreeContentHelper();

  protected Color f1, f2, b1, b2;
  protected boolean cachedDataIsValid = true;

  public XMLTreeExtension(Tree tree) {
    super(tree);
    control = tree;
    modifier = new MyCellModifier();
    setCellModifier(modifier);
    String[] properties = {STRUCTURE_PROPERTY, VALUE_PROPERTY};
    setColumnProperties(properties);

    /* https://bugs.eclipse.org/bugs/show_bug.cgi?id=185851 - Used to be hardcoded as COLOR_BLACK */
    f1 = tree.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
    Color background = tree.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

    int r = Math.abs(background.getRed() - 125);
    int g = Math.abs(background.getGreen() - 85);
    int b = Math.abs(background.getBlue() - 105);

    f2 = new Color(tree.getDisplay(), r, g, b);
    b1 = tree.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
    b2 = background;

    propertyDescriptorFactory = new XMLTableTreePropertyDescriptorFactory();
  }

  public void dispose() {
    super.dispose();
    f2.dispose();
  }

  public void resetCachedData() {
    cachedDataIsValid = false;
  }

  public void paintItems(GC gc, TreeItem[] items, Rectangle treeBounds) {
    super.paintItems(gc, items, treeBounds);
    cachedDataIsValid = true;
  }

  protected Object[] computeTreeExtensionData(Object object) {
    Color color = f1;
    String string = ""; //$NON-NLS-1$
    if (string.length() == 0) {
      string = (String) modifier.getValue(object, VALUE_PROPERTY);
      color = f1;
    }
    if ((string.length() == 0) && (object instanceof Element)) {
      string = getElementValueHelper((Element) object);
      color = f2;
    }
    Object[] data = new Object[2];
    data[0] = string;
    data[1] = color;
    return data;
  }

  protected void paintItem(GC gc, TreeItem item, Rectangle bounds) {
    super.paintItem(gc, item, bounds);
    Object[] data = computeTreeExtensionData(item.getData());
    if ((data != null) && (data.length == 2)) {
      gc.setClipping(columnPosition, bounds.y + 1, controlWidth, bounds.height);
      gc.setForeground((Color) data[1]);
      gc.drawString((String) data[0], columnPosition + 5, bounds.y + 1);
      gc.setClipping((Rectangle) null);
    }
  }

  protected void addEmptyTreeMessage(GC gc) {
    // here we print a message when the document is empty just to give the
    // user a visual cue
    // so that they know how to proceed to edit the blank view
    gc.setForeground(fTree.getDisplay().getSystemColor(SWT.COLOR_BLACK));
    gc.setBackground(fTree.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    gc.drawString(XMLEditorMessages.XMLTreeExtension_3, 10, 10);
    gc.drawString(XMLEditorMessages.XMLTreeExtension_4, 10, 10 + gc.getFontMetrics().getHeight());
  }

  public String getElementValueHelper(Element element) {
    String result = null;

    ModelQuery mq = ModelQueryUtil.getModelQuery(element.getOwnerDocument());
    if ((result == null) && (mq != null)) {
      CMElementDeclaration ed = mq.getCMElementDeclaration(element);
      if ((ed != null) && !Boolean.TRUE.equals(ed.getProperty("isInferred"))) { //$NON-NLS-1$
        result = decriptionBuilder.buildDescription(ed);
      }
    }
    return result != null ? result : ""; //$NON-NLS-1$
  }

  /**
	 * 
	 */
  public class MyCellModifier implements ICellModifier, TreeExtension.ICellEditorProvider {
    public boolean canModify(Object element, String property) {
      boolean result = false;
      if (element instanceof Node) {
        Node node = (Node) element;
        result = (property == VALUE_PROPERTY) && treeContentHelper.isEditable(node);
      }
      return result;
    }

    public Object getValue(Object object, String property) {
      String result = null;
      if (object instanceof Node) {
        result = treeContentHelper.getNodeValue((Node) object);
      }
      return (result != null) ? result : ""; //$NON-NLS-1$
    }

    public void modify(Object element, String property, Object value) {
      // enableNodeSelectionListener(false);
      Item item = (Item) element;
      String oldValue = treeContentHelper.getNodeValue((Node) item.getData());
      String newValue = value.toString();
      if ((newValue != null) && !newValue.equals(oldValue)) {
        treeContentHelper.setNodeValue((Node) item.getData(), value.toString());
      }
      // enableNodeSelectionListener(true);
    }

    public CellEditor getCellEditor(Object o, int col) {
      IPropertyDescriptor pd = propertyDescriptorFactory.createPropertyDescriptor(o);
      return pd != null ? pd.createPropertyEditor(control) : null;
    }
  }
}
