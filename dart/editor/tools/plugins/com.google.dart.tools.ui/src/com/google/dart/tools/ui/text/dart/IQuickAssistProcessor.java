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

import org.eclipse.core.runtime.CoreException;

/**
 * Interface to be implemented by contributors to the extension point
 * <code>org.eclipse.wst.jsdt.ui.quickAssistProcessors</code>. Provisional API: This class/interface
 * is part of an interim API that is still under development and expected to change significantly
 * before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost
 * certainly be broken (repeatedly) as the API evolves.
 */
public interface IQuickAssistProcessor {

  /**
   * Collects quick assists for the given context.
   * 
   * @param context Defines current compilation unit, position and a shared AST
   * @param locations The locations of problems at the invocation offset. The processor can decide
   *          to only add assists when there are no errors at the selection offset.
   * @return Returns the assists applicable at the location or <code>null</code> if no proposals can
   *         be offered.
   * @throws CoreException CoreException can be thrown if the operation fails
   */
  IDartCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
      throws CoreException;

  /**
   * Evaluates if quick assists can be created for the given context. This evaluation must be
   * precise.
   * 
   * @param context The invocation context
   * @return Returns <code>true</code> if quick assists can be created
   * @throws CoreException CoreException can be thrown if the operation fails
   */
  boolean hasAssists(IInvocationContext context) throws CoreException;

}
