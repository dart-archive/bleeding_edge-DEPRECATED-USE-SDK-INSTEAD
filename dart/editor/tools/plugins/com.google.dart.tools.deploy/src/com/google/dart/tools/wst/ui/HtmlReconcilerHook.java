/*
 * Copyright (c) 2014, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.wst.ui;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.analysis.model.Project;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.ISourceValidator;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;

/**
 * Bridge between WST HTML document and resolved {@link HtmlUnit}.
 */
public class HtmlReconcilerHook implements ISourceValidator, IValidator {

  private IDocumentListener documentListener = new IDocumentListener() {
    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    @Override
    public void documentChanged(DocumentEvent event) {
      sourceChanged(document.get());
    }
  };

  private IDocument document;
  private StructuredDocumentDartInfo documentInfo;

  public HtmlReconcilerHook() {
  }

  @Override
  public void cleanup(IReporter reporter) {
    // Not used, but WST expects IValidator
  }

  @Override
  public void connect(IDocument document) {
    this.document = document;
    this.documentInfo = StructuredDocumentDartInfo.create(document);
    // we need it
    if (documentInfo == null) {
      return;
    }
    // track changes
    document.addDocumentListener(documentListener);
    HtmlReconcilerManager.getInstance().reconcileWith(document, this);
    // force reconcile
    sourceChanged(document.get());
  }

  @Override
  public void disconnect(IDocument document) {
    sourceChanged(null);
    // clean up
    document.removeDocumentListener(documentListener);
    DartReconcilerManager.getInstance().reconcileWith(document, null);
    this.document = null;
    this.documentInfo = null;
  }

  public HtmlUnit getResolvedUnit() {
    if (documentInfo != null) {
      AnalysisContext context = documentInfo.getContext();
      Source source = documentInfo.getSource();
      if (context == null || source == null) {
        return null;
      }
      return context.getResolvedHtmlUnit(source);
    }
    return null;
  }

  @Override
  public void validate(IRegion dirtyRegion, IValidationContext helper, IReporter reporter) {
    // Not used, but WST expects IValidator
  }

  @Override
  @SuppressWarnings("restriction")
  public void validate(IValidationContext helper, IReporter reporter)
      throws org.eclipse.wst.validation.internal.core.ValidationException {
    // Not used, but WST expects IValidator
  }

  /**
   * Notify the context that the source has changed.
   * <p>
   * Note, that {@link Source} is updated in {@link AnalysisContext} in the background thread, so
   * there is a small probability that some operation will be initiated on not-quite-synchronized
   * content. But it is better than nothing for now...
   * 
   * @param code the new source code or {@code null} if the source should be pulled from disk
   */
  private void sourceChanged(String code) {
    if (documentInfo != null) {
      AnalysisContext context = documentInfo.getContext();
      Source source = documentInfo.getSource();
      if (context != null && source != null) {
        Project project = documentInfo.getProject();
        HtmlReconcilerManager.performUpdateInBackground(project, context, source, code);
      }
    }
  }
}
