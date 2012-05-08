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
package com.google.dart.tools.search.ui.text;

import org.eclipse.ui.IEditorPart;

/**
 * This interface serves as an adapter between matches and editors. It is used to highlight matches
 * in editors. Search implementors who want their matches highlighted must return an implementation
 * of <code>IEditorMatchAdapter</code> from the <code>getEditorMatchAdapter()</code> method in their
 * search result subclass. It is assumed that the match adapters are stateless, and no lifecycle
 * management is provided.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @see com.google.dart.tools.search.ui.text.AbstractTextSearchResult
 */
public interface IEditorMatchAdapter {
  /**
   * Determines whether a match should be displayed in the given editor. For example, if a match is
   * reported in a file, This method should return <code>true</code>, if the given editor displays
   * the file.
   * 
   * @param match The match
   * @param editor The editor that possibly contains the matches element
   * @return whether the given match should be displayed in the editor
   */
  public abstract boolean isShownInEditor(Match match, IEditorPart editor);

  /**
   * Returns all matches that are contained in the element shown in the given editor. For example,
   * if the editor shows a particular file, all matches in that file should be returned.
   * 
   * @param result the result to search for matches
   * @param editor The editor.
   * @return All matches that are contained in the element that is shown in the given editor.
   */
  public abstract Match[] computeContainedMatches(AbstractTextSearchResult result,
      IEditorPart editor);

}
