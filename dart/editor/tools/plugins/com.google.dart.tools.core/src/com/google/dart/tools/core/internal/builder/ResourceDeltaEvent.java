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
import com.google.dart.tools.core.analysis.model.Project;

import org.eclipse.core.resources.IResource;

public interface ResourceDeltaEvent {

  /**
   * Answer the context in which the resource should be analyzed
   * 
   * @return the context (not {@code null})
   */
  AnalysisContext getContext();

  /**
   * Answer the project in which the resource resides
   * 
   * @return the project (not {@code null})
   */
  Project getProject();

  /**
   * Answer the resource that was added, changed, or removed
   * 
   * @return the resource (not {@code null})
   */
  IResource getResource();
}
