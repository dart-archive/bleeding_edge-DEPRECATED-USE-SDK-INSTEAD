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

/**
 * The interface <code>SourceReference</code> defines the behavior common to Dart elements that have
 * associated source code. This set consists of <code>CompilationUnit</code>, <code>Type</code>,
 * <code>Field</code>, and <code>Method</code>.
 * <p>
 * Source reference elements may be working copies if they were created from a compilation unit that
 * is a working copy.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface SourceReference {
  /**
   * Return <code>true</code> if this element exists in the model.
   * 
   * @return <code>true</code> if this element exists in the Dart model
   */
  public boolean exists();

  /**
   * Return the name range associated with this element.
   * <p>
   * If the element is a {@link Member}, it returns the source range of this member's simple name,
   * or <code>null</code> if this member does not have a name.
   * 
   * @return the name range associated with this element, or <code>null</code> if not available
   * @throws DartModelException if the source range cannot be determined
   */
  public SourceRange getNameRange() throws DartModelException;

  /**
   * Return the source code associated with this element. This extracts the substring from the
   * source buffer containing this source element. This corresponds to the source range that would
   * be returned by <code>getSourceRange</code>.
   * 
   * @return the source code, or <code>null</code> if this element has no associated source code
   * @throws DartModelException if an exception occurs while accessing its corresponding resource
   */
  public String getSource() throws DartModelException;

  /**
   * Return the source range associated with this element.
   * 
   * @return the source range, or <code>null</code> if this element has no associated source code
   * @throws DartModelException if an exception occurs while accessing its corresponding resource
   */
  public SourceRange getSourceRange() throws DartModelException;
}
