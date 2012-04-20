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
package com.google.dart.tools.ui.internal.search;

import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.text.Match;
import com.google.dart.tools.search.ui.text.MatchFilter;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.search.MatchPresentation;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Dart element search result.
 */
public class DartSearchResult extends AbstractDartSearchResult {

  private final Map<Object, MatchPresentation> elementsToParticipants;
  private final DartSearchQuery query;

  public DartSearchResult(DartSearchQuery query) {
    this.query = query;
    elementsToParticipants = new HashMap<Object, MatchPresentation>();
//TODO (pquitslund): implement DartMatchFilter
//    setActiveMatchFilters(DartMatchFilter.getLastUsedFilters());
  }

  @Override
  public MatchFilter[] getAllMatchFilters() {
//TODO (pquitslund): implement DartMatchFilter
//    return JavaMatchFilter.allFilters(fQuery);
    return new MatchFilter[0];
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return query.getImageDescriptor();
  }

  @Override
  public String getLabel() {
    return query.getResultLabel(getMatchCount());
  }

  @Override
  public ISearchQuery getQuery() {
    return query;
  }

  @Override
  public String getTooltip() {
    return getLabel();
  }

  @Override
  public void removeAll() {
    synchronized (this) {
      elementsToParticipants.clear();
    }
    super.removeAll();
  }

  @Override
  public void removeMatch(Match match) {
    synchronized (this) {
      if (getMatchCount(match.getElement()) == 1) {
        elementsToParticipants.remove(match.getElement());
      }
    }
    super.removeMatch(match);
  }

  @Override
  public void setActiveMatchFilters(MatchFilter[] filters) {
    super.setActiveMatchFilters(filters);
//TODO (pquitslund): implement DartMatchFilter
//    JavaMatchFilter.setLastUsedFilters(filters);
  }

  boolean addMatch(Match match, MatchPresentation participant) {
    Object element = match.getElement();
    if (elementsToParticipants.get(element) != null) {
      DartToolsPlugin.log(new Status(IStatus.WARNING, DartToolsPlugin.getPluginId(), 0,
          "A second search participant was found for an element", null)); //$NON-NLS-1$
      return false;
    }
    elementsToParticipants.put(element, participant);
    addMatch(match);
    return true;
  }

  synchronized MatchPresentation getSearchParticpant(Object element) {
    return elementsToParticipants.get(element);
  }
}
