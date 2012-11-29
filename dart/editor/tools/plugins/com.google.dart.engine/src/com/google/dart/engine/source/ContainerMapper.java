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
 * The interface {@code ContainerMapper} defines the behavior of objects that can be used to compute
 * a container for a source.
 */
public interface ContainerMapper {
  /**
   * Return the container that should be associated with the given source.
   * 
   * @param source the source with which the returned container is associated
   * @return the container that should be associated with the given source
   */
  public SourceContainer getContainerFor(Source source);
}
