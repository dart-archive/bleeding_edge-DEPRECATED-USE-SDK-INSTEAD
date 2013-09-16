/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.provisional;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISourceEditingTextTools;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint.NodeLocation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public interface IDOMSourceEditingTextTools extends ISourceEditingTextTools {
  /**
   * Returns a W3C DOM document
   * 
   * @return Document object or <code>null</code> if corresponding document does not exist
   */
  Document getDOMDocument();

  /**
   * Returns the W3C DOM Node at the given offset
   * 
   * @param offset the offset within the IDocument
   * @return a Node at that location, if one is present
   * @throws BadLocationException for invalid offsets
   */
  Node getNode(int offset) throws BadLocationException;

  /**
   * Returns a NodeLocation object describing the position information of the Node's start and end
   * tags.
   * 
   * @param node
   * @return The NodeLocation for this Node, null for unsupported Node instances.
   */
  NodeLocation getNodeLocation(Node node);

  /**
   * Returns the current server-side page language for the Document of the given Node.
   * 
   * @return The server-side page language for this nodem null for Nodes within unsupported
   *         Documents.
   */
  String getPageLanguage(Node node);

  /**
   * Returns start offset of given Node.
   * 
   * @param node w3c <code>Node</code> object to check
   * @return the start offset or -1 for unsupported Nodes
   */
  int getStartOffset(Node node);
}
