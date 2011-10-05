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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

/**
 * Instances of the class <code>DartModelStatus</code> represent the outcome of a Dart model
 * operation. Status objects are used inside <code>DartModelException</code> objects to indicate
 * what went wrong.
 * <p>
 * Dart model status object are distinguished by their plug-in id: <code>getPlugin</code> returns
 * <code>"com.google.dart.tools.core"</code>. <code>getCode</code> returns one of the status codes
 * declared in <code>DartModelStatusConstants</code>.
 * </p>
 * <p>
 * A Dart model status may also carry additional information (that is, in addition to the
 * information defined in <code>IStatus</code>):
 * <ul>
 * <li>elements - optional handles to Dart elements associated with the failure</li>
 * <li>string - optional string associated with the failure</li>
 * </ul>
 */
public interface DartModelStatus extends IStatus {
  /**
   * Return an array containing any Dart elements associated with the failure (see specification of
   * the status code), or an empty array if no elements are related to this particular status code.
   * 
   * @return the list of Dart element culprits
   * @see DartModelStatusConstants
   */
  public DartElement[] getElements();

  /**
   * Return the path associated with the failure (see specification of the status code), or
   * <code>null</code> if the failure does not include path information.
   * 
   * @return the path that caused the failure, or <code>null</code> if none
   */
  public IPath getPath();

  /**
   * Return <code>true</code> if this status indicates that a Dart model element does not exist.
   * This convenience method is equivalent to
   * <code>getCode() == DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST</code>.
   * 
   * @return <code>true</code> if the status code indicates that a Dart model element does not exist
   * @see DartModelStatusConstants#ELEMENT_DOES_NOT_EXIST
   */
  public boolean isDoesNotExist();
}
