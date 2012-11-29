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

import com.google.dart.engine.context.AnalysisContext;

/**
 * The interface {@code SourceContainer} is a marker interface for objects representing a container
 * for {@link Source sources}.
 * <p>
 * Source containers are not used within analysis engine, but can be used by clients to group
 * sources for the purposes of accessing composite dependency information. For example, the Eclipse
 * client uses source containers to represent Eclipse projects, which allows it to easily compute
 * project-level dependencies.
 * 
 * @see AnalysisContext#getDependedOnContainers(SourceContainer)
 */
public interface SourceContainer {
  /**
   * Return {@code true} if the given object is a source container representing the same set of
   * sources as this container.
   * 
   * @return {@code true} if the given object and this container represent the same container
   */
  @Override
  public boolean equals(Object object);
}
