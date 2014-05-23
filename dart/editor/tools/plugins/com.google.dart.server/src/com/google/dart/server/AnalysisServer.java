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

//  /**
//   * Inform the specified context that the specified sources should be analyzed as indicated.
//   * 
//   * @param contextId the identifier of the context to be notified
//   * @param delta indications of what analysis should be performed
//   */
//  public void applyAnalysisDelta(String contextId, AnalysisDelta delta);
//
//  /**
//   * Inform the specified context that the changes encoded in the change set have been made. Any
//   * invalidated analysis results will be flushed from the context.
//   * 
//   * @param contextId the identifier of the context to which the changes are to be applied
//   * @param changeSet the change set to be applied
//   */
//  public void applyChanges(String contextId, ChangeSet changeSet);
//
//  /**
//   * Performs the final validation and computes a change to apply the specific refactoring. This
//   * method may be invoked several times, for example after changing options using
//   * {@link #setRefactoringExtractLocalOptions(String, boolean, String)}. When done,
//   * {@link #deleteRefactoring(String)} should be invoked.
//   * 
//   * @param refactoringId the identifier of the refactoring to apply
//   * @param consumer the results listener
//   */
//  public void applyRefactoring(String refactoringId, RefactoringApplyConsumer consumer);
//
//  /**
//   * Computes code completion suggestions at the given position in the {@link Source}. The given
//   * consumer is invoked asynchronously on a different thread.
//   * 
//   * @param contextId the identifier of the context to compute suggestions within
//   * @param source the {@link Source} to perform completion within
//   * @param offset the offset within the {@code source}
//   * @param consumer the results listener
//   */
//  public void computeCompletionSuggestions(String contextId, Source source, int offset,
//      CompletionSuggestionsConsumer consumer);
//
//  /**
//   * Computes the set of fixes that are available for problems related to the given error. The given
//   * consumer is invoked asynchronously on a different thread.
//   * 
//   * @param contextId the identifier of the context in which the source was analyzed
//   * @param errors the errors for which fixes are being requested
//   * @param consumer the results listener
//   */
//  public void computeFixes(String contextId, AnalysisError[] errors, FixesConsumer consumer);
//
//  /**
//   * Computes a set of minor refactorings which can be performed at the given offset in the given
//   * {@link Source}. The given consumer is invoked asynchronously on a different thread.
//   * 
//   * @param contextId the identifier of the context to perform refactorings within
//   * @param source the {@link Source} to perform refactorings within
//   * @param offset the offset within the {@code source}
//   * @param length the length of the selected code within the {@code source}
//   * @param consumer the results listener
//   */
//  public void computeMinorRefactorings(String contextId, Source source, int offset, int length,
//      MinorRefactoringsConsumer consumer);
//
//  /**
//   * Computes a type hierarchy for the given {@link Element} - class or member. The given consumer
//   * is invoked asynchronously on a different thread.
//   * 
//   * @param contextId the identifier of the context to compute hierarchy within
//   * @param element the {@link Element} to compute hierarchy for
//   * @param consumer the results listener
//   */
//  public void computeTypeHierarchy(String contextId, Element element, TypeHierarchyConsumer consumer);
//
//  /**
//   * Create a new context in which analysis can be performed. The context that is created will
//   * persist until {@link #deleteContext(String)} is used to delete it. Clients, therefore, are
//   * responsible for managing the lifetime of contexts.
//   * 
//   * @param name the name of the context, used for debugging purposes
//   * @param sdkDirectory the path to the root directory of the Dart SDK
//   * @param packageMap a table mapping package names to the path of the directory containing the
//   *          package
//   * @return an identifier used to identify the context that was created
//   */
//  public String createContext(String name, String sdkDirectory, Map<String, String> packageMap);
//
//  /**
//   * Create a new "Extract Local" refactoring. The refactoring that is created will persist until
//   * {@link #deleteRefactoring(String)} is used to delete it. Clients, therefore, are responsible
//   * for managing the lifetime of refactorings.
//   * 
//   * @param contextId the identifier of the context to create refactoring within
//   * @param source the {@link Source} to create refactoring within
//   * @param offset the offset within the {@code source}
//   * @param length the length of the selected code within the {@code source}
//   * @param consumer the results listener
//   */
//  public void createRefactoringExtractLocal(String contextId, Source source, int offset,
//      int length, RefactoringExtractLocalConsumer consumer);
//
//  /**
//   * Create a new "Extract Method" refactoring. The refactoring that is created will persist until
//   * {@link #deleteRefactoring(String)} is used to delete it. Clients, therefore, are responsible
//   * for managing the lifetime of refactorings.
//   * 
//   * @param contextId the identifier of the context to create refactoring within
//   * @param source the {@link Source} to create refactoring within
//   * @param offset the offset within the {@code source}
//   * @param length the length of the selected code within the {@code source}
//   * @param consumer the results listener
//   */
//  public void createRefactoringExtractMethod(String contextId, Source source, int offset,
//      int length, RefactoringExtractMethodConsumer consumer);
//
//  /**
//   * Delete the context with the given id. Future attempts to use the context id will result in an
//   * error being returned.
//   * 
//   * @param contextId the identifier of the context to be deleted
//   */
//  public void deleteContext(String contextId);
//
//  /**
//   * Delete the refactoring with the given id. Future attempts to use the refactoring id will result
//   * in an error being returned.
//   * 
//   * @param refactoringId the identifier of the refactoring to be deleted
//   */
//  public void deleteRefactoring(String refactoringId);
//
//  /**
//   * Reports with a set of {@link ErrorCode}s for which server may be able to {@link #computeFixes}
//   * in the given context.
//   * 
//   * @param contextId the identifier of the context
//   * @param consumer the results listener
//   */
//  public void getFixableErrorCodes(String contextId, FixableErrorCodesConsumer consumer);

  /**
   * Return the version number of the analysis server.
   * 
   * @param consumer the results listener
   */
  public void getVersion(VersionConsumer consumer);

  /**
   * Remove the given listener from the list of listeners that will receive notification when new
   * analysis results become available.
   * 
   * @param listener the listener to be removed
   */
  public void removeAnalysisServerListener(AnalysisServerListener listener);

//  /**
//   * Searches for declarations of class members with the given name. The given consumer is invoked
//   * asynchronously on a different thread.
//   * 
//   * @param name the name of a member
//   * @param consumer the results listener
//   */
//  public void searchClassMemberDeclarations(String name, SearchResultsConsumer consumer);
//
//  /**
//   * Searches for resolved and unresolved references to class members with the given name. The given
//   * consumer is invoked asynchronously on a different thread.
//   * 
//   * @param name the name of a member
//   * @param consumer the results listener
//   */
//  public void searchClassMemberReferences(String name, SearchResultsConsumer consumer);
//
//  /**
//   * Searches for references to the given element.
//   * <p>
//   * If the given element is a class member, then also references to all corresponding members in
//   * the class hierarchy are searched.
//   * <p>
//   * If the given element is a class member and {@code withPotential} is {@code true}, then
//   * potential references should also be reported.
//   * <p>
//   * The given consumer is invoked asynchronously on a different thread.
//   * 
//   * @param element the element to find references to, not {@code null}
//   * @param withPotential is {@code true} if potential references should also be reported
//   * @param consumer the results listener
//   */
//  public void searchElementReferences(Element element, boolean withPotential,
//      SearchResultsConsumer consumer);
//
//  /**
//   * Searches the given context for declarations of top-level elements with names matching the given
//   * pattern. The given consumer is invoked asynchronously on a different thread.
//   * 
//   * @param contextId the context to search declarations in, {@code null} to search in the universe
//   * @param pattern the regular expression to match names against, not {@code null}
//   * @param consumer the results listener
//   */
//  public void searchTopLevelDeclarations(String contextId, String pattern,
//      SearchResultsConsumer consumer);
//
//  /**
//   * Set the options controlling analysis within a context to the given set of options.
//   * 
//   * @param contextId the identifier of the context to which the options are to be applied
//   * @param options the options to be applied
//   */
//  public void setOptions(String contextId, AnalysisOptions options);
//
//  /**
//   * Set the priority sources in the specified context to the sources in the given array.
//   * 
//   * @param contextId the identifier of the context to which the priority sources are to be applied
//   * @param sources the sources to be given priority over other sources
//   */
//  public void setPrioritySources(String contextId, Source[] sources);
//
//  /**
//   * Set the options for the "Extract Local" refactoring instance.
//   * 
//   * @param refactoringId the identifier of the refactoring to which the options are to be applied
//   * @param allOccurrences is {@code true} if all of the expression occurrences should be extracted
//   * @param name the name of the variable
//   * @param consumer the results listener
//   */
//  public void setRefactoringExtractLocalOptions(String refactoringId, boolean allOccurrences,
//      String name, RefactoringOptionsValidationConsumer consumer);
//
//  /**
//   * Set the options for the "Extract Method" refactoring instance.
//   * 
//   * @param refactoringId the identifier of the refactoring to which the options are to be applied
//   * @param name the name of the method to extract
//   * @param asGetter is {@code true} if a getter should be extracted instead of a regular method
//   * @param allOccurrences is {@code true} if all of the expression occurrences should be extracted
//   * @param parameters the parameters of the extracted method
//   * @param consumer the results listener
//   */
//  public void setRefactoringExtractMethodOptions(String refactoringId, String name,
//      boolean asGetter, boolean allOccurrences, Parameter[] parameters,
//      RefactoringExtractMethodOptionsValidationConsumer consumer);

  /**
   * Cleanly shutdown the analysis server.
   */
  public void shutdown();

//  /**
//   * Updates subscriptions for one or more notifications.
//   * <p>
//   * The source sets associated with notification kinds that are not included in the map are not
//   * changed.
//   * 
//   * @param contextId the identifier of the context for which the subscriptions are to be updated
//   * @param subscriptions a table mapping notification kinds to the source sets
//   */
//  public void subscribe(String contextId, Map<NotificationKind, SourceSet> subscriptions);
}
