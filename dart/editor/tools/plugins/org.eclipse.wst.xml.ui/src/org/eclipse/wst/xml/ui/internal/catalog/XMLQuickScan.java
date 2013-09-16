/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.wst.xml.ui.internal.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 */
public class XMLQuickScan {
  public static String getTargetNamespaceURIForSchema(String uri) {
    String result = null;
    try {
      URL url = new URL(uri);
      InputStream inputStream = url.openStream();
      result = XMLQuickScan.getTargetNamespaceURIForSchema(inputStream);
    } catch (Exception e) {
    }
    return result;
  }

  public static String getTargetNamespaceURIForSchema(InputStream input) {
    TargetNamespaceURIContentHandler handler = new TargetNamespaceURIContentHandler();
    ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(XMLQuickScan.class.getClassLoader());
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setNamespaceAware(true);
      SAXParser parser = factory.newSAXParser();
      parser.parse(new InputSource(input), handler);
    } catch (StopParseException e) {
      // this is a normal exception to stop parsing early,
      // when info is found, so we can safely ignore
    } catch (ParserConfigurationException e) {
      Logger.logException(e);
    } catch (SAXException e) {
      Logger.logException(e);
    } catch (IOException e) {
      Logger.logException(e);
    } finally {
      Thread.currentThread().setContextClassLoader(prevClassLoader);
    }
    return handler.targetNamespaceURI;
  }

  /**
   * This is a special exception that is used to stop parsing when required information is found.
   */
  static class StopParseException extends org.xml.sax.SAXException {
    static final long serialVersionUID = 1L;

    /**
     * Constructor StopParseException.
     */
    StopParseException() {
      super("targetnamespace found, no need to continue the parse");
    }
  }

  static class TargetNamespaceURIContentHandler extends DefaultHandler {
    public String targetNamespaceURI;

    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      if (localName.equals("schema")) //$NON-NLS-1$
      {
        int nAttributes = attributes.getLength();
        for (int i = 0; i < nAttributes; i++) {
          if (attributes.getLocalName(i).equals("targetNamespace")) //$NON-NLS-1$
          {
            targetNamespaceURI = attributes.getValue(i);
            break;
          }
        }
      }
      throw new StopParseException();
    }
  }
}
