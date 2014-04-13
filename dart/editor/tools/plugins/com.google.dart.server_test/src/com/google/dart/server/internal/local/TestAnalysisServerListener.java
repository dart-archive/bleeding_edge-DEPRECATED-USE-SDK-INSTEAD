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

package com.google.dart.server.internal.local;

import com.google.common.collect.Maps;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.Outline;

import junit.framework.AssertionFailedError;

import java.util.Map;

/**
 * Mock implementation of {@link AnalysisServerListener}.
 */
public class TestAnalysisServerListener implements AnalysisServerListener {
  private final Map<Source, AnalysisError[]> sourcesErrors = Maps.newHashMap();

  /**
   * Assert that the number of errors that have been gathered matches the number of errors that are
   * given and that they have the expected error codes. The order in which the errors were gathered
   * is ignored.
   * 
   * @param source the source to check errors for
   * @param expectedErrorCodes the error codes of the errors that should have been gathered
   * @throws AssertionFailedError if a different number of errors have been gathered than were
   *           expected
   */
  public void assertErrorsWithCodes(Source source, ErrorCode... expectedErrorCodes) {
    GatheringErrorListener listener = new GatheringErrorListener();
    AnalysisError[] errors = sourcesErrors.get(source);
    if (errors != null) {
      for (AnalysisError error : errors) {
        listener.onError(error);
      }
    }
    listener.assertErrorsWithCodes(expectedErrorCodes);
  }

  @Override
  public void computedErrors(String contextId, Source source, AnalysisError[] errors) {
    sourcesErrors.put(source, errors);
  }

  @Override
  public void computedHighlights(String contextId, Source source, HighlightRegion[] highlights) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void computedNavigation(String contextId, Source source, NavigationRegion[] targets) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void computedOutline(String contextId, Source source, Outline outline) {
    throw new UnsupportedOperationException();
  }
}
