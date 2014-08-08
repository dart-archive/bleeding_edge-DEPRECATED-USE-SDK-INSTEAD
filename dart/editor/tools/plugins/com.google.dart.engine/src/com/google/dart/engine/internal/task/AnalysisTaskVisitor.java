/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.context.AnalysisException;

/**
 * The interface {@code AnalysisTaskVisitor} defines the behavior of objects that can visit tasks.
 * While tasks are not structured in any interesting way, this class provides the ability to
 * dispatch to an appropriate method.
 */
public interface AnalysisTaskVisitor<E> {
  /**
   * Visit a {@link GenerateDartErrorsTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitGenerateDartErrorsTask(GenerateDartErrorsTask task) throws AnalysisException;

  /**
   * Visit a {@link GenerateDartHintsTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitGenerateDartHintsTask(GenerateDartHintsTask task) throws AnalysisException;

  /**
   * Visit a {@link GetContentTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitGetContentTask(GetContentTask task) throws AnalysisException;

  /**
   * Visit an {@link IncrementalAnalysisTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitIncrementalAnalysisTask(IncrementalAnalysisTask incrementalAnalysisTask)
      throws AnalysisException;

  /**
   * Visit a {@link ParseDartTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitParseDartTask(ParseDartTask task) throws AnalysisException;

  /**
   * Visit a {@link ParseHtmlTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitParseHtmlTask(ParseHtmlTask task) throws AnalysisException;

  /**
   * Visit a {@link PolymerBuildHtmlTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitPolymerBuildHtmlTask(PolymerBuildHtmlTask task) throws AnalysisException;

  /**
   * Visit a {@link PolymerResolveHtmlTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitPolymerResolveHtmlTask(PolymerResolveHtmlTask task) throws AnalysisException;

  /**
   * Visit a {@link ResolveAngularComponentTemplateTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitResolveAngularComponentTemplateTask(ResolveAngularComponentTemplateTask task)
      throws AnalysisException;

  /**
   * Visit a {@link ResolveAngularEntryHtmlTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitResolveAngularEntryHtmlTask(ResolveAngularEntryHtmlTask task)
      throws AnalysisException;

  /**
   * Visit a {@link ResolveDartLibraryCycleTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitResolveDartLibraryCycleTask(ResolveDartLibraryCycleTask task)
      throws AnalysisException;

  /**
   * Visit a {@link ResolveDartLibraryTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitResolveDartLibraryTask(ResolveDartLibraryTask task) throws AnalysisException;

  /**
   * Visit a {@link ResolveDartUnitTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitResolveDartUnitTask(ResolveDartUnitTask task) throws AnalysisException;

  /**
   * Visit a {@link ResolveHtmlTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitResolveHtmlTask(ResolveHtmlTask task) throws AnalysisException;

  /**
   * Visit a {@link ScanDartTask}.
   * 
   * @param task the task to be visited
   * @return the result of visiting the task
   * @throws AnalysisException if the visitor throws an exception for some reason
   */
  public E visitScanDartTask(ScanDartTask task) throws AnalysisException;
}
