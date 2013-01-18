/*
 * Copyright 2013 Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.SourceContainer;

public interface SourceContainerDeltaEvent extends ResourceDeltaEvent {

  /**
   * Answer the source container for the resource associated with the receiver
   * 
   * @return the source container or {@code null} if the resource location is null and the source
   *         container could not be determined
   */
  SourceContainer getSourceContainer();

  /**
   * Answer {@code true} if the the current container associated with this event contains all
   * resources associated with the {@link AnalysisContext} associated with this container, or
   * {@code false} if this container is a subfolder within that folder hierarchy.
   */
  boolean isTopContainerInContext();
}
