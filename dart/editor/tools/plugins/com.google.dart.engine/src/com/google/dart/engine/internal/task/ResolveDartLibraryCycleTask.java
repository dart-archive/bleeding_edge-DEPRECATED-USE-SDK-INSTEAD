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
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.resolver.LibraryResolver2;
import com.google.dart.engine.internal.resolver.ResolvableLibrary;
import com.google.dart.engine.source.Source;

import java.util.List;

/**
 * Instances of the class {@code ResolveDartLibraryTask} resolve a specific Dart library.
 */
public class ResolveDartLibraryCycleTask extends AnalysisTask {
  /**
   * The source representing the file whose compilation unit is to be returned. TODO(brianwilkerson)
   * This should probably be removed, but is being left in for now to ease the transition.
   */
  private Source unitSource;

  /**
   * The source representing the library to be resolved.
   */
  private Source librarySource;

  /**
   * The libraries that are part of the cycle containing the library to be resolved.
   */
  private List<ResolvableLibrary> librariesInCycle;

  /**
   * The library resolver holding information about the libraries that were resolved.
   */
  private LibraryResolver2 resolver;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param unitSource the source representing the file whose compilation unit is to be returned
   * @param librarySource the source representing the library to be resolved
   * @param librariesInCycle the libraries that are part of the cycle containing the library to be
   *          resolved
   */
  public ResolveDartLibraryCycleTask(InternalAnalysisContext context, Source unitSource,
      Source librarySource, List<ResolvableLibrary> librariesInCycle) {
    super(context);
    this.unitSource = unitSource;
    this.librarySource = librarySource;
    this.librariesInCycle = librariesInCycle;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitResolveDartLibraryCycleTask(this);
  }

  /**
   * Return the library resolver holding information about the libraries that were resolved.
   * 
   * @return the library resolver holding information about the libraries that were resolved
   */
  public LibraryResolver2 getLibraryResolver() {
    return resolver;
  }

  /**
   * Return the source representing the library to be resolved.
   * 
   * @return the source representing the library to be resolved
   */
  public Source getLibrarySource() {
    return librarySource;
  }

  /**
   * Return the source representing the file whose compilation unit is to be returned.
   * 
   * @return the source representing the file whose compilation unit is to be returned
   */
  public Source getUnitSource() {
    return unitSource;
  }

  @Override
  protected String getTaskDescription() {
    if (librarySource == null) {
      return "resolve library null source";
    }
    return "resolve library " + librarySource.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    resolver = new LibraryResolver2(getContext());
    resolver.resolveLibrary(librarySource, librariesInCycle);
  }
}
