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
import com.google.dart.engine.context.ChangeSet;
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
 * <p>
 * Currently, only two operations are supported.
 * <ul>
 * <li>Semantic highlighting requires a parsed AST but works better with a resolved AST. Since
 * highlighting runs synchronously in the UI thread the parsed AST is computed when the resolved AST
 * is unavailable.
 * <li>Code completion requires a resolved AST. That is obtained by valiate(), which runs in a
 * background thread.
 * </ul>
 * Other operations that work with the resolved AST could be supported, as long as no changes are
 * required to compute the AST. Parsing and resolving can interfere with each other, leaving fake
 * sources in the analysis context (which breaks refactoring). It isn't practical to synchronize the
 * operations since parsing runs on the UI thread, so separate sources are used for each. Obviously,
 * this is too fragile to be used for much more that it currently is.
 * <p>
 * This whole scheme should be replaced ASAP, after the analysis engine properly records analysis
 * artifacts for HTML files.
 * <p>
 * NB: Although Dartium is currently restricted to a single script in HTML, polymer and angular both
 * support Dart references within {{...}} data binding expressions, and those will have to be
 * handled, too. In particular, Rename will have to work. Eventually, Dartium is expected to support
 * multiple script tags.
 */
@SuppressWarnings("restriction")
public class EmbeddedDartReconcilerHook implements ISourceValidator, IValidator {

  private File file;
  private IResource resource;
  private Project dartProject;
  private CompilationUnit parsedUnit;
  private CompilationUnit resolvedUnit;
  private IDocument document;
  private int partOffset;
  private int partLength;

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
    return null;
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
    if (this.document == null) {
      connect(document);
    }
    return resolveParsedUnit(offset, length);
  }

  public boolean isParsed(int offset, int length) {
    return parsedUnit != null && partOffset == offset && partLength == length;
  }

  public boolean isResolved(int offset, int length) {
    return resolvedUnit != null && partOffset == offset && partLength == length;
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

  /**
   * Add a fake source containing only the embedded Dart code to the analysis context, parse it,
   * then delete the fake source. Leaving the fake source in place causes various problems elsewhere
   * as most of the analysis routines assume all sources are always valid, and represent an entire
   * file.
   * 
   * @param offset index of the beginning of the Dart script partition
   * @param length length of the Dart script partition
   * @return a parsed compilation unit
   */
  private CompilationUnit parseSource(int offset, int length) {
    IDocument document = this.document; // Cache in case of async disconnect().
    File file = this.file;
    AnalysisContext analysisContext = getInputAnalysisContext();
    if (analysisContext == null) {
      return null;
    }
    try {
      String code = document.get(offset, length);
      File tempFile = new File(file.getParentFile(), file.getName() + offset + "p.dart");
      Source source = new FileBasedSource(tempFile);
      analysisContext.setContents(source, code);
      parsedUnit = analysisContext.parseCompilationUnit(source);
      analysisContext.setContents(source, null);
      ChangeSet changeSet = new ChangeSet();
      changeSet.removedSource(source);
      analysisContext.applyChanges(changeSet);
    } catch (BadLocationException ex) {
      Activator.logError(ex);
      return null;
    } catch (AnalysisException ex) {
      Activator.logError(ex);
      return null;
    }
    partOffset = offset;
    partLength = length;
    return parsedUnit;
  }

  private void reset() {
    partOffset = partLength = -1;
    parsedUnit = resolvedUnit = null;
  }

  /**
   * Add a fake source containing only the embedded Dart code to the analysis context, parse and
   * resolve it, record the error messages, then delete the fake source. Leaving the fake source in
   * place causes various problems elsewhere as most of the analysis routines assume all sources are
   * always valid, and represent an entire file.
   * 
   * @param offset index of the beginning of the Dart script partition
   * @param length length of the Dart script partition
   * @return a resolved compilation unit
   */
  private CompilationUnit resolveParsedUnit(int offset, int length) {
    IDocument document = this.document; // Cache in case of async disconnect().
    File file = this.file;
    AnalysisContext analysisContext = getInputAnalysisContext();
    if (analysisContext == null) {
      return null;
    }
    try {
      String code = document.get(offset, length);
      File tempFile = new File(file.getParentFile(), file.getName() + offset + "r.dart");
      Source source = new FileBasedSource(tempFile);
      analysisContext.setContents(source, code);
      LibraryElement library = analysisContext.computeLibraryElement(source);
      resolvedUnit = analysisContext.resolveCompilationUnit(source, library);
      parsedUnit = resolvedUnit;
      AnalysisErrorInfo errorInfo = analysisContext.getErrors(source);
      adjustPosition(errorInfo.getErrors(), offset);
      AnalysisMarkerManager.getInstance().queueErrors(
          resource,
          new LineInfo(getLineStarts()),
          errorInfo.getErrors());
      analysisContext.setContents(source, null);
      ChangeSet changeSet = new ChangeSet();
      changeSet.removedSource(source);
      analysisContext.applyChanges(changeSet);
    } catch (AnalysisException ex) {
      Activator.logError(ex);
      return null;
    } catch (BadLocationException ex) {
      Activator.logError(ex);
      return null;
    }
    partOffset = offset;
    partLength = length;
    return resolvedUnit;
  }
}
