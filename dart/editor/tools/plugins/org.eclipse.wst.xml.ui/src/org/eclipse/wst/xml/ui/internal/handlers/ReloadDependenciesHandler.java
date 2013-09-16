/*******************************************************************************
 * Copyright (c) 2008 Standards for Technology in Automotive Retail and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: David Carver - initial API and
 * implementation, bug 212330
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.modelqueryimpl.CMDocumentLoader;
import org.eclipse.wst.xml.core.internal.contentmodel.modelqueryimpl.InferredGrammarBuildingCMDocumentLoader;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;

public class ReloadDependenciesHandler extends AbstractHandler implements IHandler {
  protected IStructuredModel model;

  /**
	 * 
	 */
  public ReloadDependenciesHandler() {
    // TODO Auto-generated constructor stub
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
   */
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IEditorPart editor = HandlerUtil.getActiveEditor(event);
    ITextEditor textEditor = null;
    if (editor instanceof ITextEditor)
      textEditor = (ITextEditor) editor;
    else {
      Object o = editor.getAdapter(ITextEditor.class);
      if (o != null)
        textEditor = (ITextEditor) o;
    }
    if (textEditor != null) {
      IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
      IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForRead(
          document);
      if (model != null) {
        ModelQuery modelQuery = null;
        try {
          modelQuery = ModelQueryUtil.getModelQuery(model);
        } finally {
          model.releaseFromRead();
        }
        Document domDocument = ((IDOMModel) model).getDocument();
        if ((modelQuery != null) && (modelQuery.getCMDocumentManager() != null)) {
          modelQuery.getCMDocumentManager().getCMDocumentCache().clear();
          // TODO... need to figure out how to access the
          // DOMObserver via ModelQuery
          // ...why?
          CMDocumentLoader loader = new InferredGrammarBuildingCMDocumentLoader(domDocument,
              modelQuery);
          loader.loadCMDocuments();
        }
      }
    }
    return null;
  }
}
