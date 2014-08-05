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
 * The interface {@code AnalysisServer} defines the behavior of objects that interface to an
 * analysis server.
 * 
 * @coverage dart.server
 */
public interface AnalysisServer {

  /**
   * Add the given listener to the list of listeners that will receive notification when new
   * analysis results become available.
   * 
   * @param listener the listener to be added
   */
  public void addAnalysisServerListener(AnalysisServerListener listener);

  /**
   * Performs the final validation and computes a change to apply the specific refactoring. This
   * method may be invoked several times, for example after changing options using
   * {@link #setRefactoringExtractLocalOptions(String, boolean, String)}. When done,
   * {@link #deleteRefactoring(String)} should be invoked.
   * 
   * @param refactoringId the identifier of the refactoring to apply
   * @param consumer the results listener
   */
  public void applyRefactoring(String refactoringId, RefactoringApplyConsumer consumer);

  /**
   * Create a debugging context for the executable file with the given path. The context that is
   * created will persist until debug.deleteContext is used to delete it. Clients, therefore, are
   * responsible for managing the lifetime of debugging contexts.
   * 
   * @param contextRoot the path of the Dart or HTML file that will be launched
   * @param consumer the results listener
   */
  public void createDebugContext(String contextRoot, DebugCreateContextConsumer consumer);

  /**
   * Create a refactoring operation that can be applied at a later time. The operation that is
   * created will persist until either {@code edit.applyRefactoring} or
   * {@code edit.deleteRefactoring} is used to delete it. Clients, therefore, are responsible for
   * managing the lifetime of refactoring operations.
   * 
   * @param refactoringKind the refactoring kind
   * @param file the file to create refactoring within
   * @param offset the offset within the file
   * @param length the length of the selected code within the file
   * @param consumer the results listener
   */
  public void createRefactoring(String refactoringKind, String file, int offset, int length,
      RefactoringCreateConsumer consumer);

  /**
   * Delete the debugging context with the given identifier. The context id is no longer valid after
   * this command. The server is allowed to re-use ids when they are no longer valid.
   * 
   * @param contextRoot the path of the Dart or HTML file that will be launched
   * @param consumer the results listener
   */
  public void deleteDebugContext(String id);

  /**
   * Delete the refactoring with the given id. Future attempts to use the refactoring id will result
   * in an error being returned.
   * 
   * @param refactoringId the identifier of the refactoring to be deleted
   */
  public void deleteRefactoring(String refactoringId);

  /**
   * Computes the set of assists that are available at the given location. An assist is
   * distinguished from a refactoring primarily by the fact that it affects a single file and does
   * not require user input in order to be performed. The given consumer is invoked asynchronously
   * on a different thread.
   * 
   * @param file the file containing the range for which assists are being requested
   * @param offset the offset of the code for which assists are being requested
   * @param length the length of the code for which assists are being requested
   * @param consumer the results listener
   */
  public void getAssists(String file, int offset, int length, AssistsConsumer consumer);

  /**
   * Computes code completion id for the given position in the file. The given consumer is invoked
   * asynchronously on a different thread.
   * 
   * @param file the file containing the point at which suggestions are to be made
   * @param offset the offset within the {@code source}
   * @param consumer the results listener
   */
  public void getCompletionSuggestions(String file, int offset, CompletionIdConsumer consumer);

  /**
   * Return the errors associated with the given file. If the errors for the given file have not yet
   * been computed, or the most recently computed errors for the given file are out of date, then
   * the response for this request will be delayed until they have been computed. If some or all of
   * the errors for the file cannot be computed, then the subset of the errors that can be computed
   * will be returned and the response will contain an error to indicate why the errors could not be
   * computed.
   * <p>
   * This request is intended to be used by clients that cannot asynchronously apply updated error
   * information. Clients that <b>can</b> apply error information as it becomes available should use
   * the information provided by the 'analysis.errors' notification.
   * 
   * @param file the file for which errors are being requested
   * @param consumer the errors consumer
   */
  public void getErrors(String file, AnalysisErrorsConsumer consumer);

  /**
   * Return the set of fixes that are available for the errors at a given offset in a given file.
   * 
   * @param file the file in which hover text is being requested
   * @param offset the offset in the source used to determine hover text
   * @param consumer the results listener
   */
  public void getFixes(String file, int offset, FixesConsumer consumer);

  /**
   * Computes the hover text to be displayed at the given location. The given consumer is invoked
   * asynchronously on a different thread.
   * 
   * @param file the file in which hover text is being requested
   * @param offset the offset in the source used to determine hover text
   * @param consumer the results listener
   */
  public void getHover(String file, int offset, HoverConsumer consumer);

  /**
   * Get a list of the kinds of refactorings that are valid for the given selection in the given
   * file.
   * 
   * @param file the file containing the code on which the refactoring would be based
   * @param offset the offset of the code on which the refactoring would be based
   * @param length the length of the code on which the refactoring would be based
   * @param consumer the results listener
   */
  public void getRefactorings(String file, int offset, int length, RefactoringGetConsumer consumer);

  /**
   * Computes a type hierarchy at the given location. The given consumer is invoked asynchronously
   * on a different thread.
   * 
   * @param file the file in which hierarchy is being requested
   * @param offset the offset at which hierarchy is being requested
   * @param consumer the results listener
   */
  public void getTypeHierarchy(String file, int offset, TypeHierarchyConsumer consumer);

  /**
   * Return the version number of the analysis server.
   * 
   * @param consumer the results listener
   */
  public void getVersion(VersionConsumer consumer);

  /**
   * Map a URI from the debugging context to the file that it corresponds to, or map a file to the
   * URI that it corresponds to in the debugging context.
   * <p>
   * Exactly one of the file and uri fields must be provided.
   * 
   * @param id the identifier of the debugging context in which the URI is to be mapped
   * @param file the path of the file to be mapped into a URI
   * @param uri the URI to be mapped into a file path
   * @param consumer the results listener
   */
  public void mapUri(String id, String file, String uri, MapUriConsumer consumer);

  /**
   * Force the re-analysis of everything contained in the existing analysis roots. This will cause
   * all previously computed analysis results to be discarded and recomputed, and will cause all
   * subscribed notifications to be re-sent.
   */
  public void reanalyze();

  /**
   * Remove the given listener from the list of listeners that will receive notification when new
   * analysis results become available.
   * 
   * @param listener the listener to be removed
   */
  public void removeAnalysisServerListener(AnalysisServerListener listener);

  /**
   * Searches for declarations of class members with the given name. The given consumer is invoked
   * asynchronously on a different thread.
   * 
   * @param name the name of a member
   * @param consumer the search id consumer
   */
  public void searchClassMemberDeclarations(String name, SearchIdConsumer consumer);

  /**
   * Searches for resolved and unresolved references to class members with the given name. The given
   * consumer is invoked asynchronously on a different thread.
   * 
   * @param name the name of a member
   * @param consumer the search id consumer
   */
  public void searchClassMemberReferences(String name, SearchIdConsumer consumer);

  /**
   * Perform a search for references to the element defined or referenced at the given offset in the
   * given file.
   * <p>
   * If the element is a class member, then also references to all corresponding members in the
   * class hierarchy are searched.
   * <p>
   * If the element is a class member and {@code includePotential} is {@code true}, then potential
   * references should also be reported.
   * <p>
   * The given consumer is invoked asynchronously on a different thread.
   * 
   * @param file the file containing the declaration of or a reference to the element used to define
   *          the search
   * @param offset the offset within the file of the declaration of or reference to the element
   * @param includePotential is {@code true} if potential matches are to be included in the results
   * @param consumer the search id consumer
   */
  public void searchElementReferences(String file, int offset, boolean includePotential,
      SearchIdConsumer consumer);

  /**
   * Searches the given context for declarations of top-level elements with names matching the given
   * pattern. The given consumer is invoked asynchronously on a different thread.
   * 
   * @param pattern the regular expression to match names against, not {@code null}
   * @param consumer the search id consumer
   */
  public void searchTopLevelDeclarations(String pattern, SearchIdConsumer consumer);

  /**
   * Sets the root paths used to determine which files to analyze. The set of files to be analyzed
   * are all of the files in one of the included paths that are not also in one of the excluded
   * paths.
   * 
   * @param includedPaths a list of the files and directories that should be analyzed
   * @param excludedPaths a list of the files and directories within the included directories that
   *          should <em>not</em> be analyzed
   */
  public void setAnalysisRoots(List<String> includedPaths, List<String> excludedPaths);

  /**
   * Subscribe for services. All previous subscriptions are replaced by the current set of
   * subscriptions. If a given service is not included as a key in the map then no files will be
   * subscribed to the service, exactly as if the service had been included in the map with an
   * explicit empty list of files.
   * 
   * @param subscriptions a list of the services being subscribed to.
   */
  public void setAnalysisSubscriptions(Map<AnalysisService, List<String>> subscriptions);

  /**
   * Subscribe for services. All previous subscriptions are replaced by the given set of services.
   * <p>
   * It is an error if any of the elements in the list are not valid services. If there is an error,
   * then the current subscriptions will remain unchanged.
   * 
   * @param services a list of the services being subscribed to
   */
  public void setDebugSubscriptions(List<DebugService> services);

  /**
   * Set the priority files to the files in the given list. A priority file is a file that is given
   * priority when scheduling which analysis work to do first. The list typically contains those
   * files that are visible to the user and those for which analysis results will have the biggest
   * impact on the user experience.
   * 
   * @param files the files that are to be a priority for analysis
   */
  public void setPriorityFiles(List<String> files);

  /**
   * Set the options for a refactoring operation. Clients are required to set the options before the
   * refactoring is applied if the refactoring has options. Clients are allowed to set the options
   * multiple times in order to allow users to fix any problems that might prevent the refactoring
   * from completing.
   * 
   * @param refactoringId the identifier of the refactoring whose options are to be set
   * @param refactoringOptions options for this refactoring kind
   * @param consumer the results listener
   */
  public void setRefactoringOptions(String refactoringId, Map<String, Object> refactoringOptions,
      RefactoringSetOptionsConsumer consumer);

  /**
   * Subscribe for server services.
   * <p>
   * All previous subscriptions are replaced by the given set of subscriptions.
   * 
   * @param subscriptions a list of the services being subscribed to.
   */
  public void setServerSubscriptions(List<ServerService> subscriptions);

  /**
   * Cleanly shutdown the analysis server.
   */
  public void shutdown();

  /**
   * Start the analysis server.
   * 
   * @param millisToRestart the number of milliseconds to wait for an unresponsive server before
   *          restarting it, or zero if the server should not be restarted.
   */
  public void start(long millisToRestart) throws Exception;

  /**
   * Update the options controlling analysis based on the given set of options. Any options that are
   * {@code null} will not be changed. If there are options that are not valid an error will be
   * reported but the values of the valid options will still be updated.
   * 
   * @param options the options that are to control analysis
   */
  public void updateAnalysisOptions(AnalysisOptions options);

  /**
   * Update the content of one or more files. Files that were previously updated but not included in
   * this update remain unchanged.
   * 
   * @param files a table mapping the files whose content has changed to a description of the
   *          content
   */
  public void updateContent(Map<String, ContentChange> files);
}
