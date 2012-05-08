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
package com.google.dart.tools.search.internal.ui.text;

import com.google.dart.tools.search.core.text.TextSearchEngine;
import com.google.dart.tools.search.core.text.TextSearchMatchAccess;
import com.google.dart.tools.search.core.text.TextSearchRequestor;
import com.google.dart.tools.search.internal.core.text.PatternConstructor;
import com.google.dart.tools.search.internal.ui.Messages;
import com.google.dart.tools.search.internal.ui.SearchMessages;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.FileTextSearchScope;
import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class FileSearchQuery implements ISearchQuery {

  private final static class TextSearchResultCollector extends TextSearchRequestor {

    private static String getContents(TextSearchMatchAccess matchRequestor, int start, int end) {
      StringBuffer buf = new StringBuffer();
      for (int i = start; i < end; i++) {
        char ch = matchRequestor.getFileContentChar(i);
        if (Character.isWhitespace(ch) || Character.isISOControl(ch)) {
          buf.append(' ');
        } else {
          buf.append(ch);
        }
      }
      return buf.toString();
    }

    private final AbstractTextSearchResult fResult;
    private final boolean fIsFileSearchOnly;
    private final boolean fSearchInBinaries;

    private ArrayList<FileResourceMatch> fCachedMatches;

    private TextSearchResultCollector(AbstractTextSearchResult result, boolean isFileSearchOnly,
        boolean searchInBinaries) {
      fResult = result;
      fIsFileSearchOnly = isFileSearchOnly;
      fSearchInBinaries = searchInBinaries;

    }

    @Override
    public boolean acceptExternalFile(File file) throws CoreException {
      if (fIsFileSearchOnly) {
        fResult.addMatch(new ExternalFileMatch(file));
      }
      flushMatches();
      return true;
    }

    @Override
    public boolean acceptFile(IFile file) throws CoreException {
      if (fIsFileSearchOnly) {
        fResult.addMatch(new FileMatch(file));
      }
      flushMatches();
      return true;
    }

    @Override
    public boolean acceptPatternMatch(TextSearchMatchAccess matchRequestor) throws CoreException {
      int matchOffset = matchRequestor.getMatchOffset();

      LineElement lineElement = getLineElement(matchOffset, matchRequestor);
      if (lineElement != null) {
        FileResourceMatch fileMatch = matchRequestor.createMatch(lineElement);
        fCachedMatches.add(fileMatch);
      }
      return true;
    }

    @Override
    public void beginReporting() {
      fCachedMatches = new ArrayList<FileResourceMatch>();
    }

    @Override
    public void endReporting() {
      flushMatches();
      fCachedMatches = null;
    }

    @Override
    public boolean reportBinaryFile(IFile file) {
      return fSearchInBinaries;
    }

    private void flushMatches() {
      if (!fCachedMatches.isEmpty()) {
        fResult.addMatches(fCachedMatches.toArray(new Match[fCachedMatches.size()]));
        fCachedMatches.clear();
      }
    }

    private LineElement getLineElement(int offset, TextSearchMatchAccess matchRequestor) {
      int lineNumber = 1;
      int lineStart = 0;
      if (!fCachedMatches.isEmpty()) {
        // match on same line as last?
        FileResourceMatch last = fCachedMatches.get(fCachedMatches.size() - 1);
        LineElement lineElement = last.getLineElement();
        if (lineElement.contains(offset)) {
          return lineElement;
        }
        // start with the offset and line information from the last match
        lineStart = lineElement.getOffset() + lineElement.getLength();
        lineNumber = lineElement.getLine() + 1;
      }
      if (offset < lineStart) {
        return null; // offset before the last line
      }

      int i = lineStart;
      int contentLength = matchRequestor.getFileContentLength();
      while (i < contentLength) {
        char ch = matchRequestor.getFileContentChar(i++);
        if (ch == '\n' || ch == '\r') {
          if (ch == '\r' && i < contentLength && matchRequestor.getFileContentChar(i) == '\n') {
            i++;
          }
          if (offset < i) {
            String lineContent = getContents(matchRequestor, lineStart, i); // include line delimiter
            return new LineElement(matchRequestor.getFile(), lineNumber, lineStart, lineContent);
          }
          lineNumber++;
          lineStart = i;
        }
      }
      if (offset < i) {
        String lineContent = getContents(matchRequestor, lineStart, i); // until end of file
        return new LineElement(matchRequestor.getFile(), lineNumber, lineStart, lineContent);
      }
      return null; // offset outside of range
    }
  }

  private final FileTextSearchScope fScope;
  private final String fSearchText;
  private final boolean fIsRegEx;
  private final boolean fIsCaseSensitive;

  private FileSearchResult fResult;

  public FileSearchQuery(String searchText, boolean isRegEx, boolean isCaseSensitive,
      FileTextSearchScope scope) {
    fSearchText = searchText;
    fIsRegEx = isRegEx;
    fIsCaseSensitive = isCaseSensitive;
    fScope = scope;
  }

  @Override
  public boolean canRerun() {
    return true;
  }

  @Override
  public boolean canRunInBackground() {
    return true;
  }

  @Override
  public String getLabel() {
    return SearchMessages.FileSearchQuery_label;
  }

  public String getResultLabel(int nMatches) {
    String searchString = getSearchString();
    if (searchString.length() > 0) {
      // text search
      if (isScopeAllFileTypes()) {
        // search all file extensions
        if (nMatches == 1) {
          Object[] args = {searchString, fScope.getDescription()};
          return Messages.format(SearchMessages.FileSearchQuery_singularLabel, args);
        }
        Object[] args = {searchString, new Integer(nMatches), fScope.getDescription()};
        return Messages.format(SearchMessages.FileSearchQuery_pluralPattern, args);
      }
      // search selected file extensions
      if (nMatches == 1) {
        Object[] args = {searchString, fScope.getDescription(), fScope.getFilterDescription()};
        return Messages.format(SearchMessages.FileSearchQuery_singularPatternWithFileExt, args);
      }
      Object[] args = {
          searchString, new Integer(nMatches), fScope.getDescription(),
          fScope.getFilterDescription()};
      return Messages.format(SearchMessages.FileSearchQuery_pluralPatternWithFileExt, args);
    }
    // file search
    if (nMatches == 1) {
      Object[] args = {fScope.getFilterDescription(), fScope.getDescription()};
      return Messages.format(SearchMessages.FileSearchQuery_singularLabel_fileNameSearch, args);
    }
    Object[] args = {fScope.getFilterDescription(), new Integer(nMatches), fScope.getDescription()};
    return Messages.format(SearchMessages.FileSearchQuery_pluralPattern_fileNameSearch, args);
  }

  @Override
  public ISearchResult getSearchResult() {
    if (fResult == null) {
      fResult = new FileSearchResult(this);
      new SearchResultUpdater(fResult);
    }
    return fResult;
  }

  public FileTextSearchScope getSearchScope() {
    return fScope;
  }

  public String getSearchString() {
    return fSearchText;
  }

  public boolean isCaseSensitive() {
    return fIsCaseSensitive;
  }

  public boolean isFileNameSearch() {
    return fSearchText.length() == 0;
  }

  public boolean isRegexSearch() {
    return fIsRegEx;
  }

  @Override
  public IStatus run(final IProgressMonitor monitor) {
    AbstractTextSearchResult textResult = (AbstractTextSearchResult) getSearchResult();
    textResult.removeAll();

    Pattern searchPattern = getSearchPattern();
    boolean searchInBinaries = !isScopeAllFileTypes();

    TextSearchResultCollector collector = new TextSearchResultCollector(textResult,
        isFileNameSearch(), searchInBinaries);
    return TextSearchEngine.create().search(fScope, collector, searchPattern, monitor);
  }

//  /**
//   * @param result all result are added to this search result
//   * @param monitor the progress monitor to use
//   * @param file the file to search in
//   * @return returns the status of the operation
//   */
//  public IStatus searchInFile(final AbstractTextSearchResult result,
//      final IProgressMonitor monitor, IFile file) {
//    FileTextSearchScope scope = FileTextSearchScope.newSearchScope(new IResource[] {file},
//        new String[] {"*"}, true); //$NON-NLS-1$
//
//    Pattern searchPattern = getSearchPattern();
//    TextSearchResultCollector collector = new TextSearchResultCollector(result, isFileNameSearch(),
//        true);
//
//    return TextSearchEngine.create().search(scope, collector, searchPattern, monitor);
//  }

  protected Pattern getSearchPattern() {
    return PatternConstructor.createPattern(fSearchText, fIsCaseSensitive, fIsRegEx);
  }

  private boolean isScopeAllFileTypes() {
    String[] fileNamePatterns = fScope.getFileNamePatterns();
    if (fileNamePatterns == null) {
      return true;
    }
    for (int i = 0; i < fileNamePatterns.length; i++) {
      if ("*".equals(fileNamePatterns[i])) { //$NON-NLS-1$
        return true;
      }
    }
    return false;
  }
}
