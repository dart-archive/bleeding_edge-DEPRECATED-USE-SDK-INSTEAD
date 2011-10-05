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

import org.eclipse.core.runtime.CoreException;

/**
 * Interface to be implemented by contributors to the extension point
 * <code>org.eclipse.wst.jsdt.ui.quickFixProcessors</code>.
 * <p>
 * Since 3.2, each extension specifies the marker types it can handle, and
 * {@link #hasCorrections(CompilationUnit, int)} and
 * {@link #getCorrections(IInvocationContext, IProblemLocation[])} are called if (and only if) quick
 * fix is required for a problem of these types.
 * </p>
 * <p>
 * Note, if a extension does not specify marker types it will be only called for problem of type
 * <code>org.eclipse.wst.jsdt.core.problem</code>,
 * <code>org.eclipse.wst.jsdt.core.buildpath_problem</code> and
 * <code>org.eclipse.wst.jsdt.core.task</code>; compatible with the behavior prior to 3.2
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public interface IQuickFixProcessor {

  /**
   * Collects corrections or code manipulations for the given context.
   * 
   * @param context Defines current compilation unit, position and a shared AST
   * @param locations Problems are the current location.
   * @return the corrections applicable at the location or <code>null</code> if no proposals can be
   *         offered
   * @throws CoreException CoreException can be thrown if the operation fails
   */
  IDartCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
      throws CoreException;

  /**
   * Returns <code>true</code> if the processor has proposals for the given problem. This test
   * should be an optimistic guess and be very cheap.
   * 
   * @param unit the compilation unit
   * @param problemId the problem Id. The id is of a problem of the problem type(s) this processor
   *          specified in the extension point.
   * @return <code>true</code> if the processor has proposals for the given problem
   */
  boolean hasCorrections(CompilationUnit unit, int problemId);

}
