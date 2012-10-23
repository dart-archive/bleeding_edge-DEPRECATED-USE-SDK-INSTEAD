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
package com.google.dart.tools.search.internal.core.text;

import com.google.dart.tools.search.core.text.TextSearchMatchAccess;
import com.google.dart.tools.search.core.text.TextSearchRequestor;
import com.google.dart.tools.search.core.text.TextSearchScope;
import com.google.dart.tools.search.internal.core.text.FileCharSequenceProvider.FileCharSequenceException;
import com.google.dart.tools.search.internal.ui.Messages;
import com.google.dart.tools.search.internal.ui.SearchMessages;
import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.internal.ui.text.ExternalFile;
import com.google.dart.tools.search.internal.ui.text.ExternalFileMatch;
import com.google.dart.tools.search.internal.ui.text.FileMatch;
import com.google.dart.tools.search.internal.ui.text.FileResource;
import com.google.dart.tools.search.internal.ui.text.FileResourceMatch;
import com.google.dart.tools.search.internal.ui.text.LineElement;
import com.google.dart.tools.search.internal.ui.text.WorkspaceFile;
import com.google.dart.tools.search.ui.NewSearchUI;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import java.io.CharConversionException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes a text search across {@link File} and {@link IFile} resources.
 */
public class TextSearchExecutor {

  private static class ReusableMatchAccess extends TextSearchMatchAccess {

    private int offset;
    private int length;
    private Object /* <IFile,File> */file;
    private CharSequence content;

    @Override
    public FileResourceMatch createMatch(LineElement lineElement) {
      if (file instanceof IFile) {
        return new FileMatch((IFile) file, getMatchOffset(), getMatchLength(), lineElement);
      }
      if (file instanceof File) {
        return new ExternalFileMatch((File) file, getMatchOffset(), getMatchLength(), lineElement);
      }
      throw new IllegalArgumentException("file must be of type IFile or File");
    }

    @Override
    public FileResource<?> getFile() {
      if (file instanceof IFile) {
        return new WorkspaceFile((IFile) file);
      }
      if (file instanceof File) {
        return new ExternalFile((File) file);
      }
      throw new IllegalArgumentException("file must be of type IFile or File");
    }

    @Override
    public String getFileContent(int offset, int length) {
      return content.subSequence(offset, offset + length).toString(); // must pass a copy!
    }

    @Override
    public char getFileContentChar(int offset) {
      return content.charAt(offset);
    }

    @Override
    public int getFileContentLength() {
      return content.length();
    }

    @Override
    public int getMatchLength() {
      return length;
    }

    @Override
    public int getMatchOffset() {
      return offset;
    }

    public void initialize(Object file, int offset, int length, CharSequence content) {
      this.file = file;
      this.offset = offset;
      this.length = length;
      this.content = content;
    }
  }

  private final TextSearchRequestor collector;
  private final Matcher matcher;

  private IProgressMonitor progressMonitor;

  private int numberOfScannedFiles;
  private int numberOfFilesToScan;

  private Object /* File, IFile */currentFile;

  private final MultiStatus status;

  private ExternalFileCharSequenceProvider externalFileCharSequenceProvider;
  private final FileCharSequenceProvider fileCharSequenceProvider;

  private final ReusableMatchAccess matchAccess;

  public TextSearchExecutor(TextSearchRequestor collector, Pattern searchPattern) {
    this.collector = collector;
    this.status = new MultiStatus(
        NewSearchUI.PLUGIN_ID,
        IStatus.OK,
        SearchMessages.TextSearchEngine_statusMessage,
        null);

    this.matcher = searchPattern.pattern().length() == 0 ? null
        : searchPattern.matcher(new String());

    this.fileCharSequenceProvider = new FileCharSequenceProvider();
    this.externalFileCharSequenceProvider = new ExternalFileCharSequenceProvider();
    this.matchAccess = new ReusableMatchAccess();
  }

  /**
   * Initiate a search across the given resources.
   * 
   * @param files the workspace file roots to search
   * @param externalFiles the external file roots to search
   * @param monitor a progress monitor for reporting progress
   * @return execution status
   */
  public IStatus search(IFile[] files, File[] externalFiles, IProgressMonitor monitor) {
    progressMonitor = monitor == null ? new NullProgressMonitor() : monitor;
    numberOfScannedFiles = 0;
    numberOfFilesToScan = files.length + externalFiles.length;
    currentFile = null;

    Job monitorUpdateJob = new Job(SearchMessages.TextSearchVisitor_progress_updating_job) {
      private int fLastNumberOfScannedFiles = 0;

      @Override
      public IStatus run(IProgressMonitor inner) {
        while (!inner.isCanceled()) {
          Object file = currentFile;
          if (file != null) {
            String fileName = getFileName(file);
            Object[] args = {
                fileName, new Integer(numberOfScannedFiles), new Integer(numberOfFilesToScan)};
            progressMonitor.subTask(Messages.format(SearchMessages.TextSearchVisitor_scanning, args));
            int steps = numberOfScannedFiles - fLastNumberOfScannedFiles;
            progressMonitor.worked(steps);
            fLastNumberOfScannedFiles += steps;
          }
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            return Status.OK_STATUS;
          }
        }
        return Status.OK_STATUS;
      }
    };

    try {
      String taskName = matcher == null ? SearchMessages.TextSearchVisitor_filesearch_task_label
          : Messages.format(
              SearchMessages.TextSearchVisitor_textsearch_task_label,
              matcher.pattern().pattern());
      progressMonitor.beginTask(taskName, numberOfFilesToScan);
      monitorUpdateJob.setSystem(true);
      monitorUpdateJob.schedule();
      try {
        collector.beginReporting();
        processFiles(files);
        processExternalFiles(externalFiles);
        return status;
      } finally {
        monitorUpdateJob.cancel();
      }
    } finally {
      progressMonitor.done();
      collector.endReporting();
    }
  }

  /**
   * Initiate a search in a given search scope.
   * 
   * @param scope the scope for the search
   * @param monitor a progress monitor for reporting progress
   * @return execution status
   */
  public IStatus search(TextSearchScope scope, IProgressMonitor monitor) {
    return search(
        scope.evaluateFilesInScope(status),
        scope.evaluateExternalFilesInScope(status),
        monitor);
  }

  /**
   * @return returns a map from IFile to IDocument for all open, dirty editors
   */
  private Map<IFile, IDocument> evalNonFileBufferDocuments() {
    Map<IFile, IDocument> result = new HashMap<IFile, IDocument>();
    IWorkbench workbench = SearchPlugin.getDefault().getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    for (int i = 0; i < windows.length; i++) {
      IWorkbenchPage[] pages = windows[i].getPages();
      for (int x = 0; x < pages.length; x++) {
        IEditorReference[] editorRefs = pages[x].getEditorReferences();
        for (int z = 0; z < editorRefs.length; z++) {
          IEditorPart ep = editorRefs[z].getEditor(false);
          if (ep instanceof ITextEditor && ep.isDirty()) { // only dirty editors
            evaluateTextEditor(result, ep);
          }
        }
      }
    }
    return result;
  }

  private void evaluateTextEditor(Map<IFile, IDocument> result, IEditorPart ep) {
    IEditorInput input = ep.getEditorInput();
    if (input instanceof IFileEditorInput) {
      IFile file = ((IFileEditorInput) input).getFile();
      if (!result.containsKey(file)) { // take the first editor found
        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(
            file.getFullPath(),
            LocationKind.IFILE);
        if (textFileBuffer != null) {
          // file buffer has precedence
          result.put(file, textFileBuffer.getDocument());
        } else {
          // use document provider
          IDocument document = ((ITextEditor) ep).getDocumentProvider().getDocument(input);
          if (document != null) {
            result.put(file, document);
          }
        }
      }
    }
  }

  private String getCharSetName(File file) {
    return "unknown";
  }

  private String getCharSetName(IFile file) {
    try {
      return file.getCharset();
    } catch (CoreException e) {
      return "unknown"; //$NON-NLS-1$
    }
  }

  private String getExceptionMessage(Exception e) {
    String message = e.getLocalizedMessage();
    if (message == null) {
      return e.getClass().getName();
    }
    return message;
  }

  private String getFileName(Object file) {
    if (file instanceof File) {
      return ((File) file).getName();
    }
    if (file instanceof IFile) {
      return ((IFile) file).getName();
    }
    return null;
  }

  private IDocument getOpenDocument(IFile file, Map<IFile, IDocument> documentsInEditors) {
    IDocument document = documentsInEditors.get(file);
    if (document == null) {
      ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
      ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(
          file.getFullPath(),
          LocationKind.IFILE);
      if (textFileBuffer != null) {
        document = textFileBuffer.getDocument();
      }
    }
    return document;
  }

  private boolean hasBinaryContent(CharSequence seq, File file) throws CoreException {

    // avoid calling seq.length() at it runs through the complete file,
    // thus it would do so for all binary files.
    try {
      int limit = FileCharSequenceProvider.BUFFER_SIZE;
      for (int i = 0; i < limit; i++) {
        if (seq.charAt(i) == '\0') {
          return true;
        }
      }
    } catch (IndexOutOfBoundsException e) {
    } catch (FileCharSequenceException ex) {
      if (ex.getCause() instanceof CharConversionException) {
        return true;
      }
      throw ex;
    }
    return false;
  }

  private boolean hasBinaryContent(CharSequence seq, IFile file) throws CoreException {
    IContentDescription desc = file.getContentDescription();
    if (desc != null) {
      IContentType contentType = desc.getContentType();
      if (contentType != null
          && contentType.isKindOf(Platform.getContentTypeManager().getContentType(
              IContentTypeManager.CT_TEXT))) {
        return false;
      }
    }

    // avoid calling seq.length() at it runs through the complete file,
    // thus it would do so for all binary files.
    try {
      int limit = FileCharSequenceProvider.BUFFER_SIZE;
      for (int i = 0; i < limit; i++) {
        if (seq.charAt(i) == '\0') {
          return true;
        }
      }
    } catch (IndexOutOfBoundsException e) {
    } catch (FileCharSequenceException ex) {
      if (ex.getCause() instanceof CharConversionException) {
        return true;
      }
      throw ex;
    }
    return false;
  }

  private void locateMatches(File file, CharSequence searchInput) throws CoreException {
    try {
      matcher.reset(searchInput);
      int k = 0;
      while (matcher.find()) {
        int start = matcher.start();
        int end = matcher.end();
        if (end != start) { // don't report 0-length matches
          matchAccess.initialize(file, start, end - start, searchInput);
          boolean res = collector.acceptPatternMatch(matchAccess);
          if (!res) {
            return; // no further reporting requested
          }
        }
        if (k++ == 20) {
          if (progressMonitor.isCanceled()) {
            throw new OperationCanceledException(SearchMessages.TextSearchVisitor_canceled);
          }
          k = 0;
        }
      }
    } finally {
      matchAccess.initialize(null, 0, 0, new String()); // clear references
    }
  }

  private void locateMatches(IFile file, CharSequence searchInput) throws CoreException {
    try {
      matcher.reset(searchInput);
      int k = 0;
      while (matcher.find()) {
        int start = matcher.start();
        int end = matcher.end();
        if (end != start) { // don't report 0-length matches
          matchAccess.initialize(file, start, end - start, searchInput);
          boolean res = collector.acceptPatternMatch(matchAccess);
          if (!res) {
            return; // no further reporting requested
          }
        }
        if (k++ == 20) {
          if (progressMonitor.isCanceled()) {
            throw new OperationCanceledException(SearchMessages.TextSearchVisitor_canceled);
          }
          k = 0;
        }
      }
    } finally {
      matchAccess.initialize(null, 0, 0, new String()); // clear references
    }
  }

  private boolean processExternalFile(File file) {
    try {
      if (!collector.acceptExternalFile(file) || matcher == null) {
        return true;
      }

      CharSequence seq = null;
      try {
        seq = externalFileCharSequenceProvider.newCharSequence(file);
        //TODO(pquitslund): flip this test?
        if (hasBinaryContent(seq, file) && !collector.reportBinaryExternalFile(file)) {
          return true;
        }
        locateMatches(file, seq);
      } catch (FileCharSequenceProvider.FileCharSequenceException e) {
        e.throwWrappedException();
      } finally {
        if (seq != null) {
          try {
            fileCharSequenceProvider.releaseCharSequence(seq);
          } catch (IOException e) {
            SearchPlugin.log(e);
          }
        }

      }
    } catch (UnsupportedCharsetException e) {
      String[] args = {getCharSetName(file), file.getAbsolutePath().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_unsupportedcharset, args);
      status.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (IllegalCharsetNameException e) {
      String[] args = {getCharSetName(file), file.getAbsolutePath().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_illegalcharset, args);
      status.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (IOException e) {
      String[] args = {getExceptionMessage(e), file.getAbsolutePath().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_error, args);
      status.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (CoreException e) {
      String[] args = {getExceptionMessage(e), file.getAbsolutePath().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_error, args);
      status.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (StackOverflowError e) {
      String message = SearchMessages.TextSearchVisitor_patterntoocomplex0;
      status.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
      return false;
    } finally {
      numberOfScannedFiles++;
    }
    if (progressMonitor.isCanceled()) {
      throw new OperationCanceledException(SearchMessages.TextSearchVisitor_canceled);
    }

    return true;
  }

  private void processExternalFiles(File[] files) {
    for (File file : files) {
      currentFile = file;
      processExternalFile(file);
    }
  }

  private boolean processFile(IFile file, Map<IFile, IDocument> documentsInEditors) {
    try {

      if (!file.exists() || !collector.acceptFile(file) || matcher == null) {
        return true;
      }

      IDocument document = getOpenDocument(file, documentsInEditors);

      if (document != null) {
        DocumentCharSequence documentCharSequence = new DocumentCharSequence(document);
        // assume all documents are non-binary
        locateMatches(file, documentCharSequence);
      } else {
        CharSequence seq = null;
        try {
          seq = fileCharSequenceProvider.newCharSequence(file);
          if (hasBinaryContent(seq, file) && !collector.reportBinaryFile(file)) {
            return true;
          }
          locateMatches(file, seq);
        } catch (FileCharSequenceProvider.FileCharSequenceException e) {
          e.throwWrappedException();
        } finally {
          if (seq != null) {
            try {
              fileCharSequenceProvider.releaseCharSequence(seq);
            } catch (IOException e) {
              SearchPlugin.log(e);
            }
          }
        }
      }
    } catch (UnsupportedCharsetException e) {
      String[] args = {getCharSetName(file), file.getFullPath().makeRelative().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_unsupportedcharset, args);
      status.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (IllegalCharsetNameException e) {
      String[] args = {getCharSetName(file), file.getFullPath().makeRelative().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_illegalcharset, args);
      status.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (IOException e) {
      String[] args = {getExceptionMessage(e), file.getFullPath().makeRelative().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_error, args);
      status.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (CoreException e) {
      String[] args = {getExceptionMessage(e), file.getFullPath().makeRelative().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_error, args);
      status.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (StackOverflowError e) {
      String message = SearchMessages.TextSearchVisitor_patterntoocomplex0;
      status.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
      return false;
    } finally {
      numberOfScannedFiles++;
    }
    if (progressMonitor.isCanceled()) {
      throw new OperationCanceledException(SearchMessages.TextSearchVisitor_canceled);
    }

    return true;
  }

  private void processFiles(IFile[] files) {
    final Map<IFile, IDocument> documentsInEditors;
    if (PlatformUI.isWorkbenchRunning()) {
      documentsInEditors = evalNonFileBufferDocuments();
    } else {
      documentsInEditors = Collections.emptyMap();
    }

    for (IFile file : files) {
      currentFile = file;
      boolean res = processFile(file, documentsInEditors);
      if (!res) {
        break;
      }
    }
  }

}
