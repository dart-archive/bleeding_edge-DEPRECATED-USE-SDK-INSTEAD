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

import com.google.dart.engine.context.AnalysisContext;

/**
 * This gives access to the internals of the AnalysisServer. This is a temporary access point to
 * keep the analysis server internals and the old model in sync until such time as we have ported
 * all functionality to use the new {@link AnalysisServer} API. This interface and all uses of it
 * should be deleted before we can consider Dart Editor fully ported to use the analysis server.
 */
public interface InternalAnalysisServer {

  /**
   * Answer the context for the given identifier.
   * 
   * @param contextId the context identifier
   * @return the context or {@code null} if none is currently associated with that identifier
   */
  public AnalysisContext getContext(String contextId);
}
