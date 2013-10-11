/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code CacheRetentionPolicy} define the behavior of objects that determine
 * how important it is for data to be retained in the analysis cache.
 */
public interface CacheRetentionPolicy {
  /**
   * Return the priority of retaining the AST structure for the given source.
   * 
   * @param source the source whose AST structure is being considered for removal
   * @param sourceEntry the entry representing the source
   * @return the priority of retaining the AST structure for the given source
   */
  public RetentionPriority getAstPriority(Source source, SourceEntry sourceEntry);
}
