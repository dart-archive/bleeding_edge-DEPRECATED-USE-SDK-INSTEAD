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
 * Instances of the class {@code DefaultContainerMapper} implement a container mapper that can be
 * used in systems where the container is ignored.
 */
public final class DefaultContainerMapper implements ContainerMapper {
  /**
   * A container mapper that can be used in systems where the container is ignored.
   */
  private static final DefaultContainerMapper INSTANCE = new DefaultContainerMapper();

  /**
   * The container returned by this mapper for all sources.
   */
  private static final SourceContainer CONTAINER = new SourceContainer() {
  };

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static DefaultContainerMapper getInstance() {
    return INSTANCE;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private DefaultContainerMapper() {
    super();
  }

  @Override
  public SourceContainer getContainerFor(Source source) {
    return CONTAINER;
  }
}
