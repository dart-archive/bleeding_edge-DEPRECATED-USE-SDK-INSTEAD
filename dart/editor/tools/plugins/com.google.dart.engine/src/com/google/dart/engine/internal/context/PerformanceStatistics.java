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
   * The {@link TimeCounter} for time spent in reading files.
   */
  public static TimeCounter io = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in scanning.
   */
  public static TimeCounter scan = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in parsing.
   */
  public static TimeCounter parse = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in resolving.
   */
  public static TimeCounter resolve = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in Angular analysis.
   */
  public static TimeCounter angular = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in Polymer analysis.
   */
  public static TimeCounter polymer = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in error verifier.
   */
  public static TimeCounter errors = new TimeCounter();

  /**
   * The {@link TimeCounter} for time spent in hints generator.
   */
  public static TimeCounter hints = new TimeCounter();

  /**
   * Reset all of the time counters to zero.
   */
  public static void reset() {
    io = new TimeCounter();
    scan = new TimeCounter();
    parse = new TimeCounter();
    resolve = new TimeCounter();
    angular = new TimeCounter();
    polymer = new TimeCounter();
    errors = new TimeCounter();
    hints = new TimeCounter();
  }
}
