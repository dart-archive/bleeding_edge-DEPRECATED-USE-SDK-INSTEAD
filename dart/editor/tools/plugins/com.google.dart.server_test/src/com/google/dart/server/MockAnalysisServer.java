/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server;

import java.util.List;
import java.util.Map;

/**
 * Mock used for testing. Subclasses should override whatever methods they expect to be called.
 */
public class MockAnalysisServer implements AnalysisServer {

  @Override
  public void addAnalysisServerListener(AnalysisServerListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void analysis_getErrors(String file, GetErrorsConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void analysis_getHover(String file, Integer offset, GetHoverConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void analysis_reanalyze() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void analysis_setAnalysisRoots(List<String> included, List<String> excluded) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void analysis_setPriorityFiles(List<String> files) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void analysis_setSubscriptions(Map<String, List<String>> subscriptions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void analysis_updateContent(Map<String, Object> files) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void analysis_updateOptions(AnalysisOptions options) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void completion_getSuggestions(String file, Integer offset, GetSuggestionsConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void debug_createContext(String contextRoot, CreateContextConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void debug_deleteContext(String id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void debug_mapUri(String id, String file, String uri, MapUriConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void debug_setSubscriptions(List<String> subscriptions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void edit_getAssists(String file, Integer offset, Integer length,
      GetAssistsConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void edit_getAvailableRefactorings(String file, Integer offset, Integer length,
      GetAvailableRefactoringsConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void edit_getFixes(String file, Integer offset, GetFixesConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void edit_getRefactoring(String kindId, String file, Integer offset, Integer length,
      Boolean validateOnly, Object options, GetRefactoringConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAnalysisServerListener(AnalysisServerListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void search_findElementReferences(String file, Integer offset, Boolean includePotential,
      FindElementReferencesConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void search_findMemberDeclarations(String name, FindMemberDeclarationsConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void search_findMemberReferences(String name, FindMemberReferencesConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void search_findTopLevelDeclarations(String pattern,
      FindTopLevelDeclarationsConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void search_getTypeHierarchy(String file, Integer offset, GetTypeHierarchyConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void server_getVersion(GetVersionConsumer consumer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void server_setSubscriptions(List<String> subscriptions) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void server_shutdown() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void start(long millisToRestart) throws Exception {
    throw new UnsupportedOperationException();
  }
}
