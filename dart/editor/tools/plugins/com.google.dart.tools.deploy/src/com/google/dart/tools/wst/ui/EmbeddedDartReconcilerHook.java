/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.builder.AnalysisMarkerManager;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.deploy.Activator;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.ISourceValidator;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;

import java.io.File;

/**
 * Attempt to bridge the impedance mismatch between Dart's reconciler and that of WST.
 */
@SuppressWarnings("restriction")
public class EmbeddedDartReconcilerHook implements ISourceValidator, IValidator {

  private File file;
  private IResource resource;
  private Project dartProject;
  private Source source;
  private CompilationUnit parsedUnit;
  private CompilationUnit resolvedUnit;
  private IDocument document;
  private int partOffset;
  private int partLength;
  boolean isParsed;
  boolean isResolved;

  private IDocumentListener documentListener = new IDocumentListener() {
    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
      reset();
    }

    @Override
    public void documentChanged(DocumentEvent event) {
    }
  };

  public EmbeddedDartReconcilerHook() {
  }

  @Override
  public void cleanup(IReporter reporter) {
    // IValidator -- not needed
  }

  @Override
  public void connect(IDocument document) {
    // ISourceValidator
    this.document = document;
    ITextFileBufferManager fileManager = FileBuffers.getTextFileBufferManager();
    ITextFileBuffer fileBuffer = fileManager.getTextFileBuffer(document);
    try {
      file = fileBuffer.getFileStore().toLocalFile(0, null);
    } catch (CoreException ex) {
      Activator.logError(ex);
      return;
    }
    IResource resource = ResourceUtil.getResource(file);
    if (resource == null) {
      // TODO(messick) How to handle html files in packages?
      this.document = null;
      this.file = null;
      this.dartProject = null;
      this.resource = null;
      reset();
      return;
    }
    IProject project = resource.getProject();
    dartProject = DartCore.getProjectManager().getProject(project);
    DartReconcilerManager.getInstance().reconcileWith(document, this);
    document.addPrenotifiedDocumentListener(documentListener);
    this.resource = resource;
  }

  @Override
  public void disconnect(IDocument document) {
    // ISourceValidator
    AnalysisMarkerManager.getInstance().clearMarkers(resource);
    document.removePrenotifiedDocumentListener(documentListener);
    DartReconcilerManager.getInstance().reconcileWith(document, null);
    this.document = null;
    this.file = null;
    this.dartProject = null;
    this.resource = null;
    reset();
  }

  public IDocument getDocument() {
    return document;
  }

  public AnalysisContext getInputAnalysisContext() {
    // DartReconcilingEditor
    if (resource == null || dartProject == null) {
      return null;
    }
    return dartProject.getContext(resource);
  }

  public Project getInputProject() {
    // DartReconcilingEditor
    return dartProject;
  }

  public Source getInputSource() {
    // DartReconcilingEditor
    return source;
  }

  public CompilationUnit getParsedUnit(int offset, int length, IDocument document) {
    if (isParsed(offset, length)) {
      return parsedUnit;
    }
    if (this.document == null) {
      connect(document);
    }
    return parseSource(offset, length);
  }

  public CompilationUnit getResolvedUnit(int offset, int length, IDocument document) {
    if (isResolved(offset, length)) {
      return resolvedUnit;
    }
    if (isParsed(offset, length)) {
      if (resolveParsedUnit() != null) {
        return resolvedUnit;
      }
    }
    getParsedUnit(offset, length, document);
    return resolveParsedUnit();
  }

  @Override
  public void validate(IRegion dirtyRegion, IValidationContext helper, IReporter reporter) {
    // ISourceValidator
    if (file == null) {
      return;
    }
    int start = dirtyRegion.getOffset();
    int length = dirtyRegion.getLength();
    getResolvedUnit(start, length, document);
    AnalysisMarkerManager.getInstance().clearMarkers(resource);
    AnalysisErrorInfo errorInfo = getInputAnalysisContext().getErrors(source);
    adjustPosition(errorInfo.getErrors(), start);
    AnalysisMarkerManager.getInstance().queueErrors(
        resource,
        new LineInfo(getLineStarts()),
        errorInfo.getErrors());
  }

  @Override
  public void validate(IValidationContext helper, IReporter reporter) throws ValidationException {
    // IValidator -- hopefully, not used
  }

  private void adjustPosition(AnalysisError[] errors, int delta) {
    for (AnalysisError error : errors) {
      int offset = error.getOffset() + delta;
      ReflectionUtils.setField(error, "offset", offset);
    }
  }

  private int[] getLineStarts() {
    int n = document.getNumberOfLines();
    int[] starts = new int[n];
    int pos = 0;
    try {
      for (int i = 0; i < n; i++) {
        starts[i] = pos;
        int len = document.getLineLength(i);
        pos += len;
      }
    } catch (BadLocationException ex) {
      return new int[1];
    }
    return starts;
  }

  private boolean isParsed(int offset, int length) {
    return parsedUnit != null && partOffset == offset && partLength == length;
  }

  private boolean isResolved(int offset, int length) {
    return resolvedUnit != null && partOffset == offset && partLength == length;
  }

  private CompilationUnit parseSource(int offset, int length) {
    if (document == null || resource == null) {
      return null;
    }
    AnalysisContext analysisContext = getInputAnalysisContext();
    if (analysisContext == null) {
      return null;
    }
    if (source != null && (partOffset != offset || partLength != length)) {
      analysisContext.setContents(source, null); // delete previous source
    }
    try {
      String code = document.get(offset, length);
      File tempFile = new File(file.getParentFile(), file.getName() + offset + ".dart");
      source = new FileBasedSource(analysisContext.getSourceFactory().getContentCache(), tempFile);
      analysisContext.setContents(source, code);
      parsedUnit = analysisContext.parseCompilationUnit(source);
    } catch (BadLocationException ex) {
      Activator.logError(ex);
      return null;
    } catch (AnalysisException ex) {
      Activator.logError(ex);
      return null;
    }
    partOffset = offset;
    partLength = length;
    isParsed = true;
    return parsedUnit;
  }

  private void reset() {
    isParsed = isResolved = false;
    partOffset = partLength = -1;
    parsedUnit = resolvedUnit = null;
  }

  private CompilationUnit resolveParsedUnit() {
    AnalysisContext analysisContext = getInputAnalysisContext();
    if (analysisContext == null) {
      return null;
    }
    try {
      LibraryElement library = analysisContext.computeLibraryElement(source);
      resolvedUnit = analysisContext.resolveCompilationUnit(source, library);
    } catch (AnalysisException ex) {
      Activator.logError(ex);
      return null;
    }
    isResolved = true;
    return resolvedUnit;
  }
}
