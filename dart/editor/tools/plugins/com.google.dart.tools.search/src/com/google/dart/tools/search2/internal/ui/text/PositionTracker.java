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
package com.google.dart.tools.search2.internal.ui.text;

import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.ui.IQueryListener;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.ISearchResultListener;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.SearchResultEvent;
import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;
import com.google.dart.tools.search.ui.text.IFileMatchAdapter;
import com.google.dart.tools.search.ui.text.Match;
import com.google.dart.tools.search.ui.text.MatchEvent;
import com.google.dart.tools.search.ui.text.RemoveAllEvent;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PositionTracker implements IQueryListener, ISearchResultListener, IFileBufferListener {

  private interface IFileBufferMatchOperation {
    void run(ITextFileBuffer buffer, Match match);
  }

  public static Position convertToCharacterPosition(Position linePosition, IDocument doc)
      throws BadLocationException {
    int lineOffset = linePosition.getOffset();
    int lineLength = linePosition.getLength();

    int charOffset = doc.getLineOffset(lineOffset);
    int charLength = 0;
    if (lineLength > 0) {
      int lastLine = lineOffset + lineLength - 1;
      int endPosition = doc.getLineOffset(lastLine) + doc.getLineLength(lastLine);
      charLength = endPosition - charOffset;
    }
    return new Position(charOffset, charLength);
  }

  public static Position convertToLinePosition(Position pos, IDocument doc)
      throws BadLocationException {
    int offset = doc.getLineOfOffset(pos.getOffset());
    int end = doc.getLineOfOffset(pos.getOffset() + pos.getLength());
    int lineLength = end - offset;
    if (pos.getLength() > 0 && lineLength == 0) {
      // if the character length is > 0, add the last line, too
      lineLength++;
    }
    return new Position(offset, lineLength);
  }

  private Map<Match, Position> fMatchesToPositions = new HashMap<Match, Position>();

  private Map<Match, AbstractTextSearchResult> fMatchesToSearchResults = new HashMap<Match, AbstractTextSearchResult>();

  private Map<ITextFileBuffer, Set<Match>> fFileBuffersToMatches = new HashMap<ITextFileBuffer, Set<Match>>();

  public PositionTracker() {
    NewSearchUI.addQueryListener(this);
    FileBuffers.getTextFileBufferManager().addFileBufferListener(this);
  }

  @Override
  public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {
    // not interesting for us.
  }

  @Override
  public void bufferContentReplaced(IFileBuffer buffer) {
    final int[] trackCount = new int[1];
    doForExistingMatchesIn(buffer, new IFileBufferMatchOperation() {
      @Override
      public void run(ITextFileBuffer textBuffer, Match match) {
        trackCount[0]++;
        AbstractTextSearchResult result = fMatchesToSearchResults.get(match);
        untrackPosition(textBuffer, match);
        trackPosition(result, textBuffer, match);
      }
    });
  }

  // IFileBufferListener implementation ---------------------------------------------------------------------
  @Override
  public void bufferCreated(IFileBuffer buffer) {
    final int[] trackCount = new int[1];
    if (!(buffer instanceof ITextFileBuffer)) {
      return;
    }

    IFile file = FileBuffers.getWorkspaceFileAtLocation(buffer.getLocation());
    if (file == null) {
      return;
    }

    ISearchQuery[] queries = NewSearchUI.getQueries();
    for (int i = 0; i < queries.length; i++) {
      ISearchResult result = queries[i].getSearchResult();
      if (result instanceof AbstractTextSearchResult) {
        AbstractTextSearchResult textResult = (AbstractTextSearchResult) result;
        IFileMatchAdapter adapter = textResult.getFileMatchAdapter();
        if (adapter != null) {
          Match[] matches = adapter.computeContainedMatches(textResult, file);
          for (int j = 0; j < matches.length; j++) {
            trackCount[0]++;
            trackPosition((AbstractTextSearchResult) result, (ITextFileBuffer) buffer, matches[j]);
          }
        }
      }
    }
  }

  @Override
  public void bufferDisposed(IFileBuffer buffer) {
    final int[] trackCount = new int[1];
    doForExistingMatchesIn(buffer, new IFileBufferMatchOperation() {
      @Override
      public void run(ITextFileBuffer textBuffer, Match match) {
        trackCount[0]++;
        untrackPosition(textBuffer, match);
      }
    });
  }

  @Override
  public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty) {
    if (isDirty) {
      return;
    }
    final int[] trackCount = new int[1];
    doForExistingMatchesIn(buffer, new IFileBufferMatchOperation() {
      @Override
      public void run(ITextFileBuffer textBuffer, Match match) {
        trackCount[0]++;
        Position pos = fMatchesToPositions.get(match);
        if (pos != null) {
          if (pos.isDeleted()) {
            AbstractTextSearchResult result = fMatchesToSearchResults.get(match);
            // might be that the containing element has been removed.
            if (result != null) {
              result.removeMatch(match);
            }
            untrackPosition(textBuffer, match);
          } else {
            if (match.getBaseUnit() == Match.UNIT_LINE) {
              try {
                pos = convertToLinePosition(pos, textBuffer.getDocument());
              } catch (BadLocationException e) {
                SearchPlugin.getDefault().getLog().log(
                    new Status(IStatus.ERROR, SearchPlugin.getID(), 0, e.getLocalizedMessage(), e));
              }
            }
            match.setOffset(pos.getOffset());
            match.setLength(pos.getLength());
          }
        }
      }
    });
  }

  public void dispose() {
    NewSearchUI.removeQueryListener(this);
    FileBuffers.getTextFileBufferManager().removeFileBufferListener(this);
  }

  public Position getCurrentPosition(Match match) {
    Position pos = fMatchesToPositions.get(match);
    if (pos == null) {
      return pos;
    }
    AbstractTextSearchResult result = fMatchesToSearchResults.get(match);
    if (match.getBaseUnit() == Match.UNIT_LINE && result != null) {
      ITextFileBuffer fb = getTrackedFileBuffer(result, match.getElement());
      if (fb != null) {
        IDocument doc = fb.getDocument();
        try {
          pos = convertToLinePosition(pos, doc);
        } catch (BadLocationException e) {

        }
      }
    }

    return pos;
  }

  // tracking search results --------------------------------------------------------------
  @Override
  public void queryAdded(ISearchQuery query) {
    if (query.getSearchResult() instanceof AbstractTextSearchResult) {
      query.getSearchResult().addListener(this);
    }
  }

  @Override
  public void queryFinished(ISearchQuery query) {
    // not interested
  }

  @Override
  public void queryRemoved(ISearchQuery query) {
    ISearchResult result = query.getSearchResult();
    if (result instanceof AbstractTextSearchResult) {
      untrackAll((AbstractTextSearchResult) result);
      result.removeListener(this);
    }
  }

  @Override
  public void queryStarting(ISearchQuery query) {
    // not interested here
  }

  // tracking matches ---------------------------------------------------------------------
  @Override
  public void searchResultChanged(SearchResultEvent e) {
    if (e instanceof MatchEvent) {
      MatchEvent evt = (MatchEvent) e;
      Match[] matches = evt.getMatches();
      int kind = evt.getKind();
      AbstractTextSearchResult result = (AbstractTextSearchResult) e.getSearchResult();
      for (int i = 0; i < matches.length; i++) {
        ITextFileBuffer fb = getTrackedFileBuffer(result, matches[i].getElement());
        if (fb != null) {
          updateMatch(matches[i], fb, kind, result);
        }
      }
    } else if (e instanceof RemoveAllEvent) {
      RemoveAllEvent evt = (RemoveAllEvent) e;
      ISearchResult result = evt.getSearchResult();
      untrackAll((AbstractTextSearchResult) result);
    }
  }

  @Override
  public void stateChangeFailed(IFileBuffer buffer) {
    // not interesting for us.
  }

  @Override
  public void stateChanging(IFileBuffer buffer) {
    // not interesting for us
  }

  @Override
  public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {
    // not interesting for us.
  }

  @Override
  public void underlyingFileDeleted(IFileBuffer buffer) {
    // not interesting for us.
  }

  @Override
  public void underlyingFileMoved(IFileBuffer buffer, IPath path) {
    // not interesting for us.
  }

  private void addFileBufferMapping(ITextFileBuffer fb, Match match) {
    Set<Match> matches = fFileBuffersToMatches.get(fb);
    if (matches == null) {
      matches = new HashSet<Match>();
      fFileBuffersToMatches.put(fb, matches);
    }
    matches.add(match);
  }

  private void doForExistingMatchesIn(IFileBuffer buffer, IFileBufferMatchOperation operation) {
    if (!(buffer instanceof ITextFileBuffer)) {
      return;
    }
    Set<Match> matches = fFileBuffersToMatches.get(buffer);
    if (matches != null) {
      HashSet<Match> matchSet = new HashSet<Match>(matches);
      for (Iterator<Match> matchIterator = matchSet.iterator(); matchIterator.hasNext();) {
        Match element = matchIterator.next();
        operation.run((ITextFileBuffer) buffer, element);
      }
    }
  }

  private ITextFileBuffer getTrackedFileBuffer(AbstractTextSearchResult result, Object element) {
    IFileMatchAdapter adapter = result.getFileMatchAdapter();
    if (adapter == null) {
      return null;
    }
    IFile file = adapter.getFile(element);
    if (file == null) {
      return null;
    }
    if (!file.exists()) {
      return null;
    }
    return FileBuffers.getTextFileBufferManager().getTextFileBuffer(
        file.getFullPath(),
        LocationKind.IFILE);
  }

  private void removeFileBufferMapping(ITextFileBuffer fb, Match match) {
    Set<Match> matches = fFileBuffersToMatches.get(fb);
    if (matches != null) {
      matches.remove(match);
      if (matches.size() == 0) {
        fFileBuffersToMatches.remove(fb);
      }
    }
  }

  private void trackPosition(AbstractTextSearchResult result, ITextFileBuffer fb, Match match) {
    int offset = match.getOffset();
    int length = match.getLength();
    if (offset < 0 || length < 0) {
      return;
    }

    try {
      IDocument doc = fb.getDocument();
      Position position = new Position(offset, length);
      if (match.getBaseUnit() == Match.UNIT_LINE) {
        position = convertToCharacterPosition(position, doc);
      }
      doc.addPosition(position);
      fMatchesToSearchResults.put(match, result);
      fMatchesToPositions.put(match, position);
      addFileBufferMapping(fb, match);
    } catch (BadLocationException e) {
      // the match is outside the document
      result.removeMatch(match);
    }
  }

  private void untrackAll(AbstractTextSearchResult result) {
    Set<Match> matchSet = new HashSet<Match>(fMatchesToPositions.keySet());
    for (Iterator<Match> matches = matchSet.iterator(); matches.hasNext();) {
      Match match = matches.next();
      AbstractTextSearchResult matchContainer = fMatchesToSearchResults.get(match);
      if (result.equals(matchContainer)) {
        ITextFileBuffer fb = getTrackedFileBuffer(result, match.getElement());
        if (fb != null) {
          untrackPosition(fb, match);
        }
      }
    }
  }

  private void untrackPosition(ITextFileBuffer fb, Match match) {
    Position position = fMatchesToPositions.get(match);
    if (position != null) {
      removeFileBufferMapping(fb, match);
      fMatchesToSearchResults.remove(match);
      fMatchesToPositions.remove(match);
      fb.getDocument().removePosition(position);
    }
  }

  private void updateMatch(Match match, ITextFileBuffer fb, int kind,
      AbstractTextSearchResult result) {
    if (kind == MatchEvent.ADDED) {
      trackPosition(result, fb, match);
    } else if (kind == MatchEvent.REMOVED) {
      untrackPosition(fb, match);
    }
  }

}
