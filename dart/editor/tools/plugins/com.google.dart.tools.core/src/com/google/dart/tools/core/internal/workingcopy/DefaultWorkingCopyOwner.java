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
package com.google.dart.tools.core.internal.workingcopy;

import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

/**
 * The unique instance of the class <code>DefaultWorkingCopyOwner</code> implement the default
 * working copy owner.
 * 
 * @coverage dart.tools.core
 */
public class DefaultWorkingCopyOwner extends WorkingCopyOwner {
  private static final DefaultWorkingCopyOwner UniqueInstance = new DefaultWorkingCopyOwner();

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static DefaultWorkingCopyOwner getInstance() {
    return UniqueInstance;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private DefaultWorkingCopyOwner() {
    super();
  }

  @Override
  public String toString() {
    return "Primary owner"; //$NON-NLS-1$
  }
}
