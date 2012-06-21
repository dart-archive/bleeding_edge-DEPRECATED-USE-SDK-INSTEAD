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

import com.google.dart.tools.search.internal.core.text.PatternConstructor;
import com.google.dart.tools.search.internal.ui.Messages;
import com.google.dart.tools.search.internal.ui.SearchMessages;
import com.google.dart.tools.search.ui.text.Match;
import com.google.dart.tools.search2.internal.ui.InternalSearchUI;
import com.google.dart.tools.search2.internal.ui.text.PositionTracker;

import com.ibm.icu.text.Collator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ReplaceRefactoring extends Refactoring {

  public static class SearchResultUpdateChange extends Change {

    private MatchGroup[] fMatchGroups;
    private Match[] fMatches;

    private Map<URI, ArrayList<FileMatch>> fIgnoredMatches;
    private final FileSearchResult fResult;
    private final boolean fIsRemove;

    public SearchResultUpdateChange(FileSearchResult result, MatchGroup[] matchGroups,
        Map<URI, ArrayList<FileMatch>> ignoredMatches) {
      this(result, null, ignoredMatches, true);
      fMatchGroups = matchGroups;
    }

    private SearchResultUpdateChange(FileSearchResult result, Match[] matches,
        Map<URI, ArrayList<FileMatch>> ignoredMatches, boolean isRemove) {
      fResult = result;
      fMatches = matches;
      fIgnoredMatches = ignoredMatches;
      fIsRemove = isRemove;
    }

    @Override
    public Object getModifiedElement() {
      return null;
    }

    @Override
    public String getName() {
      return SearchMessages.ReplaceRefactoring_result_update_name;
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm) {
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
        OperationCanceledException {
      return new RefactoringStatus();
    }

    @Override
    public Change perform(IProgressMonitor pm) throws CoreException {
      Match[] matches = getMatches();
      if (fIsRemove) {
        fResult.removeMatches(matches);
      } else {
        fResult.addMatches(matches);
      }
      return new SearchResultUpdateChange(fResult, matches, fIgnoredMatches, !fIsRemove);
    }

    private Match[] getMatches() {
      if (fMatches == null) {
        ArrayList<FileMatch> matches = new ArrayList<FileMatch>();
        for (int i = 0; i < fMatchGroups.length; i++) {
          MatchGroup curr = fMatchGroups[i];
          if (curr.group.isEnabled()) {
            FileMatch match = curr.match;
            matches.add(match);

            if (fIgnoredMatches == null) {
              continue;
            }

            // Add matches that we removed before starting the refactoring
            IFile file = match.getFile();
            URI uri = file.getLocationURI();
            if (uri != null) {
              ArrayList<FileMatch> ignoredMatches = fIgnoredMatches.get(uri);
              if (ignoredMatches != null) {
                matches.addAll(ignoredMatches);
              }
            }
          }
        }
        fMatches = matches.toArray(new Match[matches.size()]);
        fMatchGroups = null;
      }
      return fMatches;
    }

  }

  private static class MatchGroup {
    public TextEditChangeGroup group;
    public FileMatch match;

    public MatchGroup(TextEditChangeGroup group, FileMatch match) {
      this.group = group;
      this.match = match;
    }
  }

  private static String getOriginalText(IDocument doc, int offset, int length) {
    try {
      return doc.get(offset, length);
    } catch (BadLocationException e) {
      return null;
    }
  }

  private final FileSearchResult fResult;

  private final Object[] fSelection;

  private final HashMap<IFile, HashSet<FileMatch>> fMatches;

  /** Map that keeps already collected locations. */
  private final Map<URI, IFile> fAlreadyCollected;

  /** Map that keeps ignored matches (can be null). */
  private Map<URI, ArrayList<FileMatch>> fIgnoredMatches;

  private String fReplaceString;

  private Change fChange;

  public ReplaceRefactoring(FileSearchResult result, Object[] selection) {
    Assert.isNotNull(result);

    fResult = result;
    fSelection = selection;

    fMatches = new HashMap<IFile, HashSet<FileMatch>>();
    fAlreadyCollected = new HashMap<URI, IFile>(selection != null ? selection.length
        : result.getElements().length);

    fReplaceString = null;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException,
      OperationCanceledException {
    if (fReplaceString == null) {
      return RefactoringStatus.createFatalErrorStatus(SearchMessages.ReplaceRefactoring_error_no_replace_string);
    }

    Pattern pattern = null;
    FileSearchQuery query = getQuery();
    if (query.isRegexSearch()) {
      pattern = createSearchPattern(query);
    }

    RefactoringStatus resultingStatus = new RefactoringStatus();

    Collection<IFile> allFilesSet = fMatches.keySet();
    IFile[] allFiles = allFilesSet.toArray(new IFile[allFilesSet.size()]);
    Arrays.sort(allFiles, new Comparator() {
      private Collator fCollator = Collator.getInstance();

      @Override
      public int compare(Object o1, Object o2) {
        String p1 = ((IFile) o1).getFullPath().toString();
        String p2 = ((IFile) o2).getFullPath().toString();
        return fCollator.compare(p1, p2);
      }
    });
    checkFilesToBeChanged(allFiles, resultingStatus);
    if (resultingStatus.hasFatalError()) {
      return resultingStatus;
    }

    CompositeChange compositeChange = new CompositeChange(
        SearchMessages.ReplaceRefactoring_composite_change_name);
    compositeChange.markAsSynthetic();

    ArrayList<MatchGroup> matchGroups = new ArrayList<MatchGroup>();
    boolean hasChanges = false;
    try {
      for (int i = 0; i < allFiles.length; i++) {
        IFile file = allFiles[i];
        Collection bucket = fMatches.get(file);
        if (!bucket.isEmpty()) {
          try {
            TextChange change = createFileChange(
                file,
                pattern,
                bucket,
                resultingStatus,
                matchGroups);
            if (change != null) {
              compositeChange.add(change);
              hasChanges = true;
            }
          } catch (CoreException e) {
            String message = Messages.format(
                SearchMessages.ReplaceRefactoring_error_access_file,
                new Object[] {file.getName(), e.getLocalizedMessage()});
            return RefactoringStatus.createFatalErrorStatus(message);
          }
        }
      }
    } catch (PatternSyntaxException e) {
      String message = Messages.format(
          SearchMessages.ReplaceRefactoring_error_replacement_expression,
          e.getLocalizedMessage());
      return RefactoringStatus.createFatalErrorStatus(message);
    }
    if (!hasChanges && resultingStatus.isOK()) {
      return RefactoringStatus.createFatalErrorStatus(SearchMessages.ReplaceRefactoring_error_no_changes);
    }

    compositeChange.add(new SearchResultUpdateChange(
        fResult,
        matchGroups.toArray(new MatchGroup[matchGroups.size()]),
        fIgnoredMatches));

    fChange = compositeChange;
    return resultingStatus;
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException,
      OperationCanceledException {
    String searchString = getQuery().getSearchString();
    if (searchString.length() == 0) {
      return RefactoringStatus.createFatalErrorStatus(SearchMessages.ReplaceRefactoring_error_illegal_search_string);
    }
    fMatches.clear();

    if (fSelection != null) {
      for (int i = 0; i < fSelection.length; i++) {
        collectMatches(fSelection[i]);
      }
    } else {
      Object[] elements = fResult.getElements();
      for (int i = 0; i < elements.length; i++) {
        collectMatches(elements[i]);
      }
    }
    if (!hasMatches()) {
      return RefactoringStatus.createFatalErrorStatus(SearchMessages.ReplaceRefactoring_error_no_matches);
    }
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return fChange;
  }

  @Override
  public String getName() {
    return SearchMessages.ReplaceRefactoring_refactoring_name;
  }

  public int getNumberOfFiles() {
    return fMatches.keySet().size();
  }

  public int getNumberOfMatches() {
    int count = 0;
    for (Iterator<HashSet<FileMatch>> iterator = fMatches.values().iterator(); iterator.hasNext();) {
      Set<FileMatch> bucket = iterator.next();
      count += bucket.size();
    }
    return count;
  }

  public FileSearchQuery getQuery() {
    return (FileSearchQuery) fResult.getQuery();
  }

  public boolean hasMatches() {
    return !fMatches.isEmpty();
  }

  public void setReplaceString(String string) {
    fReplaceString = string;
  }

  private void checkFilesToBeChanged(IFile[] filesToBeChanged, RefactoringStatus resultingStatus)
      throws CoreException {
    ArrayList<IFile> readOnly = new ArrayList<IFile>();
    for (int i = 0; i < filesToBeChanged.length; i++) {
      IFile file = filesToBeChanged[i];
      if (file.isReadOnly()) {
        readOnly.add(file);
      }
    }
    IFile[] readOnlyFiles = readOnly.toArray(new IFile[readOnly.size()]);

    IStatus status = ResourcesPlugin.getWorkspace().validateEdit(
        readOnlyFiles,
        getValidationContext());
    if (status.getSeverity() == IStatus.CANCEL) {
      throw new OperationCanceledException();
    }
    resultingStatus.merge(RefactoringStatus.create(status));
    if (resultingStatus.hasFatalError()) {
      return;
    }
    resultingStatus.merge(ResourceChangeChecker.checkFilesToBeChanged(filesToBeChanged, null));
  }

  private void collectMatches(Object object) throws CoreException {
    if (object instanceof LineElement) {
      LineElement lineElement = (LineElement) object;
      FileMatch[] matches = lineElement.getMatches(fResult);
      for (int i = 0; i < matches.length; i++) {
        FileMatch fileMatch = matches[i];
        if (isMatchToBeIncluded(fileMatch)) {
          getBucket(fileMatch.getFile()).add(fileMatch);
        }
      }
    } else if (object instanceof IContainer) {
      IContainer container = (IContainer) object;
      IResource[] members = container.members();
      for (int i = 0; i < members.length; i++) {
        collectMatches(members[i]);
      }
    } else if (object instanceof IFile) {
      Match[] matches = fResult.getMatches(object);
      if (matches.length > 0) {
        Collection<FileMatch> bucket = null;
        for (int i = 0; i < matches.length; i++) {
          FileMatch fileMatch = (FileMatch) matches[i];
          if (isMatchToBeIncluded(fileMatch)) {
            if (bucket == null) {
              bucket = getBucket((IFile) object);
            }
            bucket.add(fileMatch);
          }
        }
      }
    }
  }

  private String computeReplacementString(Pattern pattern, String originalText,
      String replacementText, String lineDelimiter) throws PatternSyntaxException {
    if (pattern != null) {
      try {
        replacementText = PatternConstructor.interpretReplaceEscapes(
            replacementText,
            originalText,
            lineDelimiter);

        Matcher matcher = pattern.matcher(originalText);
        StringBuffer sb = new StringBuffer();
        matcher.reset();
        if (matcher.find()) {
          matcher.appendReplacement(sb, replacementText);
        } else {
          return null;
        }
        matcher.appendTail(sb);
        return sb.toString();
      } catch (IndexOutOfBoundsException ex) {
        throw new PatternSyntaxException(ex.getLocalizedMessage(), replacementText, -1);
      }
    }
    return replacementText;
  }

  private TextChange createFileChange(IFile file, Pattern pattern, Collection<FileMatch> matches,
      RefactoringStatus resultingStatus, Collection<MatchGroup> matchGroups)
      throws PatternSyntaxException, CoreException {
    PositionTracker tracker = InternalSearchUI.getInstance().getPositionTracker();

    TextFileChange change = new TextFileChange(Messages.format(
        SearchMessages.ReplaceRefactoring_group_label_change_for_file,
        file.getName()), file);
    change.setEdit(new MultiTextEdit());

    ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
    manager.connect(file.getFullPath(), LocationKind.IFILE, null);
    try {
      ITextFileBuffer textFileBuffer = manager.getTextFileBuffer(
          file.getFullPath(),
          LocationKind.IFILE);
      if (textFileBuffer == null) {
        resultingStatus.addError(Messages.format(
            SearchMessages.ReplaceRefactoring_error_accessing_file_buffer,
            file.getName()));
        return null;
      }
      IDocument document = textFileBuffer.getDocument();
      String lineDelimiter = TextUtilities.getDefaultLineDelimiter(document);

      for (Iterator<FileMatch> iterator = matches.iterator(); iterator.hasNext();) {
        FileMatch match = iterator.next();
        int offset = match.getOffset();
        int length = match.getLength();
        Position currentPosition = tracker.getCurrentPosition(match);
        if (currentPosition != null) {
          offset = currentPosition.offset;
          if (length != currentPosition.length) {
            resultingStatus.addError(Messages.format(
                SearchMessages.ReplaceRefactoring_error_match_content_changed,
                file.getName()));
            continue;
          }
        }

        String originalText = getOriginalText(document, offset, length);
        if (originalText == null) {
          resultingStatus.addError(Messages.format(
              SearchMessages.ReplaceRefactoring_error_match_content_changed,
              file.getName()));
          continue;
        }

        String replacementString = computeReplacementString(
            pattern,
            originalText,
            fReplaceString,
            lineDelimiter);
        if (replacementString == null) {
          resultingStatus.addError(Messages.format(
              SearchMessages.ReplaceRefactoring_error_match_content_changed,
              file.getName()));
          continue;
        }

        ReplaceEdit replaceEdit = new ReplaceEdit(offset, length, replacementString);
        change.addEdit(replaceEdit);
        TextEditChangeGroup textEditChangeGroup = new TextEditChangeGroup(
            change,
            new TextEditGroup(
                SearchMessages.ReplaceRefactoring_group_label_match_replace,
                replaceEdit));
        change.addTextEditChangeGroup(textEditChangeGroup);
        matchGroups.add(new MatchGroup(textEditChangeGroup, match));
      }
    } finally {
      manager.disconnect(file.getFullPath(), LocationKind.IFILE, null);
    }
    return change;
  }

  private Pattern createSearchPattern(FileSearchQuery query) {
    return PatternConstructor.createPattern(
        query.getSearchString(),
        true,
        true,
        query.isCaseSensitive(),
        false);
  }

  private Collection<FileMatch> getBucket(IFile file) {
    HashSet<FileMatch> col = fMatches.get(file);
    if (col == null) {
      col = new HashSet<FileMatch>();
      fMatches.put(file, col);
    }
    return col;
  }

  /**
   * Checks whether the match should be included. Also collects ignored matches whose file is linked
   * to an already collected match.
   * 
   * @param match the match
   * @return <code>true</code> iff the match should be included
   */
  private boolean isMatchToBeIncluded(FileMatch match) {
    IFile file = match.getFile();
    URI uri = file.getLocationURI();
    if (uri == null) {
      return true;
    }

    for (Iterator<URI> iter = fAlreadyCollected.keySet().iterator(); iter.hasNext();) {
      if (URIUtil.equals(iter.next(), uri)) {
        if (file.equals(fAlreadyCollected.get(uri))) {
          return true; // another FileMatch for an IFile which already had matches
        }

        if (fIgnoredMatches == null) {
          fIgnoredMatches = new HashMap<URI, ArrayList<FileMatch>>();
        }

        ArrayList<FileMatch> matches = fIgnoredMatches.get(uri);
        if (matches == null) {
          matches = new ArrayList<FileMatch>();
          fIgnoredMatches.put(uri, matches);
        }
        matches.add(match);

        return false;
      }
    }

    fAlreadyCollected.put(uri, file);
    return true;
  }

}
