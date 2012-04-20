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
package com.google.dart.tools.internal.corext.refactoring.reorg;

/**
 * @coverage dart.editor.ui.refactoring.core
 */
public interface IReorgDestinationValidator {

  /**
   * Is it possible, that destination contains valid destinations as children?
   * 
   * @param destination the destination to verify
   * @return true if destination can have valid destinations
   */
  public boolean canChildrenBeDestinations(IReorgDestination destination);

  /**
   * Is it possible, that the given kind of destination is a target for the reorg?
   * 
   * @param destination the destination to verify
   * @return true if possible
   */
  public boolean canElementBeDestination(IReorgDestination destination);
}
