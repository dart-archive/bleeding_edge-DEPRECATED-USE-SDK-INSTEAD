/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.common.uriresolver.internal.util.URIHelper;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLSchemaValidationChecker {
  public boolean isValid(IFile ifile) {
    String xsdFileName = ifile.getLocation().toString();
    return isValid(xsdFileName);
  }

  /**
   * Should this be implemented as a Validator and simply called as such with a reporter that only
   * checks the results for severity = error? Or should the Xerces requirement be broken using a
   * plug-in extension?
   */

  public boolean isValid(String xsdFileName) {
    // DOMASBuilderImpl builder = new DOMASBuilderImpl();
    // DOMErrorHandler errorHandler = new DOMErrorHandler();
    // builder.setErrorHandler(errorHandler);
    try {
      // String uri =
      URIHelper.getURIForFilePath(xsdFileName);
      // ASModel model = builder.parseASURI(uri);
      // if (errorHandler.hasError())
      // return false;
    } catch (Exception e) // invalid schema
    {
      return false;
    }

    return true;
  }

  // inner class
  class DOMErrorHandler implements ErrorHandler {
    private boolean hasError = false;

    public boolean hasError() {
      return hasError;
    }

    public void error(SAXParseException err) {
      hasError = true;
    }

    public void fatalError(SAXParseException exception) throws SAXException {
      hasError = true;
    }

    public void warning(SAXParseException exception) throws SAXException {
      // not an error
    }
  } // end DOMErrorHandlerImpl

}
