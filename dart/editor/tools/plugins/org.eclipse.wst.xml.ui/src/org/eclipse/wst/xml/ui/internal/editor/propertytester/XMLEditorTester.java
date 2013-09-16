/******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 ******************************************************************************/
package org.eclipse.wst.xml.ui.internal.editor.propertytester;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A property tester that determines if the editor part is or contains (in the case of a
 * {@link org.eclipse.ui.part.MultiPageEditorPart}) the XML editor.
 */
public class XMLEditorTester extends PropertyTester {

  private static final String PROPERTY = "editor"; //$NON-NLS-1$

  /** The XML editor's part ID */
  private static final String EDITOR_ID = "org.eclipse.core.runtime.xml.source"; //$NON-NLS-1$

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if (receiver instanceof IEditorPart && PROPERTY.equals(property)) {
      ITextEditor editor = null;
      if (receiver instanceof ITextEditor)
        editor = (ITextEditor) receiver;
      else
        editor = (ITextEditor) ((IEditorPart) receiver).getAdapter(ITextEditor.class);
      if (editor != null) {
        IEditorSite site = editor.getEditorSite();
        if (site != null) {
          return EDITOR_ID.equals(site.getId());
        }
      }
    }
    return false;
  }

}
