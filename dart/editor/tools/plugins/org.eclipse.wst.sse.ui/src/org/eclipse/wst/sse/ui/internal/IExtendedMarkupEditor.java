/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;

/**
 * IExtendedMarkupEditor
 * 
 * @deprecated use the editor's ISourceEditingTextTools adapter and cast to
 *             IDOMSourceEditingTextTools when appropriate
 */
public interface IExtendedMarkupEditor {

  /**
   * @deprecated - use the IDOMSourceEditingTextTools.getNode(int) method with
   *             ISourceEditingTextTools.getCaretOffset()
   * @return
   */
  public Node getCaretNode();

  /**
   * @deprecated - use method on IDOMSourceEditingTextTools
   */
  public Document getDOMDocument();

  /**
   * @deprecated - some editors will provide the list of selected nodes as part of
   *             ISourceEditingTextTools.getSelection()
   */
  public List getSelectedNodes();
}
