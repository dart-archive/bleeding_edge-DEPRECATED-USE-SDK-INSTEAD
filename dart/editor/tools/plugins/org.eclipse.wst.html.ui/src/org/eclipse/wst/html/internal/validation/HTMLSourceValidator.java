/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.internal.validation;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.html.core.internal.document.HTMLDocumentTypeConstants;
import org.eclipse.wst.html.core.internal.validate.HTMLValidationAdapterFactory;
import org.eclipse.wst.html.core.internal.validation.HTMLValidationReporter;
import org.eclipse.wst.html.core.internal.validation.HTMLValidator;
import org.eclipse.wst.html.core.internal.validation.LocalizedMessage;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.FileBufferModelManager;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.validate.ValidationAdapter;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.ISourceValidator;
import org.eclipse.wst.validation.internal.core.Message;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.xml.core.internal.document.DocumentTypeAdapter;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Text;

public class HTMLSourceValidator extends HTMLValidator implements ISourceValidator {

  private IDocument fDocument;

  public HTMLSourceValidator() {

    super();
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.reconcile.validator.ISourceValidator
   */
  public void connect(IDocument document) {
    fDocument = document;
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.reconcile.validator.ISourceValidator
   */
  public void disconnect(IDocument document) {
    fDocument = null;
  }

  /**
   * This validate call is for the ISourceValidator partial document validation approach
   * 
   * @param dirtyRegion
   * @param helper
   * @param reporter
   * @see org.eclipse.wst.sse.ui.internal.reconcile.validator.ISourceValidator
   */
  public void validate(IRegion dirtyRegion, IValidationContext helper, IReporter reporter) {

    if (helper == null || fDocument == null)
      return;

    if ((reporter != null) && (reporter.isCancelled() == true)) {
      throw new OperationCanceledException();
    }

    IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
        fDocument);
    if (model == null)
      return; // error

    try {

      IDOMDocument document = null;
      if (model instanceof IDOMModel) {
        document = ((IDOMModel) model).getDocument();
      }

      if (document == null || !hasHTMLFeature(document)) {
        // handled in finally clause
        // model.releaseFromRead();
        return; // ignore
      }

      IPath filePath = null;
      IFile file = null;

      ITextFileBuffer fb = FileBufferModelManager.getInstance().getBuffer(fDocument);
      if (fb != null) {
        filePath = fb.getLocation();

        if (filePath.segmentCount() > 1) {
          file = ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
          if (!file.isAccessible()) {
            file = null;
          }
        }
      } else {
        filePath = new Path(model.getId());
      }

      // this will be the wrong region if it's Text (instead of Element)
      // we don't know how to validate Text
      IndexedRegion ir = getCoveringNode(dirtyRegion); // model.getIndexedRegion(dirtyRegion.getOffset());
      if (ir instanceof Text) {
        while (ir != null && ir instanceof Text) {
          // it's assumed that this gets the IndexedRegion to
          // the right of the end offset
          ir = model.getIndexedRegion(ir.getEndOffset());
        }
      }

      if (ir instanceof INodeNotifier) {
        INodeAdapterFactory factory = HTMLValidationAdapterFactory.getInstance();
        ValidationAdapter adapter = (ValidationAdapter) factory.adapt((INodeNotifier) ir);
        if (adapter == null)
          return; // error

        if (reporter != null) {
          HTMLValidationReporter rep = null;
          rep = getReporter(reporter, file, (IDOMModel) model);
          rep.clear();
          adapter.setReporter(rep);

          Message mess = new LocalizedMessage(IMessage.LOW_SEVERITY, filePath.toString().substring(
              1));
          reporter.displaySubtask(this, mess);
        }
        adapter.validate(ir);
      }
    } finally {
      releaseModel(model);
    }
  }

  private IndexedRegion getCoveringNode(IRegion dirtyRegion) {

    IndexedRegion largestRegion = null;
    if (fDocument instanceof IStructuredDocument) {
      IStructuredDocumentRegion[] regions = ((IStructuredDocument) fDocument).getStructuredDocumentRegions(
          dirtyRegion.getOffset(), dirtyRegion.getLength());
      largestRegion = getLargest(regions);
    }
    return largestRegion;
  }

  /**
	 */
  private boolean hasHTMLFeature(IDOMDocument document) {
    DocumentTypeAdapter adapter = (DocumentTypeAdapter) document.getAdapterFor(DocumentTypeAdapter.class);
    if (adapter == null)
      return false;
    return adapter.hasFeature(HTMLDocumentTypeConstants.HTML);
  }

  protected IndexedRegion getLargest(IStructuredDocumentRegion[] sdRegions) {

    if (sdRegions == null || sdRegions.length == 0)
      return null;

    IndexedRegion currentLargest = getCorrespondingNode(sdRegions[0]);
    for (int i = 0; i < sdRegions.length; i++) {
      if (!sdRegions[i].isDeleted()) {
        IndexedRegion corresponding = getCorrespondingNode(sdRegions[i]);

        if (currentLargest instanceof Text)
          currentLargest = corresponding;

        if (corresponding != null) {
          if (!(corresponding instanceof Text)) {
            if (corresponding.getStartOffset() <= currentLargest.getStartOffset()
                && corresponding.getEndOffset() >= currentLargest.getEndOffset())
              currentLargest = corresponding;
          }
        }

      }
    }
    return currentLargest;
  }

  protected IndexedRegion getCorrespondingNode(IStructuredDocumentRegion sdRegion) {
    IStructuredModel sModel = StructuredModelManager.getModelManager().getExistingModelForRead(
        fDocument);
    IndexedRegion indexedRegion = null;
    try {
      if (sModel != null)
        indexedRegion = sModel.getIndexedRegion(sdRegion.getStart());
    } finally {
      if (sModel != null)
        sModel.releaseFromRead();
    }
    return indexedRegion;
  }
}
