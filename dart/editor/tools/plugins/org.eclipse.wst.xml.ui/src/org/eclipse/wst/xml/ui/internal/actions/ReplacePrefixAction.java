/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.actions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMVisitor;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ReplacePrefixAction extends NodeAction {

  class NodeCollectingDOMVisitor extends DOMVisitor {
    public List list = new Vector();

    protected boolean isPrefixChangedNeeded(Node node) {
      String key = node.getPrefix() != null ? node.getPrefix() : ""; //$NON-NLS-1$
      return prefixMapping.get(key) != null;
    }

    public void visitAttr(Attr attr) {
      /*
       * if (isPrefixChangedNeeded(element)) { list.add(attr); }
       */
    }

    protected void visitElement(Element element) {
      super.visitElement(element);
      if (isPrefixChangedNeeded(element)) {
        list.add(element);
      }
    }
  }

  protected static ImageDescriptor imageDescriptor;
  protected Element element;
  protected AbstractNodeActionManager manager;
  protected Map prefixMapping;

  public ReplacePrefixAction(AbstractNodeActionManager manager, Element element, Map prefixMapping) {
    this.manager = manager;
    this.element = element;
    this.prefixMapping = prefixMapping;
  }

  public String getUndoDescription() {
    return ""; //$NON-NLS-1$
  }

  public void run() {
    Shell shell = XMLUIPlugin.getInstance().getWorkbench().getActiveWorkbenchWindow().getShell();
    if (validateEdit(manager.getModel(), shell)) {
      NodeCollectingDOMVisitor visitor = new NodeCollectingDOMVisitor();
      visitor.visitNode(element);
      for (Iterator i = visitor.list.iterator(); i.hasNext();) {
        Node node = (Node) i.next();
        String key = node.getPrefix() != null ? node.getPrefix() : ""; //$NON-NLS-1$
        String newPrefix = (String) prefixMapping.get(key);
        if (newPrefix != null) {
          node.setPrefix(newPrefix);
        }
      }
    }
  }
}
