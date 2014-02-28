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

package com.google.dart.engine.internal.context;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.utilities.general.TimeCounter;

/**
 * Container with global {@link AnalysisContext} performance statistics.
 */
public class PerformanceStatistics {
  /**
   * The {@link TimeCounter} for time spent in Angular analysis.
   */
  public static final TimeCounter angular = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in reading files.
   */
  public static final TimeCounter io = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in scanning.
   */
  public static final TimeCounter scan = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in parsing.
   */
  public static final TimeCounter parse = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in resolving.
   */
  public static final TimeCounter resolve = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in error verifier.
   */
  public static final TimeCounter errors = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in hints generator.
   */
  public static final TimeCounter hints = new TimeCounter();
}
