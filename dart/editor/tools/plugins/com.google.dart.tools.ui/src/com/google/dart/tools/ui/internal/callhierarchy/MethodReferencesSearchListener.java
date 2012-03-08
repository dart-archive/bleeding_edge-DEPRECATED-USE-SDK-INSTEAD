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
package com.google.dart.tools.ui.internal.callhierarchy;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.search.SearchListener;
import com.google.dart.tools.core.search.SearchMatch;

import java.util.Map;

public class MethodReferencesSearchListener implements SearchListener {

  private CallSearchResultCollector searchResults = new CallSearchResultCollector();

  public Map<String, MethodCall> getCallers() {
    return searchResults.getCallers();
  }

  @Override
  public void matchFound(SearchMatch match) {
    DartElement element = match.getElement();
    SourceRange range = match.getSourceRange();
    switch (element.getElementType()) {
      case DartElement.FIELD:
      case DartElement.METHOD:
      case DartElement.FUNCTION:
      case DartElement.FUNCTION_TYPE_ALIAS:
        searchResults.addMember(element, element, range.getOffset(),
            range.getOffset() + range.getLength());
    }
  }

  @Override
  public void searchComplete() {
    // Ignored
  }
}
