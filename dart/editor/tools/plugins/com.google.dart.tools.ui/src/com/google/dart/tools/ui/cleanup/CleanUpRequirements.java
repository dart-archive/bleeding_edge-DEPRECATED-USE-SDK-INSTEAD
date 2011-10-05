/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.cleanup;

import org.eclipse.core.runtime.Assert;

import java.util.Map;

/**
 * Specifies the requirements of a clean up.
 * 
 * @since 3.5
 */
public final class CleanUpRequirements {

  private final boolean fRequiresAST;

  private final Map<String, String> fCompilerOptions;

  private final boolean fRequiresFreshAST;

  private final boolean fRequiresChangedRegions;

  /**
   * Create a new instance
   * 
   * @param requiresAST <code>true</code> if an AST is required
   * @param requiresFreshAST <code>true</code> if a fresh AST is required
   * @param requiresChangedRegions <code>true</code> if changed regions are required
   * @param compilerOptions map of compiler options or <code>null</code> if no requirements
   */
  public CleanUpRequirements(boolean requiresAST, boolean requiresFreshAST,
      boolean requiresChangedRegions, Map<String, String> compilerOptions) {
    Assert.isLegal(!requiresFreshAST || requiresAST,
        "Must not request fresh AST if no AST is required"); //$NON-NLS-1$
    Assert.isLegal(compilerOptions == null || requiresAST,
        "Must not provide options if no AST is required"); //$NON-NLS-1$
    fRequiresAST = requiresAST;
    fRequiresFreshAST = requiresFreshAST;
    fCompilerOptions = compilerOptions;
    fRequiresChangedRegions = requiresChangedRegions;
  }

  /**
   * Required compiler options.
   * 
   * @return the compiler options map or <code>null</code> if none
   * @see JavaCore
   */
  public Map<String, String> getCompilerOptions() {
    return fCompilerOptions;
  }

  /**
   * Tells whether the clean up requires an AST.
   * <p>
   * <strong>Note:</strong> This should return <code>false</code> whenever possible because creating
   * an AST is expensive.
   * </p>
   * 
   * @return <code>true</code> if the {@linkplain CleanUpContext context} must provide an AST
   */
  public boolean requiresAST() {
    return fRequiresAST;
  }

  /**
   * Tells whether this clean up requires to be informed about changed regions. The changed regions
   * are the regions which have been changed between the last save state of the compilation unit and
   * its current state.
   * <p>
   * Has only an effect if the clean up is used as save action.
   * </p>
   * <p>
   * <strong>Note:</strong>: This should return <code>false</code> whenever possible because
   * calculating the changed regions is expensive.
   * </p>
   * 
   * @return <code>true</code> if the {@linkplain CleanUpContext context} must provide changed
   *         regions
   */
  public boolean requiresChangedRegions() {
    return fRequiresChangedRegions;
  }

  /**
   * Tells whether a fresh AST, containing all the changes from previous clean ups, will be needed.
   * 
   * @return <code>true</code> if the caller needs an up to date AST
   */
  public boolean requiresFreshAST() {
    return fRequiresFreshAST;
  }

}
