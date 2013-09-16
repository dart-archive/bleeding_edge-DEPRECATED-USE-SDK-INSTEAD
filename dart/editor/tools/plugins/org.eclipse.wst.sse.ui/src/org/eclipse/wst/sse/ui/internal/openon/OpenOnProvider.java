/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.openon;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredPartitioning;

/**
 * Determines the appropriate IOpenOn to call based on current partition.
 * 
 * @deprecated Use base support for hyperlink navigation
 */
public class OpenOnProvider {
  private static OpenOnProvider fInstance;

  /**
   * returns singleton instance of OpenOnProvider
   * 
   * @return OpenOnProvider
   */
  public synchronized static OpenOnProvider getInstance() {
    if (fInstance == null) {
      fInstance = new OpenOnProvider();
    }
    return fInstance;
  }

  /**
   * Returns the content type of document
   * 
   * @param document - assumes document is not null
   * @return String content type of given document
   */
  protected String getContentType(IDocument document) {
    String type = null;

    IModelManager mgr = StructuredModelManager.getModelManager();
    IStructuredModel model = null;
    try {
      model = mgr.getExistingModelForRead(document);
      if (model != null) {
        type = model.getContentTypeIdentifier();
      }
    } finally {
      if (model != null) {
        model.releaseFromRead();
      }
    }
    return type;
  }

  /**
   * Returns the appropriate IOpenOn for the current partition
   * 
   * @return
   */
  public IOpenOn getOpenOn(IDocument document, int offset) {
    IOpenOn openOn = null;

    // determine the current partition
    if (document != null) {
      String contentTypeID = getContentType(document);
      String partitionType = getPartitionType(document, offset);

      IContentType contentType = Platform.getContentTypeManager().getContentType(contentTypeID);

      while (openOn == null && contentType != null) {
        // Query OpenOnBuilder and get the list of OpenOns for the
        // current partition
        OpenOnDefinition[] defs = OpenOnBuilder.getInstance().getOpenOnDefinitions(
            contentType.getId(), partitionType);
        contentType = contentType.getBaseType();

        // If more than 1 openon is returned, need to further check
        // which OpenOn is the appropriate one to return
        // for now just returning the first one
        if (defs != null && defs.length > 0) {
          openOn = defs[0].createOpenOn();
        }
      }
    }

    return openOn;
  }

  /**
   * Returns the partition type located at offset in the document
   * 
   * @param document - assumes document is not null
   * @param offset
   * @return String partition type
   */
  protected String getPartitionType(IDocument document, int offset) {
    String type = null;
    try {
      // TODO: provide partitioning information so we're not using a default like this
      if (document instanceof IStructuredDocument) {
        type = TextUtilities.getContentType(document,
            IStructuredPartitioning.DEFAULT_STRUCTURED_PARTITIONING, offset, false);
      }
    } catch (BadLocationException e1) {
    } finally {
      if (type == null) {
        try {
          ITypedRegion region = document.getPartition(offset);
          if (region != null) {
            type = region.getType();
          }
        } catch (BadLocationException e) {
          type = null;
        }
      }
    }
    return type;
  }
}
