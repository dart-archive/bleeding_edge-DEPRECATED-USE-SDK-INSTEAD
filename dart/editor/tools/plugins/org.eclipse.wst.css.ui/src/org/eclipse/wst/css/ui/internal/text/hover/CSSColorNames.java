/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.text.hover;

import com.ibm.icu.util.StringTokenizer;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.css.ui.internal.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * Associates css color names to the appropriate {@link RGB} values. These associations are defined
 * in org.eclipse.wst.css.ui/csscolors/extended-color-mapping.xml
 */
class CSSColorNames {

  class ColorMappingHandler extends DefaultHandler {
    private final String COLOR_ELEM = "color"; //$NON-NLS-1$
    private final String NAME_ATTR = "name"; //$NON-NLS-1$
    private final String RGB_ATTR = "rgb"; //$NON-NLS-1$

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      if (COLOR_ELEM.equals(qName)) {
        final String name = attributes.getValue(NAME_ATTR);
        final String rgb = attributes.getValue(RGB_ATTR);
        if (name != null && rgb != null) {
          final RGB rgbValue = getRGB(rgb);
          if (rgbValue != null) {
            colors.put(name, rgbValue);
          }
        }
      }
    }

    /**
     * Converts an rgb string into an {@link RGB}
     * 
     * @param rgb the color string
     * @return an {@link RGB} if one can be created from the string; otherwise, null
     */
    private RGB getRGB(String rgb) {
      final StringTokenizer tokenizer = new StringTokenizer(rgb, ","); //$NON-NLS-1$
      int[] weights = new int[3];
      for (int i = 0; tokenizer.hasMoreTokens(); i++) {
        if (i > 2) {
          return null;
        }
        try {
          weights[i] = Integer.parseInt(tokenizer.nextToken().trim());
        } catch (NumberFormatException e) {
          return null;
        }
        if (weights[i] > 255 || weights[i] < 0) {
          return null;
        }
      }
      return new RGB(weights[0], weights[1], weights[2]);
    }
  }

  private static CSSColorNames instance;

  private static final Map colors = new HashMap();

  public static synchronized CSSColorNames getInstance() {
    if (instance == null) {
      instance = new CSSColorNames();
    }
    return instance;
  }

  private CSSColorNames() {
    try {
      URL url = CSSUIPlugin.getDefault().getBundle().getResource(
          "csscolors/extended-color-mapping.xml"); //$NON-NLS-1$
      if (url == null) {
        return;
      }
      final XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
      xmlReader.setContentHandler(new ColorMappingHandler());
      xmlReader.parse(new InputSource(url.openStream()));
    } catch (IOException e) {
      Logger.logException(e);
    } catch (SAXException e) {
      Logger.logException(e);
    } catch (ParserConfigurationException e) {
      Logger.logException(e);
    }
  }

  /**
   * Returns the {@link RGB} value associated with this color name.
   * 
   * @param name the color name
   * @return {@link RGB} associated with <code>name</code>, null if it is an unknown name or invalid
   *         RGB value
   */
  public RGB getRGB(String name) {
    return (RGB) colors.get(name);
  }

}
