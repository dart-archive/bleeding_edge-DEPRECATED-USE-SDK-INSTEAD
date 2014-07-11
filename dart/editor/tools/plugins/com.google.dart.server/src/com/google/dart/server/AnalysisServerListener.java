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

/**
 * The interface {@code AnalysisServerListener} defines the behavior of objects that listen for
 * results from an analysis server.
 * 
 * @coverage dart.server
 */
public interface AnalysisServerListener {
  /**
   * A new collection of completions have been computed for the given completion id.
   * 
   * @param completionId the id associated with the completion
   * @param completions the completion suggestions being reported
   * @param last {@code true} if this is the last set of results that will be returned for the
   *          indicated completion
   */
  public void computedCompletion(String completionId, CompletionSuggestion[] completions,
      boolean last);

  /**
   * Reports the errors associated with a given file.
   * 
   * @param file the file containing the errors
   * @param errors the errors contained in the file
   */
  public void computedErrors(String file, AnalysisError[] errors);

  /**
   * A new collection of highlight regions has been computed for the given file. Each highlight
   * region represents a particular syntactic or semantic meaning associated with some range. Note
   * that the highlight regions that are returned can overlap other highlight regions if there is
   * more than one meaning associated with a particular region.
   * 
   * @param file the file containing the highlight regions
   * @param highlights the highlight regions contained in the file
   */
  public void computedHighlights(String file, HighlightRegion[] highlights);

  /**
   * A new collection of navigation regions has been computed for the given file. Each navigation
   * region represents a list of targets associated with some range. The lists will usually contain
   * a single target, but can contain more in the case of a part that is included in multiple
   * libraries or an Dart code that is compiled against multiple versions of a package. Note that
   * the navigation regions that are returned do not overlap other navigation regions.
   * 
   * @param file the file containing the navigation regions
   * @param highlights the highlight regions associated with the source
   */
  public void computedNavigation(String file, NavigationRegion[] targets);

  /**
   * A new collection of occurrences that been computed for the given file. Each occurrences object
   * represents a list of occurrences for some element in the file.
   * 
   * @param file the file containing the occurrences
   * @param occurrencesArray the array of occurrences in the passed file
   */
  public void computedOccurrences(String file, Occurrences[] occurrencesArray);

  /**
   * A new outline has been computed for the given file.
   * 
   * @param file the file with which the outline is associated
   * @param outline the outline associated with the file
   */
  public void computedOutline(String file, Outline outline);

  /**
   * A new collection of overrides that have been computed for a given file. Each override array
   * represents a list of overrides for some file.
   * 
   * @param file the file with which the outline is associated
   * @param overrides the overrides associated with the file
   */
  public void computedOverrides(String file, OverrideMember[] overrides);

  /**
   * A new collection of search results have been computed for the given completion id.
   * 
   * @param searchId the id associated with the search
   * @param results the search results being reported
   * @param last {@code true} if this is the last set of results that will be returned for the
   *          indicated search
   */
  public void computedSearchResults(String searchId, SearchResult[] results, boolean last);

  /**
   * Reports that the server is running. This notification is issued once after the server has
   * started running to let the client know that it started correctly.
   */
  public void serverConnected();

  /**
   * An error happened in the {@link AnalysisServer}.
   * 
   * @param isFatal {@code true} if the error is a fatal error, meaning that the server will
   *          shutdown automatically after sending this notification
   * @param message the error message indicating what kind of error was encountered
   * @param stackTrace the stack trace associated with the generation of the error, used for
   *          debugging the server
   */
  public void serverError(boolean isFatal, String message, String stackTrace);

  /**
   * Reports the current status of the server.
   * 
   * @param status the current status of the server
   */
  public void serverStatus(ServerStatus status);
}
