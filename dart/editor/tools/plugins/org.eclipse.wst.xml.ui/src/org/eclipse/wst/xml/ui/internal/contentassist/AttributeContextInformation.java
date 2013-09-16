/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import java.util.HashMap;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.sse.ui.internal.util.Assert;

/**
 * Implementation of IContextInformation. Adds knowledge about the information display string such
 * as required attributes for this context.
 * 
 * @author pavery
 */
public class AttributeContextInformation implements IContextInformation,
    IContextInformationExtension {
  private HashMap fAttr2RangeMap;
  /** The name of the context */
  private String fContextDisplayString;
  /** The image to be displayed */
  private Image fImage;
  /** The information to be displayed */
  private String fInformationDisplayString;
  private int fPosition = -1;

  /**
   * Creates a new context information with an image.
   * 
   * @param image the image to display when presenting the context information
   * @param contextDisplayString the string to be used when presenting the context
   * @param informationDisplayString the string to be displayed when presenting the context
   *          information, may not be <code>null</code>
   */
  public AttributeContextInformation(Image image, String contextDisplayString,
      String informationDisplayString, HashMap attr2RangeMap) {
    Assert.isNotNull(informationDisplayString,
        "illegal argument: informationDisplayString can not be null"); //$NON-NLS-1$

    fImage = image;
    fContextDisplayString = contextDisplayString;
    fInformationDisplayString = informationDisplayString;
    fAttr2RangeMap = attr2RangeMap;
  }

  /**
   * Creates a new context information without an image.
   * 
   * @param contextDisplayString the string to be used when presenting the context
   * @param informationDisplayString the string to be displayed when presenting the context
   *          information
   */
  public AttributeContextInformation(String contextDisplayString, String informationDisplayString,
      HashMap attr2RangeMap) {
    this(null, contextDisplayString, informationDisplayString, attr2RangeMap);
  }

  /**
   * Maps (String -> Position). The attribute name to the Text position.
   */
  public HashMap getAttr2RangeMap() {
    return fAttr2RangeMap;
  }

  /**
   * @see org.eclipse.jface.text.contentassist.IContextInformation#getContextDisplayString()
   */
  public String getContextDisplayString() {
    if (fContextDisplayString != null) {
      return fContextDisplayString;
    }
    return fInformationDisplayString;
  }

  /**
   * @see org.eclipse.jface.text.contentassist.IContextInformationExtension#getContextInformationPosition()
   */
  public int getContextInformationPosition() {
    return fPosition;
  }

  /**
   * @see org.eclipse.jface.text.contentassist.IContextInformation#getImage()
   */
  public Image getImage() {
    return fImage;
  }

  /**
   * @see org.eclipse.jface.text.contentassist.IContextInformation#getInformationDisplayString()
   */
  public String getInformationDisplayString() {
    return fInformationDisplayString;
  }

  public void setContextInformationPosition(int position) {
    fPosition = position;
  }
}
