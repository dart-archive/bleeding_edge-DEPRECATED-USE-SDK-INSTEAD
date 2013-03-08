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
package com.google.dart.engine.element;

/**
 * The interface {@code ShowCombinator} defines the behavior of combinators that cause some of the
 * names in a namespace to be visible (and the rest hidden) when being imported.
 * 
 * @coverage dart.engine.element
 */
public interface ShowCombinator extends NamespaceCombinator {
  /**
   * Return an array containing the names that are to be made visible in the importing library if
   * they are defined in the imported library.
   * 
   * @return the names from the imported library that are visible in the importing library
   */
  public String[] getShownNames();
}
