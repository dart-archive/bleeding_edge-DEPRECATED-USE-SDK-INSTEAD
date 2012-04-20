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
package com.google.dart.tools.ui.search;

import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.PartInitException;

/**
 * This interface serves to display elements that a search participant has contributed to a search
 * result.
 * <p>
 * Each {@link MatchPresentation} is associated with a particular {@link QueryParticipant}. The
 * {@link MatchPresentation} will only be asked to handle elements and matches which its
 * {@link QueryParticipant} contributed to the search result. If two search participants report
 * matches against the same element, one of them will be chosen to handle the element.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface MatchPresentation {
  /**
   * Creates a new instance of a label provider for elements that have been contributed to a search
   * result by the corresponding query participant. The search view will call this method when it
   * needs to render elements and will dispose the label providers when it is done with them. This
   * method may therefore be called multiple times.
   * 
   * @return A label provider for elements found by the corresponding query participant.
   */
  ILabelProvider createLabelProvider();

  /**
   * Opens an editor on the given element and selects the given range of text. The location of
   * matches are automatically updated when a file is edited through the file buffer infrastructure
   * (see {@link org.eclipse.core.filebuffers.ITextFileBufferManager}). When a file buffer is saved,
   * the current positions are written back to the match. If the <code>activate</code> parameter is
   * <code>true</code> the opened editor should have be activated. Otherwise the focus should not be
   * changed.
   * 
   * @param match The match to show.
   * @param currentOffset The current start offset of the match.
   * @param currentLength The current length of the selection.
   * @param activate Whether to activate the editor the match is shown in.
   * @throws PartInitException If an editor can't be opened.
   */
  void showMatch(Match match, int currentOffset, int currentLength, boolean activate)
      throws PartInitException;
}
