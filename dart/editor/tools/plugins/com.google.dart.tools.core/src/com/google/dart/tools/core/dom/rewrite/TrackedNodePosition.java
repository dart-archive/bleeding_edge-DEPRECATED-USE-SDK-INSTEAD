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
package com.google.dart.tools.core.dom.rewrite;

/**
 * The interface <code>TrackedNodePosition</code> defines the behavior of objects that are returned
 * when a rewrite change is requested to be tracked.
 */
public interface TrackedNodePosition {
  /**
   * Return the original or modified length of the tracked node depending if called before or after
   * the rewrite is applied. <code>-1</code> is returned for removed nodes.
   * 
   * @return the original or modified length of the tracked node
   */
  public int getLength();

  /**
   * Return the original or modified start position of the tracked node depending if called before
   * or after the rewrite is applied. <code>-1</code> is returned for removed nodes.
   * 
   * @return the original or modified start position of the tracked node
   */
  public int getStartPosition();
}
