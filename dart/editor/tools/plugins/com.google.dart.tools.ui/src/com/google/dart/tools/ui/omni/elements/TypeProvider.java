/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.omni.elements;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchListener;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchPatternFactory;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.omni.OmniBoxMessages;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.dialogs.SearchPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider for type elements.
 */
public class TypeProvider extends OmniProposalProvider {

  private class TypeCollector implements SearchListener {

    private final List<OmniElement> matches = new ArrayList<OmniElement>();

    public OmniElement[] getTypeArray() {
      return getTypes().toArray(EMPTY_ARRAY);
    }

    public List<OmniElement> getTypes() {
      return matches;
    }

    @Override
    public void matchFound(SearchMatch match) {
      DartElement element = match.getElement();
      if (!(element instanceof Type)) {
        return;
      }
      matches.add(new TypeElement(TypeProvider.this, (Type) element));
    }
  }

  private final IProgressMonitor progressMonitor;
  //TODO (pquitslund): support additional scopes
  private final SearchScope searchScope = SearchScopeFactory.createWorkspaceScope();

  private static OmniElement[] EMPTY_ARRAY = new OmniElement[0];

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

    TypeCollector collector = new TypeCollector();
    try {
      SearchEngine engine = SearchEngineFactory.createSearchEngine((WorkingCopyOwner) null);
      engine.searchTypeDeclarations(getSearchScope(), searchPattern, collector, progressMonitor);
    } catch (SearchException e) {
      DartToolsPlugin.log(e);
    }

    return collector.getTypeArray();

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
