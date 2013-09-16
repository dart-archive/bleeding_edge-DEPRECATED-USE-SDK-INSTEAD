/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;

/**
 * @deprecated use internal XMLEditorPluginImageHelper or external SharedXMLEditorPluginImageHelper
 *             instead
 */
public class SourceEditorImageHelper {

  public SourceEditorImageHelper() {
    super();
  }

  public Image createImage(String resource) {
    ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(XMLUIPlugin.ID, resource);
    Image image = desc.createImage();
    JFaceResources.getImageRegistry().put(resource, image);
    return image;
  }

  public Image getImage(String resource) {
    Image image = JFaceResources.getImageRegistry().get(resource);
    if (image == null) {
      image = createImage(resource);
    }
    return image;
  }

}
