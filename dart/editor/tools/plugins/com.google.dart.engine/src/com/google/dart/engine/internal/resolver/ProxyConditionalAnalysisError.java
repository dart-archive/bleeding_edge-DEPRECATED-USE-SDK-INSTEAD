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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.error.AnalysisError;

/**
 * This class is a wrapper for an {@link AnalysisError} which can also be queried after resolution
 * to find out if the error should actually be reported. In this case, these errors are conditional
 * on the non-existence of an {@code @proxy} annotation.
 * <p>
 * If we have other conditional error codes in the future, we should have this class implement some
 * ConditionalErrorCode so that after resolution, a list of ConditionalErrorCode can be visited
 * instead of multiple lists of *ConditionalErrorCodes.
 */
public class ProxyConditionalAnalysisError {
  /**
   * The enclosing {@link ClassElement}, this is what will determine if the error code should, or
   * should not, be generated on the source.
   */
  private Element enclosingElement;

  /**
   * The conditional analysis error.
   */
  private AnalysisError analysisError;

  /**
   * Instantiate a new {@link ProxyConditionalAnalysisError} with some enclosing element and the
   * conditional analysis error.
   * 
   * @param enclosingElement the enclosing element
   * @param analysisError the conditional analysis error
   */
  public ProxyConditionalAnalysisError(Element enclosingElement, AnalysisError analysisError) {
    this.enclosingElement = enclosingElement;
    this.analysisError = analysisError;
  }

  /**
   * Return the analysis error.
   * 
   * @return the analysis error
   */
  public AnalysisError getAnalysisError() {
    return analysisError;
  }

  /**
   * Return {@code true} iff the enclosing class has the proxy annotation.
   * 
   * @return {@code true} iff the enclosing class has the proxy annotation
   */
  public boolean shouldIncludeErrorCode() {
    if (enclosingElement instanceof ClassElement) {
      return !((ClassElement) enclosingElement).isOrInheritsProxy();
    }
    return true;
  }

}
