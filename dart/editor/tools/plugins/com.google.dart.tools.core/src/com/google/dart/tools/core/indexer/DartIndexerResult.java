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
package com.google.dart.tools.core.indexer;

import com.google.dart.indexer.locations.Location;

import org.eclipse.core.runtime.IPath;

/**
 * Instances of the class <code>DartIndexerResult</code> represent the result returned by the Dart
 * indexer.
 */
public class DartIndexerResult {
  /**
   * The locations that were found that matched the query.
   */
  private final Location[] result;

  /**
   * The errors that occurred while processing the query.
   */
  private final IPath[] errors;

  /**
   * Initialize a newly created result object with the given information.
   * 
   * @param result the locations that were found that matched the query
   * @param errors the errors that occurred while processing the query
   */
  public DartIndexerResult(final Location[] result, final IPath[] errors) {
    this.result = result;
    this.errors = errors;
  }

  /**
   * @return the errors that occurred while processing the query
   */
  public IPath[] getErrors() {
    return errors;
  }

  /**
   * Return the locations that were found that matched the query.
   * 
   * @return the locations that were found that matched the query
   */
  public Location[] getResult() {
    return result;
  }
}
