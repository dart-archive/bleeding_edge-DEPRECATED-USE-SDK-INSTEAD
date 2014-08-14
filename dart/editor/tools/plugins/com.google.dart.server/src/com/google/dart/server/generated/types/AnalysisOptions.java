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
 *
 * This file has been automatically generated.  Please do not edit it manually.
 * To regenerate the file, use the script "pkg/analysis_server/spec/generate_files".
 */
package com.google.dart.server.generated.types;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.google.dart.server.utilities.general.ObjectUtilities;
import org.apache.commons.lang3.StringUtils;

/**
 * A set of options controlling what kind of analysis is to be performed. If the value of a field
 * is omitted the value of the option will not be changed.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class AnalysisOptions {

  /**
   * An empty array of {@link AnalysisOptions}s.
   */
  public static final AnalysisOptions[] EMPTY_ARRAY = new AnalysisOptions[0];

  /**
   * True if the client wants to enable support for the proposed async feature.
   */
  private final boolean enableAsync;

  /**
   * True if the client wants to enable support for the proposed deferred loading feature.
   */
  private final boolean enableDeferredLoading;

  /**
   * True if the client wants to enable support for the proposed enum feature.
   */
  private final boolean enableEnums;

  /**
   * True if hints that are specific to dart2js should be generated. This option is ignored if
   * generateHints is false.
   */
  private final boolean generateDart2jsHints;

  /**
   * True is hints should be generated as part of generating errors and warnings.
   */
  private final boolean generateHints;

  /**
   * Constructor for {@link AnalysisOptions}.
   */
  public AnalysisOptions(boolean enableAsync, boolean enableDeferredLoading, boolean enableEnums, boolean generateDart2jsHints, boolean generateHints) {
    this.enableAsync = enableAsync;
    this.enableDeferredLoading = enableDeferredLoading;
    this.enableEnums = enableEnums;
    this.generateDart2jsHints = generateDart2jsHints;
    this.generateHints = generateHints;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AnalysisOptions) {
      AnalysisOptions other = (AnalysisOptions) obj;
      return
        other.enableAsync == enableAsync &&
        other.enableDeferredLoading == enableDeferredLoading &&
        other.enableEnums == enableEnums &&
        other.generateDart2jsHints == generateDart2jsHints &&
        other.generateHints == generateHints;
    }
    return false;
  }

  /**
   * True if the client wants to enable support for the proposed async feature.
   */
  public boolean getEnableAsync() {
    return enableAsync;
  }

  /**
   * True if the client wants to enable support for the proposed deferred loading feature.
   */
  public boolean getEnableDeferredLoading() {
    return enableDeferredLoading;
  }

  /**
   * True if the client wants to enable support for the proposed enum feature.
   */
  public boolean getEnableEnums() {
    return enableEnums;
  }

  /**
   * True if hints that are specific to dart2js should be generated. This option is ignored if
   * generateHints is false.
   */
  public boolean getGenerateDart2jsHints() {
    return generateDart2jsHints;
  }

  /**
   * True is hints should be generated as part of generating errors and warnings.
   */
  public boolean getGenerateHints() {
    return generateHints;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("enableAsync=");
    builder.append(enableAsync + ", ");
    builder.append("enableDeferredLoading=");
    builder.append(enableDeferredLoading + ", ");
    builder.append("enableEnums=");
    builder.append(enableEnums + ", ");
    builder.append("generateDart2jsHints=");
    builder.append(generateDart2jsHints + ", ");
    builder.append("generateHints=");
    builder.append(generateHints + ", ");
    builder.append("]");
    return builder.toString();
  }

}
