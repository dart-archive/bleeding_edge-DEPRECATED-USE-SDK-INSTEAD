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
package com.google.dart.tools.ui.text.dart;

import com.google.dart.tools.core.model.CompilationUnit;

import org.eclipse.jface.text.contentassist.IContextInformation;

/**
 * A Dart Doc processor proposes completions and computes context information for a particular
 * content type.
 * <p>
 * This interface must be implemented by clients who extend the
 * <code>com.google.dart.tools.ui.ui.dartDocCompletionProcessor</code> extension-point
 * </p>
 * . Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public interface IDartDocCompletionProcessor {

  /**
   * Flag used by <code>computeCompletionProposals</code>. Specifies that only proposals should be
   * returned that match the case of the prefix in the code (value: <code>1</code>).
   */
  int RESTRICT_TO_MATCHING_CASE = 1;

  /**
   * Returns the completion proposals based on the specified location within the compilation unit.
   * 
   * @param cu the working copy of the compilation unit in which the completion request has been
   *          called.
   * @param offset an offset within the compilation unit for which completion proposals should be
   *          computed
   * @param length the length of the current selection.
   * @param flags settings for the code assist. Flags as defined in this interface, e.g.
   *          <code>RESTRICT_TO_MATCHING_CASE</code>.
   * @return an array of completion proposals or <code>null</code> if no proposals could be found
   */
  IDartCompletionProposal[] computeCompletionProposals(CompilationUnit cu, int offset, int length,
      int flags);

  /**
   * Returns information about possible contexts based on the specified location within the
   * compilation unit.
   * 
   * @param cu the working copy of the compilation unit which is used to compute the possible
   *          contexts
   * @param offset an offset within the compilation unit for which context information should be
   *          computed
   * @return an array of context information objects or <code>null</code> if no context could be
   *         found
   */
  IContextInformation[] computeContextInformation(CompilationUnit cu, int offset);

  /**
   * Returns the reason why this completion processor was unable to produce a completion proposals
   * or context information.
   * 
   * @return an error message or <code>null</code> if no error occurred
   */
  String getErrorMessage();
}
