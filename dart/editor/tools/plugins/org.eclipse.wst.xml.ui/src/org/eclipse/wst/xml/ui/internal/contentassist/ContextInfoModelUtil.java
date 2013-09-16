/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

/**
 * @author pavery
 */
public class ContextInfoModelUtil {
  IStructuredDocument fDocument = null;

  ContextInfoModelUtil(IStructuredDocument doc) {
    fDocument = doc;
  }

  public IStructuredDocument getDocument() {
    return fDocument;
  }

  public ModelQuery getModelQuery() {
    ModelQuery mq = null;

    IStructuredModel xmlModel = null;
    try {
      xmlModel = StructuredModelManager.getModelManager().getExistingModelForRead(getDocument());
      mq = ModelQueryUtil.getModelQuery(xmlModel);
    } finally {
      if (xmlModel != null) {
        xmlModel.releaseFromRead();
      }
    }
    return mq;
  }

  public IDOMNode getXMLNode(int offset) {
    IStructuredModel xmlModel = null;
    IDOMNode xmlNode = null;
    try {
      xmlModel = StructuredModelManager.getModelManager().getExistingModelForRead(getDocument());
      if (xmlModel != null) {
        xmlNode = (IDOMNode) xmlModel.getIndexedRegion(offset);
      }
    } finally {
      if (xmlModel != null)
        xmlModel.releaseFromRead();
    }
    return xmlNode;
  }
}
