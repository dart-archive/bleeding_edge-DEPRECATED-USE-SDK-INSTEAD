/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server;

/**
 * The interface {@code HighlightRegion} defines the behavior of objects representing a particular
 * syntactic or semantic meaning associated with a source region.
 * 
 * @coverage dart.server
 */
public interface HighlightRegion extends SourceRegion {
  /**
   * Return the type of highlight associated with the region.
   * 
   * @return the type of highlight associated with the region
   */
  public HighlightType getType();
}
