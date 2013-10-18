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
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.deploy.Activator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
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
  private AnalysisContext analysisContext;
  private Project dartProject;
  private Source source;
  private CompilationUnit resolvedUnit;
  private IDocument document;
  private int partOffset;
  private int partLength;

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
      this.analysisContext = null;
      return;
    }
    IProject project = resource.getProject();
    dartProject = DartCore.getProjectManager().getProject(project);
    analysisContext = dartProject.getContext(resource);
    DartReconcilerManager.getInstance().reconcileWith(document, this);
  }

  @Override
  public void disconnect(IDocument document) {
    // ISourceValidator
    DartReconcilerManager.getInstance().reconcileWith(document, null);
    this.document = null;
  }

  public IDocument getDocument() {
    return document;
  }

  public AnalysisContext getInputAnalysisContext() {
    // DartReconcilingEditor
    return analysisContext;
  }

  public Project getInputProject() {
    // DartReconcilingEditor
    return dartProject;
  }

  public Source getInputSource() {
    // DartReconcilingEditor
    return source;
  }

  public CompilationUnit getResolvedUnit(int offset, int length, IDocument document) {
    if (resolvedUnit != null && partOffset == offset && partLength == length) {
      return resolvedUnit;
    }
    if (this.document == null) {
      connect(document);
    }
    resetSource(offset, length);
    return resolvedUnit;
  }

  @Override
  public void validate(IRegion dirtyRegion, IValidationContext helper, IReporter reporter) {
    // ISourceValidator
    if (file == null) {
      return;
    }
    int start = dirtyRegion.getOffset();
    int length = dirtyRegion.getLength();
    resetSource(start, length);
  }

  @Override
  public void validate(IValidationContext helper, IReporter reporter) throws ValidationException {
    // IValidator -- hopefully, not used
  }

  private void resetSource(int offset, int length) {
    if (document != null && analysisContext != null) {
      try {
        String code = document.get(offset, length);
        File tempFile = new File(file.getParentFile(), file.getName() + offset + ".dart");
        source = new FileBasedSource(analysisContext.getSourceFactory().getContentCache(), tempFile);
        analysisContext.setContents(source, code);
        LibraryElement library = analysisContext.computeLibraryElement(source);
        CompilationUnit libraryUnit = analysisContext.resolveCompilationUnit(source, library);
        resolvedUnit = libraryUnit;
        partOffset = offset;
        partLength = length;
      } catch (BadLocationException ex) {
        Activator.logError(ex);
        return;
      } catch (AnalysisException ex) {
        Activator.logError(ex);
        return;
      }
    }
  }
}
