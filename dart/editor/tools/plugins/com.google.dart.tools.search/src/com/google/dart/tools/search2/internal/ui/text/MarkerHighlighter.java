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
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.text.Match;
import com.google.dart.tools.search2.internal.ui.InternalSearchUI;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.core.filebuffers.IFileBuffer;

import org.eclipse.jface.text.Position;

public class MarkerHighlighter extends Highlighter {
  private IFile fFile;
  private Map<Match, IMarker> fMatchesToAnnotations;

  public MarkerHighlighter(IFile file) {
    fFile = file;
    fMatchesToAnnotations = new HashMap<Match, IMarker>();
  }

  public void addHighlights(final Match[] matches) {
    try {
      SearchPlugin.getWorkspace().run(new IWorkspaceRunnable() {
        public void run(IProgressMonitor monitor) throws CoreException {
          for (int i = 0; i < matches.length; i++) {
            IMarker marker;
            marker = createMarker(matches[i]);
            if (marker != null)
              fMatchesToAnnotations.put(matches[i], marker);
          }
        }
      }, fFile, IWorkspace.AVOID_UPDATE, null);
    } catch (CoreException e) {
      // just log the thing. There's nothing we can do anyway.
      SearchPlugin.log(e.getStatus());
    }
  }

  private IMarker createMarker(Match match) throws CoreException {
    Position position = InternalSearchUI.getInstance().getPositionTracker().getCurrentPosition(
        match);
    if (position == null) {
      if (match.getOffset() < 0 || match.getLength() < 0)
        return null;
      position = new Position(match.getOffset(), match.getLength());
    } else {
      // need to clone position, can't have it twice in a document.
      position = new Position(position.getOffset(), position.getLength());
    }
    IMarker marker = match.isFiltered() ? fFile.createMarker(SearchPlugin.FILTERED_SEARCH_MARKER)
        : fFile.createMarker(NewSearchUI.SEARCH_MARKER);
    HashMap<String, Integer> attributes = new HashMap<String, Integer>(4);
    if (match.getBaseUnit() == Match.UNIT_CHARACTER) {
      attributes.put(IMarker.CHAR_START, new Integer(position.getOffset()));
      attributes.put(IMarker.CHAR_END, new Integer(position.getOffset() + position.getLength()));
    } else {
      attributes.put(IMarker.LINE_NUMBER, new Integer(position.getOffset()));
    }
    marker.setAttributes(attributes);
    return marker;
  }

  public void removeHighlights(Match[] matches) {
    for (int i = 0; i < matches.length; i++) {
      IMarker marker = fMatchesToAnnotations.remove(matches[i]);
      if (marker != null) {
        try {
          marker.delete();
        } catch (CoreException e) {
          // just log the thing. There's nothing we can do anyway.
          SearchPlugin.log(e.getStatus());
        }
      }
    }
  }

  public void removeAll() {
    try {
      fFile.deleteMarkers(NewSearchUI.SEARCH_MARKER, true, IResource.DEPTH_INFINITE);
      fFile.deleteMarkers(SearchPlugin.FILTERED_SEARCH_MARKER, true, IResource.DEPTH_INFINITE);
      fMatchesToAnnotations.clear();
    } catch (CoreException e) {
      // just log the thing. There's nothing we can do anyway.
      SearchPlugin.log(e.getStatus());
    }
  }

  protected void handleContentReplaced(IFileBuffer buffer) {
    if (!buffer.getLocation().equals(fFile.getFullPath()))
      return;
    Match[] matches = new Match[fMatchesToAnnotations.keySet().size()];
    fMatchesToAnnotations.keySet().toArray(matches);
    removeAll();
    addHighlights(matches);
  }
}
