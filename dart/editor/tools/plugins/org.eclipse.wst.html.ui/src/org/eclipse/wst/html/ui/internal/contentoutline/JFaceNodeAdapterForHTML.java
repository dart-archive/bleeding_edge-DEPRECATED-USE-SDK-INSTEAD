/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentoutline;

import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.html.ui.internal.editor.HTMLEditorPluginImageHelper;
import org.eclipse.wst.html.ui.internal.editor.HTMLEditorPluginImages;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeAdapter;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeAdapterFactory;
import org.w3c.dom.Node;

import java.util.Locale;

/**
 * Adapts a DOM node to a JFace viewer.
 */
public class JFaceNodeAdapterForHTML extends JFaceNodeAdapter {

  private Image createHTMLImage(String imageResourceName) {
    return HTMLEditorPluginImageHelper.getInstance().getImage(imageResourceName);
  }

  /**
   * Constructor for JFaceNodeAdapterForHTML.
   * 
   * @param adapterFactory
   */
  public JFaceNodeAdapterForHTML(JFaceNodeAdapterFactory adapterFactory) {
    super(adapterFactory);
  }

  protected Image createImage(Object object) {
    Image image = null;

    Node node = (Node) object;
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      String lowerName = node.getNodeName().toLowerCase(Locale.US);
      if (lowerName.equals("table") || lowerName.endsWith(":table")) //$NON-NLS-1$
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TABLE);
      else if (lowerName.equals("a") || lowerName.endsWith(":a")) //$NON-NLS-1$
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TAG_ANCHOR);
      else if (lowerName.equals("body") || lowerName.endsWith(":body")) //$NON-NLS-1$
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TAG_BODY);
      else if (lowerName.equals("button") || lowerName.endsWith(":button")) //$NON-NLS-1$
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TAG_BUTTON);
      else if (lowerName.equals("font") || lowerName.endsWith(":font")) //$NON-NLS-1$
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TAG_FONT);
      else if (lowerName.equals("form") || lowerName.endsWith(":form")) //$NON-NLS-1$
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TAG_FORM);
      else if (lowerName.equals("html") || lowerName.endsWith(":html")) //$NON-NLS-1$
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TAG_HTML);
      else if (lowerName.equals("img") || lowerName.endsWith(":img")) //$NON-NLS-1$
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TAG_IMAGE);
      else if (lowerName.equals("map") || lowerName.endsWith(":map")) //$NON-NLS-1$
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TAG_IMAGE_MAP);
      else if (lowerName.equals("title") || lowerName.endsWith(":title")) //$NON-NLS-1$
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TAG_TITLE);
      else
        image = createHTMLImage(HTMLEditorPluginImages.IMG_OBJ_TAG);
    }
    if (image == null) {
      image = super.createImage(node);
    }
    return image;
  }
}
