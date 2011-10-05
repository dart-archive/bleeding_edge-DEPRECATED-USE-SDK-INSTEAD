/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.model;

import com.google.dart.tools.core.completion.CompletionRequestor;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The interface <code>CodeAssistElement</code> defines the behavior common to Dart elements that
 * support source code assist and code resolve.
 */
public interface CodeAssistElement {
  /**
   * Perform code completion at the given offset position in this compilation unit, reporting
   * results to the given completion requestor. The <code>offset</code> is the 0-based index of the
   * character, after which code assist is desired. An <code>offset</code> of -1 indicates to code
   * assist at the beginning of this compilation unit.
   * 
   * @param offset the given offset position
   * @param requestor the given completion requestor
   * @throws DartModelException if code assist could not be performed. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST)</li>
   *           <li>The position specified is < -1 or is greater than this compilation unit's source
   *           length (INDEX_OUT_OF_BOUNDS)
   *           </ul>
   * @throws IllegalArgumentException if <code>requestor</code> is <code>null</code>
   */
  public void codeComplete(int offset, CompletionRequestor requestor) throws DartModelException;

  /**
   * Perform code completion at the given offset position in this compilation unit, reporting
   * results to the given completion requestor. The <code>offset</code> is the 0-based index of the
   * character, after which code assist is desired. An <code>offset</code> of -1 indicates to code
   * assist at the beginning of this compilation unit.
   * <p>
   * If {@link IProgressMonitor} is not <code>null</code> then some proposals which can be very long
   * to compute are proposed. To avoid that the code assist operation take too much time a
   * {@link IProgressMonitor} which automatically cancel the code assist operation when a specified
   * amount of time is reached could be used.
   * 
   * <pre>
   * new IProgressMonitor() {
   *     private final static int TIMEOUT = 500; //ms
   *     private long endTime;
   *     public void beginTask(String name, int totalWork) {
   *         fEndTime= System.currentTimeMillis() + TIMEOUT;
   *     }
   *     public boolean isCanceled() {
   *         return endTime <= System.currentTimeMillis();
   *     }
   *     ...
   * };
   * </pre>
   * 
   * @param offset the given offset position
   * @param requestor the given completion requestor
   * @param monitor the progress monitor used to report progress
   * @throws DartModelException if code assist could not be performed. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li> <li> The position
   *           specified is < -1 or is greater than this compilation unit's source length
   *           (INDEX_OUT_OF_BOUNDS)
   *           </ul>
   * @throws IllegalArgumentException if <code>requestor</code> is <code>null</code>
   */
  public void codeComplete(int offset, CompletionRequestor requestor, IProgressMonitor monitor)
      throws DartModelException;

  /**
   * Perform code completion at the given offset position in this compilation unit, reporting
   * results to the given completion requestor. The <code>offset</code> is the 0-based index of the
   * character, after which code assist is desired. An <code>offset</code> of -1 indicates to code
   * assist at the beginning of this compilation unit. It considers types in the working copies with
   * the given owner first. In other words, the owner's working copies will take precedence over
   * their original compilation units in the workspace.
   * <p>
   * Note that if a working copy is empty, it will be as if the original compilation unit had been
   * deleted.
   * 
   * @param offset the given offset position
   * @param requestor the given completion requestor
   * @param owner the owner of working copies that take precedence over their original compilation
   *          units
   * @throws DartModelException if code assist could not be performed. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li> <li> The position
   *           specified is < -1 or is greater than this compilation unit's source length
   *           (INDEX_OUT_OF_BOUNDS)
   *           </ul>
   * @throws IllegalArgumentException if <code>requestor</code> is <code>null</code>
   */
  public void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner owner)
      throws DartModelException;

  /**
   * Perform code completion at the given offset position in this compilation unit, reporting
   * results to the given completion requestor. The <code>offset</code> is the 0-based index of the
   * character, after which code assist is desired. An <code>offset</code> of -1 indicates to code
   * assist at the beginning of this compilation unit. It considers types in the working copies with
   * the given owner first. In other words, the owner's working copies will take precedence over
   * their original compilation units in the workspace.
   * <p>
   * Note that if a working copy is empty, it will be as if the original compilation unit had been
   * deleted.
   * <p>
   * If {@link IProgressMonitor} is not <code>null</code> then some proposals which can be very long
   * to compute are proposed. To avoid that the code assist operation take too much time a
   * {@link IProgressMonitor} which automatically cancel the code assist operation when a specified
   * amount of time is reached could be used.
   * 
   * <pre>
   * new IProgressMonitor() {
   *     private final static int TIMEOUT = 500; //ms
   *     private long endTime;
   *     public void beginTask(String name, int totalWork) {
   *         fEndTime= System.currentTimeMillis() + TIMEOUT;
   *     }
   *     public boolean isCanceled() {
   *         return endTime <= System.currentTimeMillis();
   *     }
   *     ...
   * };
   * </pre>
   * 
   * @param offset the given offset position
   * @param requestor the given completion requestor
   * @param owner the owner of working copies that take precedence over their original compilation
   *          units
   * @param monitor the progress monitor used to report progress
   * @throws DartModelException if code assist could not be performed. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li> <li> The position
   *           specified is < -1 or is greater than this compilation unit's source length
   *           (INDEX_OUT_OF_BOUNDS)
   *           </ul>
   * @throws IllegalArgumentException if <code>requestor</code> is <code>null</code>
   */
  public void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner owner,
      IProgressMonitor monitor) throws DartModelException;

  /**
   * Return the Dart elements corresponding to the given selected text in this compilation unit. The
   * <code>offset</code> is the 0-based index of the first selected character. The
   * <code>length</code> is the number of selected characters.
   * <p>
   * Note that if the <code>length</code> is 0 and the <code>offset</code> is inside an identifier
   * or the index just after an identifier then this identifier is considered as the selection.
   * 
   * @param offset the given offset position
   * @param length the number of selected characters
   * @return the Dart elements corresponding to the given selected text
   * @throws DartModelException if code resolve could not be performed. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li> <li> The range
   *           specified is not within this element's source range (INDEX_OUT_OF_BOUNDS)
   *           </ul>
   */
  public DartElement[] codeSelect(int offset, int length) throws DartModelException;

  /**
   * Return the Dart elements corresponding to the given selected text in this compilation unit. The
   * <code>offset</code> is the 0-based index of the first selected character. The
   * <code>length</code> is the number of selected characters. It considers types in the working
   * copies with the given owner first. In other words, the owner's working copies will take
   * precedence over their original compilation units in the workspace.
   * <p>
   * Note that if the <code>length</code> is 0 and the <code>offset</code> is inside an identifier
   * or the index just after an identifier then this identifier is considered as the selection.
   * <p>
   * Note that if a working copy is empty, it will be as if the original compilation unit had been
   * deleted.
   * 
   * @param offset the given offset position
   * @param length the number of selected characters
   * @param owner the owner of working copies that take precedence over their original compilation
   *          units
   * @return the Dart elements corresponding to the given selected text
   * @throws DartModelException if code resolve could not be performed. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li> <li> The range
   *           specified is not within this element's source range (INDEX_OUT_OF_BOUNDS)
   *           </ul>
   */
  public DartElement[] codeSelect(int offset, int length, WorkingCopyOwner owner)
      throws DartModelException;
}
