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
}
