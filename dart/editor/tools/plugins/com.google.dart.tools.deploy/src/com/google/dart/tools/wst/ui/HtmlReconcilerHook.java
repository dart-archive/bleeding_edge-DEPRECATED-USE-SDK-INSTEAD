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
import com.google.dart.engine.internal.element.angular.AngularApplication;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.ObjectUtilities;
import com.google.dart.tools.core.analysis.model.AnalysisListener;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ResolvedHtmlEvent;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.ui.internal.text.dart.DartUpdateSourceHelper;

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
  private HtmlUnit resolvedUnit;
  private AngularApplication application;
  private AnalysisListener analysisListener;

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
    // remember resolved HtmlUnit
    analysisListener = new AnalysisListener.Empty() {
      @Override
      public void resolvedHtml(ResolvedHtmlEvent event) {
        StructuredDocumentDartInfo info = documentInfo;
        if (info != null) {
          AnalysisContext eventContext = event.getContext();
          Source eventSource = event.getSource();
          if (eventContext == info.getContext()
              && ObjectUtilities.equals(eventSource, info.getSource())) {
            resolvedUnit = event.getUnit();
            application = eventContext.getAngularApplicationWithHtml(eventSource);
          }
        }
      }
    };
    AnalysisWorker.addListener(analysisListener);
    // force reconcile
    sourceChanged(document.get());
  }

  @Override
  public void disconnect(IDocument document) {
    AnalysisWorker.removeListener(analysisListener);
    sourceChanged(null);
    // clean up
    document.removeDocumentListener(documentListener);
    DartReconcilerManager.getInstance().reconcileWith(document, null);
    this.document = null;
    this.documentInfo = null;
    this.resolvedUnit = null;
  }

  public AngularApplication getApplication() {
    return application;
  }

  public AnalysisContext getContext() {
    StructuredDocumentDartInfo info = documentInfo;
    return info != null ? info.getContext() : null;
  }

  public HtmlUnit getResolvedUnit() {
    return resolvedUnit;
  }

  public Source getSource() {
    StructuredDocumentDartInfo info = documentInfo;
    return info != null ? info.getSource() : null;
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
      if (context != null) {
        Project project = documentInfo.getProject();
        Source source = documentInfo.getSource();
        DartUpdateSourceHelper.getInstance().updateWithDelay(project, context, source, code);
      }
    }
  }
}
