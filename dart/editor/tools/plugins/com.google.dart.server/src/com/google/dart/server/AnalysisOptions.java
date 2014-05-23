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
 * A set of options controlling what kind of analysis is to be performed.
 */
public class AnalysisOptions {
  /**
   * True if the client wants Angular code to be analyzed.
   */
  Boolean analyzeAngular;

  /**
   * True if the client wants Polymer code to be analyzed.
   */
  Boolean analyzePolymer;

  /**
   * A table mapping groups of files (such as files in the SDK, files in packages, or explicitly
   * added files) to a list of the services that should be subscribed to for those files by default.
   */
  Map<SourceSetKind, List<AnalysisService>> defaultServices;

  /**
   * True if the client wants to enable support for the proposed async feature.
   */
  Boolean enableAsync;

  /**
   * True if the client wants to enable support for the proposed deferred loading feature.
   */
  Boolean enableDeferredLoading;

  /**
   * True if the client wants to enable support for the proposed enum feature.
   */
  Boolean enableEnums;

  /**
   * True if hints that are specific to dart2js should be generated. This option is ignored if
   * either provideErrors or generateHints is false.
   */
  Boolean generateDart2jsHints;

  /**
   * True is hints should be generated as part of generating errors and warnings. This option is
   * ignored if provideErrors is false.
   */
  Boolean generateHints;
}
