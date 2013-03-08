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
package com.google.dart.engine.source;

/**
 * The interface {@code SourceContainer} is used by clients to define a collection of sources
 * <p>
 * Source containers are not used within analysis engine, but can be used by clients to group
 * sources for the purposes of accessing composite dependency information. For example, the Eclipse
 * client uses source containers to represent Eclipse projects, which allows it to easily compute
 * project-level dependencies.
 * 
 * @coverage dart.engine.source
 */
public interface SourceContainer {

  /**
   * Determine if the specified source is part of the receiver's collection of sources.
   * 
   * @param source the source in question
   * @return {@code true} if the receiver contains the source, else {@code false}
   */
  public boolean contains(Source source);
}
