/*
 * Copyright (c) 2011, the Dart project authors.
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
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The visitor that does the actual work.
 */
public class TextSearchVisitor {

  public static class ReusableMatchAccess extends TextSearchMatchAccess {

    private int fOffset;
    private int fLength;
    private IFile fFile;
    private CharSequence fContent;

    @Override
    public IFile getFile() {
      return fFile;
    }

    @Override
    public String getFileContent(int offset, int length) {
      return fContent.subSequence(offset, offset + length).toString(); // must pass a copy!
    }

    @Override
    public char getFileContentChar(int offset) {
      return fContent.charAt(offset);
    }

    @Override
    public int getFileContentLength() {
      return fContent.length();
    }

    @Override
    public int getMatchLength() {
      return fLength;
    }

    @Override
    public int getMatchOffset() {
      return fOffset;
    }

    public void initialize(IFile file, int offset, int length, CharSequence content) {
      fFile = file;
      fOffset = offset;
      fLength = length;
      fContent = content;
    }
  }

  private final TextSearchRequestor fCollector;
  private final Matcher fMatcher;

  private IProgressMonitor fProgressMonitor;

  private int fNumberOfScannedFiles;
  private int fNumberOfFilesToScan;
  private IFile fCurrentFile;

  private final MultiStatus fStatus;

  private final FileCharSequenceProvider fFileCharSequenceProvider;

  private final ReusableMatchAccess fMatchAccess;

  public TextSearchVisitor(TextSearchRequestor collector, Pattern searchPattern) {
    fCollector = collector;
    fStatus = new MultiStatus(NewSearchUI.PLUGIN_ID, IStatus.OK,
        SearchMessages.TextSearchEngine_statusMessage, null);

    fMatcher = searchPattern.pattern().length() == 0 ? null : searchPattern.matcher(new String());

    fFileCharSequenceProvider = new FileCharSequenceProvider();
    fMatchAccess = new ReusableMatchAccess();
  }

  public boolean processFile(IFile file, Map<IFile, IDocument> documentsInEditors) {
    try {
      if (!fCollector.acceptFile(file) || fMatcher == null) {
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
          seq = fFileCharSequenceProvider.newCharSequence(file);
          if (hasBinaryContent(seq, file) && !fCollector.reportBinaryFile(file)) {
            return true;
          }
          locateMatches(file, seq);
        } catch (FileCharSequenceProvider.FileCharSequenceException e) {
          e.throwWrappedException();
        } finally {
          if (seq != null) {
            try {
              fFileCharSequenceProvider.releaseCharSequence(seq);
            } catch (IOException e) {
              SearchPlugin.log(e);
            }
          }
        }
      }
    } catch (UnsupportedCharsetException e) {
      String[] args = {getCharSetName(file), file.getFullPath().makeRelative().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_unsupportedcharset, args);
      fStatus.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (IllegalCharsetNameException e) {
      String[] args = {getCharSetName(file), file.getFullPath().makeRelative().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_illegalcharset, args);
      fStatus.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (IOException e) {
      String[] args = {getExceptionMessage(e), file.getFullPath().makeRelative().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_error, args);
      fStatus.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (CoreException e) {
      String[] args = {getExceptionMessage(e), file.getFullPath().makeRelative().toString()};
      String message = Messages.format(SearchMessages.TextSearchVisitor_error, args);
      fStatus.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
    } catch (StackOverflowError e) {
      String message = SearchMessages.TextSearchVisitor_patterntoocomplex0;
      fStatus.add(new Status(IStatus.ERROR, NewSearchUI.PLUGIN_ID, IStatus.ERROR, message, e));
      return false;
    } finally {
      fNumberOfScannedFiles++;
    }
    if (fProgressMonitor.isCanceled()) {
      throw new OperationCanceledException(SearchMessages.TextSearchVisitor_canceled);
    }

    return true;
  }

  public IStatus search(IFile[] files, IProgressMonitor monitor) {
    fProgressMonitor = monitor == null ? new NullProgressMonitor() : monitor;
    fNumberOfScannedFiles = 0;
    fNumberOfFilesToScan = files.length;
    fCurrentFile = null;

    Job monitorUpdateJob = new Job(SearchMessages.TextSearchVisitor_progress_updating_job) {
      private int fLastNumberOfScannedFiles = 0;

      @Override
      public IStatus run(IProgressMonitor inner) {
        while (!inner.isCanceled()) {
          IFile file = fCurrentFile;
          if (file != null) {
            String fileName = file.getName();
            Object[] args = {
                fileName, new Integer(fNumberOfScannedFiles), new Integer(fNumberOfFilesToScan)};
            fProgressMonitor.subTask(Messages.format(SearchMessages.TextSearchVisitor_scanning,
                args));
            int steps = fNumberOfScannedFiles - fLastNumberOfScannedFiles;
            fProgressMonitor.worked(steps);
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
      String taskName = fMatcher == null ? SearchMessages.TextSearchVisitor_filesearch_task_label
          : Messages.format(SearchMessages.TextSearchVisitor_textsearch_task_label,
              fMatcher.pattern().pattern());
      fProgressMonitor.beginTask(taskName, fNumberOfFilesToScan);
      monitorUpdateJob.setSystem(true);
      monitorUpdateJob.schedule();
      try {
        fCollector.beginReporting();
        processFiles(files);
        return fStatus;
      } finally {
        monitorUpdateJob.cancel();
      }
    } finally {
      fProgressMonitor.done();
      fCollector.endReporting();
    }
  }

  public IStatus search(TextSearchScope scope, IProgressMonitor monitor) {
    return search(scope.evaluateFilesInScope(fStatus), monitor);
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
        ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(file.getFullPath(),
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

  private IDocument getOpenDocument(IFile file, Map<IFile, IDocument> documentsInEditors) {
    IDocument document = documentsInEditors.get(file);
    if (document == null) {
      ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
      ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(file.getFullPath(),
          LocationKind.IFILE);
      if (textFileBuffer != null) {
        document = textFileBuffer.getDocument();
      }
    }
    return document;
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

  private void locateMatches(IFile file, CharSequence searchInput) throws CoreException {
    try {
      fMatcher.reset(searchInput);
      int k = 0;
      while (fMatcher.find()) {
        int start = fMatcher.start();
        int end = fMatcher.end();
        if (end != start) { // don't report 0-length matches
          fMatchAccess.initialize(file, start, end - start, searchInput);
          boolean res = fCollector.acceptPatternMatch(fMatchAccess);
          if (!res) {
            return; // no further reporting requested
          }
        }
        if (k++ == 20) {
          if (fProgressMonitor.isCanceled()) {
            throw new OperationCanceledException(SearchMessages.TextSearchVisitor_canceled);
          }
          k = 0;
        }
      }
    } finally {
      fMatchAccess.initialize(null, 0, 0, new String()); // clear references
    }
  }

  private void processFiles(IFile[] files) {
    final Map<IFile, IDocument> documentsInEditors;
    if (PlatformUI.isWorkbenchRunning()) {
      documentsInEditors = evalNonFileBufferDocuments();
    } else {
      documentsInEditors = Collections.emptyMap();
    }

    for (int i = 0; i < files.length; i++) {
      fCurrentFile = files[i];
      boolean res = processFile(fCurrentFile, documentsInEditors);
      if (!res) {
        break;
      }
    }
  }

}
