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
package com.google.dart.tools.ui.omni.elements;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchFilter;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchPatternFactory;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.omni.OmniBoxMessages;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.dialogs.SearchPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider for type elements.
 */
public class TypeProvider extends OmniProposalProvider {

  /**
   * Filters out resources that have been marked as ignored.
   */
  private static class DartIgnoreFilter implements SearchFilter {
    @Override
    public boolean passes(SearchMatch match) {
      DartElement element = match.getElement();
      IResource resource = element.getResource();
      return resource == null || DartCore.isAnalyzed(resource);
    }
  }

  private static final DartIgnoreFilter IGNORE_FILTER = new DartIgnoreFilter();

  private final IProgressMonitor progressMonitor;

  //TODO (pquitslund): support additional scopes
  private final SearchScope searchScope = SearchScopeFactory.createWorkspaceScope();

  public TypeProvider(IProgressMonitor progressMonitor) {
    this.progressMonitor = progressMonitor;
  }

  @Override
  public OmniElement getElementForId(String id) {
    OmniElement[] elements = getElements(id);
    if (elements.length == 0) {
      return null;
    }
    return elements[0];
  }

  @Override
  public OmniElement[] getElements(String pattern) {

    com.google.dart.tools.core.search.SearchPattern searchPattern = null;
    SearchPattern sp = new SearchPattern();
    sp.setPattern(pattern);
    int matchRule = sp.getMatchRule();
    switch (matchRule) {
      case SearchPattern.RULE_CAMELCASE_MATCH:
        searchPattern = SearchPatternFactory.createCamelCasePattern(pattern, false);
        break;
      case SearchPattern.RULE_PATTERN_MATCH:
        searchPattern = SearchPatternFactory.createWildcardPattern(pattern, false);
        break;
      case SearchPattern.RULE_PREFIX_MATCH:
        searchPattern = SearchPatternFactory.createPrefixPattern(pattern, false);
        break;
      default:
        searchPattern = SearchPatternFactory.createExactPattern(pattern, false);
        break;
    }

    try {
      SearchEngine engine = SearchEngineFactory.createSearchEngine((WorkingCopyOwner) null);
      List<SearchMatch> matches = engine.searchTypeDeclarations(getSearchScope(), searchPattern,
          IGNORE_FILTER, progressMonitor);
      List<OmniElement> results = new ArrayList<OmniElement>(matches.size());
      for (SearchMatch match : matches) {
        DartElement element = match.getElement();
        if (element instanceof Type) {
          results.add(new TypeElement(TypeProvider.this, (Type) element));
        }
      }
      return results.toArray(new OmniElement[results.size()]);
    } catch (SearchException e) {
      DartToolsPlugin.log(e);
    }
    return new OmniElement[0];
  }

  @Override
  public String getId() {
    return getClass().getName();
  }

  @Override
  public String getName() {
    return OmniBoxMessages.OmniBox_Types;
  }

  private SearchScope getSearchScope() {
    return searchScope;
  }
}
