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
package com.google.dart.engine.context;

import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code ObsoleteSourceAnalysisException} represent an analysis attempt that
 * failed because a source was deleted between the time the analysis started and the time the
 * results of the analysis were ready to be recorded.
 */
public class ObsoleteSourceAnalysisException extends AnalysisException {
  /**
   * The source that was removed while it was being analyzed.
   */
  private Source source;

  /**
   * Initialize a newly created exception to represent the removal of the given source.
   * 
   * @param source the source that was removed while it was being analyzed
   */
  public ObsoleteSourceAnalysisException(Source source) {
    super("The source '" + source.getFullName() + "' was removed while it was being analyzed");
    this.source = source;
  }

  /**
   * Return the source that was removed while it was being analyzed.
   * 
   * @return the source that was removed
   */
  public Source getSource() {
    return source;
  }
}
