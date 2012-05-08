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

import com.google.dart.tools.search.ui.ISearchResult;
import com.google.dart.tools.search.ui.SearchResultEvent;

/**
 * An event object describing addition and removal of matches. Events of this class are sent when
 * <code>Match</code>es are added or removed from an <code>AbstractTextSearchResult</code>.
 * <p>
 * Clients may instantiate or subclass this class.
 * </p>
 */
public class MatchEvent extends SearchResultEvent {
  private static final long serialVersionUID = 6009335074727417445L;
  private int fKind;
  private Match[] fMatches;
  private Match[] fMatchContainer = new Match[1];
  /**
   * Constant for a matches being added.
   * 
   * @see MatchEvent#getKind()
   */
  public static final int ADDED = 1;
  /**
   * Constant for a matches being removed.
   * 
   * @see MatchEvent#getKind()
   */
  public static final int REMOVED = 2;

  private static final Match[] fgEmtpyMatches = new Match[0];

  /**
   * Constructs a new <code>MatchEvent</code>.
   * 
   * @param searchResult the search result concerned
   */
  public MatchEvent(ISearchResult searchResult) {
    super(searchResult);
  }

  /**
   * Tells whether this is a remove or an add.
   * 
   * @return one of <code>ADDED</code> or <code>REMOVED</code>
   */
  public int getKind() {
    return fKind;
  }

  /**
   * Returns the concerned matches.
   * 
   * @return the matches this event is about
   */
  public Match[] getMatches() {
    if (fMatches != null)
      return fMatches;
    else if (fMatchContainer[0] != null)
      return fMatchContainer;
    else
      return fgEmtpyMatches;
  }

  /**
   * Sets the kind of event this is.
   * 
   * @param kind the kind to set; either <code>ADDED</code> or <code>REMOVED</code>
   */
  protected void setKind(int kind) {
    fKind = kind;
  }

  /**
   * Sets the match for the change this event reports.
   * 
   * @param match the match to set
   */
  protected void setMatch(Match match) {
    fMatchContainer[0] = match;
    fMatches = null;
  }

  /**
   * Sets the matches for the change this event reports.
   * 
   * @param matches the matches to set
   */
  protected void setMatches(Match[] matches) {
    fMatchContainer[0] = null;
    fMatches = matches;
  }
}
