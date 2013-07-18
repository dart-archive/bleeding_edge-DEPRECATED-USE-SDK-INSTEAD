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
package com.google.dart.tools.core.analysis.model;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.source.Source;

import org.eclipse.core.resources.IResource;

/**
 * Instances of {@link ResolvedEvent} contain information about the resolved compilation unit.
 * 
 * @coverage dart.tools.core.model
 */
public interface ResolvedEvent extends AnalysisEvent {

  /**
   * Answer the resource of the compilation unit that was resolved.
   * 
   * @return the resource or {@code null} if the source is outside the workspace
   */
  IResource getResource();

  /**
   * Answer the source of the compilation unit that was resolved.
   * 
   * @return the source (not {@code null})
   */
  Source getSource();

  /**
   * Answer the compilation unit that was resolved.
   * 
   * @return the unit (not {@code null})
   */
  CompilationUnit getUnit();
}
