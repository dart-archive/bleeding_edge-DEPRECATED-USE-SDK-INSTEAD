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
package com.google.dart.tools.ui.internal.text.dart;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Extension to <code>IProblemRequestor</code>.
 */
public interface IProblemRequestorExtension {

  /**
   * Informs the problem requestor that a sequence of reportings is about to start. While a sequence
   * is active, multiple peering calls of <code>beginReporting</code> and <code>endReporting</code>
   * can appear.
   */
  void beginReportingSequence();

  /**
   * Informs the problem requestor that the sequence of reportings has been finished.
   */
  void endReportingSequence();

  /**
   * Sets the active state of this problem requestor.
   * 
   * @param isActive the state of this problem requestor
   */
  void setIsActive(boolean isActive);

  /**
   * Tells the problem requestor to handle temporary problems.
   * 
   * @param enable <code>true</code> if temporary problems are handled
   */
  void setIsHandlingTemporaryProblems(boolean enable);

  /**
   * Sets the progress monitor to this problem requestor.
   * 
   * @param monitor the progress monitor to be used
   */
  void setProgressMonitor(IProgressMonitor monitor);
}
