/*
 * Copyright (c) 2012, the Dart project authors.
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

package com.google.dart.tools.debug.core.util;

import org.eclipse.debug.core.model.IVariable;

/**
 * A sub-class of IVariable that adds additional meta-information about the variable.
 */
public interface IDartDebugVariable extends IVariable {

  public boolean isLibraryObject();

  public boolean isLocal();

  public boolean isStatic();

  public boolean isThisObject();

  /**
   * @return whether this variable represents a thrown exception
   */
  public boolean isThrownException();

}
