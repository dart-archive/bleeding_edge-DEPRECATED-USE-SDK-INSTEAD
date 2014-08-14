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

package com.google.dart.server.internal;

import com.google.dart.server.ErrorFixes;
import com.google.dart.server.SourceChange;
import com.google.dart.server.generated.types.AnalysisError;

import org.apache.commons.lang3.StringUtils;

/**
 * An implementation of {@link ErrorFixes}.
 * 
 * @coverage dart.server
 */
public class ErrorFixesImpl implements ErrorFixes {
  private final AnalysisError analysisError;
  private final SourceChange[] fixes;

  public ErrorFixesImpl(AnalysisError analysisError, SourceChange[] fixes) {
    this.analysisError = analysisError;
    this.fixes = fixes;
  }

  @Override
  public AnalysisError getError() {
    return analysisError;
  }

  @Override
  public SourceChange[] getFixes() {
    return fixes;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[error=");
    builder.append(analysisError.toString());
    builder.append(", fixes=");
    builder.append(StringUtils.join(fixes, ", "));
    builder.append("]");
    return builder.toString();
  }
}
