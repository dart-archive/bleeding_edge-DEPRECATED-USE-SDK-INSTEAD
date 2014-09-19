/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.common.base.Objects;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.UriReferencedElement;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.utilities.io.FilenameUtils;
import com.google.dart.tools.core.utilities.net.URIUtilities;
import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.changes.TextChangeCompatibility;
import com.google.dart.tools.internal.corext.refactoring.util.DartElementUtil;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
import com.google.dart.tools.internal.corext.refactoring.util.TextChangeManager;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * {@link MoveParticipant} for updating resource references in Dart libraries.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class MoveResourceParticipant_OLD extends MoveParticipant {
  /**
   * @return the Java {@link File} which corresponds to the given {@link Source}, may be
   *         {@code null} if cannot be determined.
   */
  private static File getSourceFile(Source source) {
    if (source instanceof FileBasedSource) {
      FileBasedSource fileBasedSource = (FileBasedSource) source;
      return new File(fileBasedSource.getFullName()).getAbsoluteFile();
    }
    return null;
  }

  private static boolean isPackageReference(SearchMatch match) throws Exception {
    Element element = match.getElement();
    AnalysisContext context = element.getContext();
    Source source = element.getSource();
    int offset = match.getSourceRange().getOffset() + "'".length();
    String content = context.getContents(source).getData().toString();
    return content.startsWith("package:", offset);
  }

  /**
   * @return {@code true} if we can prove that the given {@link UriReferencedElement} has a relative
   *         URI, so should be updated.
   */
  private static boolean isRelativeUri(UriReferencedElement uriElement) {
    if (uriElement == null) {
      return false;
    }
    String uriString = uriElement.getUri();
    if (uriString == null) {
      return false;
    }
    try {
      URI uri = new URI(uriString);
      return !uri.isAbsolute();
    } catch (URISyntaxException e) {
      return false;
    }
  }

  private final TextChangeManager changeManager = new TextChangeManager();

  private IFile file;

  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
      throws OperationCanceledException {
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return null;
  }

  @Override
  public Change createPreChange(final IProgressMonitor pm) throws CoreException,
      OperationCanceledException {
    return ExecutionUtils.runObjectLog(new RunnableObjectEx<Change>() {
      @Override
      public Change runObject() throws Exception {
        return createChangeEx(pm);
      }
    }, null);
  }

  @Override
  public String getName() {
    return RefactoringMessages.MoveResourceParticipant_name;
  }

  @Override
  protected boolean initialize(Object element) {
    if (element instanceof IFile) {
      file = (IFile) element;
      return true;
    }
    return false;
  }

  private void addReferenceUpdate(SearchMatch match, IContainer destContainer) throws Exception {
    Source source = match.getElement().getSource();
    String newUri = null;
    // try to keep package: URI
    if (isPackageReference(match)) {
      File destDir = destContainer.getLocation().toFile();
      File destFile = new File(destDir, file.getName());
      SourceFactory sourceFactory = match.getElement().getContext().getSourceFactory();
      FileBasedSource destSource = new FileBasedSource(destFile);
      URI destUri = sourceFactory.restoreUri(destSource);
      if (destUri != null) {
        newUri = destUri.toString();
      }
    }
    // if no package: URI, prepare relative
    if (newUri == null) {
      // prepare source File
      File sourceFile = getSourceFile(source);
      if (sourceFile == null) {
        return;
      }
      // prepare relative directory
      URI sourceUri = sourceFile.getParentFile().toURI();
      URI destUri = destContainer.getLocationURI();
      URI relative = URIUtilities.relativize(sourceUri, destUri);
      newUri = relative.getPath();
      if (newUri.length() != 0 && !newUri.endsWith("/")) {
        newUri += "/";
      }
      // append file name
      newUri += file.getName();
    }
    // prepare "old name" range
    SourceRange matchRange = match.getSourceRange();
    int begin = matchRange.getOffset() + "'".length();
    int end = SourceRangeUtils.getEnd(matchRange) - "'".length();
    // add TextEdit to replace "old URI" with "new URI"
    newUri = FilenameUtils.separatorsToUnix(newUri);
    TextEdit edit = new ReplaceEdit(begin, end - begin, newUri);
    addTextEdit(source, RefactoringCoreMessages.RenameProcessor_update_reference, edit);
  }

  private void addTextEdit(Source source, String groupName, TextEdit textEdit) {
    IResource resource = DartCore.getProjectManager().getResource(source);
    TextChange change = getTextChange(resource);
    if (change == null) {
      change = changeManager.get(source);
    }
    TextChangeCompatibility.addTextEdit(change, groupName, textEdit);
  }

  private void addUnitUriTextEdit(CompilationUnitElement fileElement, URI newUnitUri,
      Element element, SourceRange uriRange) {
    if (element != null) {
      Source partUnit = element.getSource();
      addUnitUriTextEdit(fileElement.getSource(), newUnitUri, uriRange, partUnit);
    }
  }

  /**
   * Updates URI of "target" referenced from "source".
   */
  private void addUnitUriTextEdit(Source source, URI sourceUri, SourceRange uriRange, Source target) {
    File sourceFile = getSourceFile(source);
    File targetFile = getSourceFile(target);
    if (sourceFile == null || targetFile == null) {
      return;
    }
    URI targetUri = targetFile.toURI();
    URI relative = URIUtilities.relativize(sourceUri, targetUri);
    String relativeStr = FilenameUtils.separatorsToUnix(relative.toString());
    String relativeSource = "'" + relativeStr + "'";
    ReplaceEdit textEdit = new ReplaceEdit(
        uriRange.getOffset(),
        uriRange.getLength(),
        relativeSource);
    String msg = RefactoringCoreMessages.RenameProcessor_update_reference;
    addTextEdit(source, msg, textEdit);
  }

  /**
   * Implementation of {@link #createChange(IProgressMonitor)} which can throw any exception.
   */
  private Change createChangeEx(IProgressMonitor pm) throws Exception {
    MoveArguments arguments = getArguments();
    // prepare unit
    CompilationUnitElement fileElement = DartElementUtil.getCompilationUnitElement(file);
    if (fileElement == null) {
      return null;
    }
    // update references
    Object destination = arguments.getDestination();
    if (arguments.getUpdateReferences() && destination instanceof IContainer) {
      IContainer destContainer = (IContainer) destination;
      // prepare references
      SearchEngine searchEngine = DartCore.getProjectManager().newSearchEngine();
      List<SearchMatch> references = searchEngine.searchReferences(fileElement, null, null);
      // update references
      for (SearchMatch match : references) {
        if (!isFileReference(match)) {
          continue;
        }
        addReferenceUpdate(match, destContainer);
      }
      // if moved Unit is defining library, updates references from it to its components
      {
        LibraryElement library = fileElement.getLibrary();
        if (library != null && Objects.equal(library.getDefiningCompilationUnit(), fileElement)) {
          //  update directives
          URI newUnitUri = destContainer.getLocationURI();
          for (CompilationUnitElement unitElement : library.getParts()) {
            updateUriElement(fileElement, unitElement, newUnitUri);
          }
          for (ImportElement importElement : library.getImports()) {
            updateUriElement(fileElement, importElement, newUnitUri);
          }
          for (ExportElement exportElement : library.getExports()) {
            updateUriElement(fileElement, exportElement, newUnitUri);
          }
        }
      }
    }
    // return as single CompositeChange
    TextChange[] changes = changeManager.getAllChanges();
    if (changes.length != 0) {
      return new CompositeChange(getName(), changes);
    } else {
      return null;
    }
  }

  /**
   * Check if the the given {@link SearchMatch} is a reference to {@link #file}.
   */
  private boolean isFileReference(SearchMatch match) throws Exception {
    Source source = match.getElement().getSource();
    SourceRange range = match.getSourceRange();
    CharSequence sourceContent = source.getContents().getData();
    String uri = sourceContent.subSequence(range.getOffset(), range.getEnd()).toString();
    String fileName = file.getName();
    if (uri.startsWith("\"") && uri.endsWith(fileName + "\"")) {
      return true;
    }
    if (uri.startsWith("'") && uri.endsWith(fileName + "'")) {
      return true;
    }
    return false;
  }

  private void updateUriElement(CompilationUnitElement fileElement,
      UriReferencedElement uriElement, URI newUnitUri) {
    // may be not relative
    if (!isRelativeUri(uriElement)) {
      return;
    }
    // prepare target Element
    Element targetElement = uriElement;
    if (targetElement instanceof ImportElement) {
      targetElement = ((ImportElement) targetElement).getImportedLibrary();
    }
    if (targetElement instanceof ExportElement) {
      targetElement = ((ExportElement) targetElement).getExportedLibrary();
    }
    // do update
    SourceRange uriRange = SourceRangeFactory.rangeStartEnd(
        uriElement.getUriOffset(),
        uriElement.getUriEnd());
    addUnitUriTextEdit(fileElement, newUnitUri, targetElement, uriRange);
  }
}
