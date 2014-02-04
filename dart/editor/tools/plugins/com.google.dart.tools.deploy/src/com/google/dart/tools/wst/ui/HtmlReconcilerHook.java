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
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.deploy.Activator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.ISourceValidator;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;

import java.io.File;

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
      performAnalysisInBackground();
    }
  };

  private IDocument document;
  private File file;
  private IResource resource;
  private Project project;

  public HtmlReconcilerHook() {
  }

  @Override
  public void cleanup(IReporter reporter) {
    // Not used, but WST expects IValidator
  }

  @Override
  public void connect(IDocument document) {
    this.document = document;
    // prepare File
    ITextFileBufferManager fileManager = FileBuffers.getTextFileBufferManager();
    ITextFileBuffer fileBuffer = fileManager.getTextFileBuffer(document);
    try {
      file = fileBuffer.getFileStore().toLocalFile(0, null);
    } catch (CoreException ex) {
      Activator.logError(ex);
      return;
    }
    // prepare IResource
    IResource resource = ResourceUtil.getResource(file);
    if (resource == null) {
      this.document = null;
      this.file = null;
      this.project = null;
      this.resource = null;
      return;
    }
    this.resource = resource;
    // prepare model Project
    IProject resourceProject = resource.getProject();
    project = DartCore.getProjectManager().getProject(resourceProject);
    // TODO(scheglov) disabled because of the problems with big Angular project
    if (true) {
      return;
    }
    // track changes
//    document.addDocumentListener(documentListener);
//    HtmlReconcilerManager.getInstance().reconcileWith(document, this);
//    // force reconcile
//    sourceChanged(document.get());
//    performAnalysisInBackground();
  }

  @Override
  public void disconnect(IDocument document) {
    sourceChanged(null);
    performAnalysisInBackground();
    // clean up
    document.removeDocumentListener(documentListener);
    DartReconcilerManager.getInstance().reconcileWith(document, null);
    this.document = null;
    this.file = null;
    this.project = null;
    this.resource = null;
  }

  public HtmlUnit getResolvedUnit() {
    AnalysisContext analysisContext = getContext();
    Source source = getSource();
    if (analysisContext == null || source == null) {
      return null;
    }
    return analysisContext.getResolvedHtmlUnit(source);
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

  private AnalysisContext getContext() {
    return DartCore.getProjectManager().getContext(resource);
  }

  private Source getSource() {
    AnalysisContext analysisContext = getContext();
    if (analysisContext == null) {
      return null;
    }
    return new FileBasedSource(analysisContext.getSourceFactory().getContentCache(), file);
  }

  /**
   * Start background analysis of the context containing the source being edited.
   */
  private void performAnalysisInBackground() {
    AnalysisContext context = getContext();
    if (context != null) {
      AnalysisManager.getInstance().performAnalysisInBackground(project, context);
    }
  }

  /**
   * Notify the context that the source has changed.
   * 
   * @param code the new source code or {@code null} if the source should be pulled from disk
   */
  private void sourceChanged(String code) {
    AnalysisContext context = getContext();
    Source source = getSource();
    if (context != null && source != null) {
      context.setContents(source, code);
    }
  }
}
