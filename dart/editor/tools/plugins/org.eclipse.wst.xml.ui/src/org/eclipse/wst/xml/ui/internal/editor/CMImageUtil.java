/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.editor;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.wst.sse.core.internal.util.JarUtilities;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author nsd
 */
public class CMImageUtil {

  public static String SMALL_ICON_URL = "small-icon"; //$NON-NLS-1$

  public static CMNode getDeclaration(Node node) {
    CMNode decl = null;
    ModelQuery mq = null;
    switch (node.getNodeType()) {
      case Node.ATTRIBUTE_NODE: {
        mq = ModelQueryUtil.getModelQuery(((Attr) node).getOwnerDocument());
        decl = mq.getCMAttributeDeclaration((Attr) node);
      }
        break;
      case Node.ELEMENT_NODE: {
        mq = ModelQueryUtil.getModelQuery(node.getOwnerDocument());
        decl = mq.getCMElementDeclaration((Element) node);
      }
        break;
    }
    return decl;
  }

  public static Image getImage(CMNode cmnode) {
    if (cmnode == null) {
      return null;
    }
    Image image = null;
    // cache CM-specified images with the XML UI plugin
    String imageURLString = (String) cmnode.getProperty(SMALL_ICON_URL);
    if ((imageURLString != null) && (imageURLString.length() > 0)) {
      /* First ensure that the descriptor itself is cached */
      ImageDescriptor imageDescriptor = getImageDescriptor(imageURLString);
      if (imageDescriptor != null) {
        /*
         * Then obtain the image from the registry so that it is both cached and properly disposed
         * of later
         */
        image = getImageRegistry().get(imageURLString);
      }
    }
    return image;
  }

  public static ImageDescriptor getImageDescriptor(CMNode cmnode) {
    if (cmnode == null) {
      return null;
    }
    // cache CM-specified images with the XML UI plugin
    String imageURLString = (String) cmnode.getProperty(SMALL_ICON_URL);
    ImageDescriptor descriptor = null;
    if ((imageURLString != null) && (imageURLString.length() > 0)) {
      descriptor = getImageDescriptor(imageURLString);
    }
    return descriptor;
  }

  private static ImageDescriptor getImageDescriptor(String imageURLString) {
    ImageDescriptor descriptor = getImageRegistry().getDescriptor(imageURLString);
    if (descriptor == null) {
      try {
        URL imageURL = new URL(imageURLString);
        InputStream inputStream = JarUtilities.getInputStream(imageURL);
        try {
          if (inputStream != null) {
            ImageData data = new ImageData(inputStream);
            descriptor = ImageDescriptor.createFromImageData(data);
            getImageRegistry().put(imageURLString, descriptor);
          }
        } catch (SWTException e) {
          /*
           * There was a problem loading image from stream (corrupt, missing, etc.)
           */
          if (inputStream != null)
            inputStream.close();
        }
      } catch (MalformedURLException e) {
        descriptor = null;
      } catch (IOException e) {
        descriptor = null;
      }
    }
    return descriptor;
  }

  private static final ImageRegistry getImageRegistry() {
    return XMLUIPlugin.getInstance().getImageRegistry();
  }

  /**
	 * 
	 */
  private CMImageUtil() {
    super();
  }
}
