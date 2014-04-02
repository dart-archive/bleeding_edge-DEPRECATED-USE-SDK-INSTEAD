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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.internal.hint.HintGenerator;
import com.google.dart.engine.source.Source;

import java.util.HashMap;

/**
 * Instances of the class {@code GenerateDartHintsTask} generate hints for a single Dart library.
 */
public class GenerateDartHintsTask extends AnalysisTask {
  /**
   * The compilation units that comprise the library, with the defining compilation unit appearing
   * first in the array.
   */
  private TimestampedData<CompilationUnit>[] units;

  /**
   * The element model for the library being analyzed.
   */
  private LibraryElement libraryElement;

  /**
   * A table mapping the sources that were analyzed to the hints that were generated for the
   * sources.
   */
  private HashMap<Source, TimestampedData<AnalysisError[]>> hintMap;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param units the compilation units that comprise the library, with the defining compilation
   *          unit appearing first in the array
   * @param libraryElement the element model for the library being analyzed
   */
  public GenerateDartHintsTask(InternalAnalysisContext context,
      TimestampedData<CompilationUnit>[] units, LibraryElement libraryElement) {
    super(context);
    this.units = units;
    this.libraryElement = libraryElement;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitGenerateDartHintsTask(this);
  }

  /**
   * Return a table mapping the sources that were analyzed to the hints that were generated for the
   * sources, or {@code null} if the task has not been performed or if the analysis did not complete
   * normally.
   * 
   * @return a table mapping the sources that were analyzed to the hints that were generated for the
   *         sources
   */
  public HashMap<Source, TimestampedData<AnalysisError[]>> getHintMap() {
    return hintMap;
  }

  /**
   * Return the element model for the library being analyzed.
   * 
   * @return the element model for the library being analyzed
   */
  public LibraryElement getLibraryElement() {
    return libraryElement;
  }

  @Override
  protected String getTaskDescription() {
    Source librarySource = libraryElement.getSource();
    if (librarySource == null) {
      return "generate Dart hints for library without source";
    }
    return "generate Dart hints for " + librarySource.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    //
    // Gather the compilation units.
    //
    int unitCount = units.length;
    CompilationUnit[] compilationUnits = new CompilationUnit[unitCount];
    for (int i = 0; i < unitCount; i++) {
      compilationUnits[i] = units[i].getData();
    }
    //
    // Analyze all of the units.
    //
    RecordingErrorListener errorListener = new RecordingErrorListener();
    HintGenerator hintGenerator = new HintGenerator(compilationUnits, getContext(), errorListener);
    hintGenerator.generateForLibrary();
    //
    // Store the results.
    //
    hintMap = new HashMap<Source, TimestampedData<AnalysisError[]>>(unitCount);
    for (int i = 0; i < unitCount; i++) {
      long modificationTime = units[i].getModificationTime();
      Source source = units[i].getData().getElement().getSource();
      AnalysisError[] errors = errorListener.getErrorsForSource(source);
      hintMap.put(source, new TimestampedData<AnalysisError[]>(modificationTime, errors));
    }
  }
}
