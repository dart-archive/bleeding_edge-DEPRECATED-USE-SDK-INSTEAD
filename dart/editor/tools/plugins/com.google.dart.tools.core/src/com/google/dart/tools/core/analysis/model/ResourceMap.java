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
package com.google.dart.tools.core.analysis.model;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

/**
 * Instances of {@code ResourceMap} provides a bi-directional map between sources in a particular
 * {@link AnalysisContext} and instances of {@link IResource}.
 */
public interface ResourceMap {

  /**
   * Answer the analysis context associated with the receiver.
   * 
   * @return the analysis context (not {@code null})
   */
  AnalysisContext getContext();

  /**
   * Answer the root container associated with the receiver.
   * 
   * @return the container (not {@code null})
   */
  IContainer getResource();

  /**
   * Answer the resource for the given source in the context associated with the receiver or
   * {@code null} if the source represents a file that is not mapped into the workspace.
   * 
   * @param source the source
   * @return the associated resource or {@code null} if none
   */
  IFile getResource(Source source);

  /**
   * Answer the source for the given resource or {@code null} if the resource cannot be mapped to a
   * source in the context associated with the receiver.
   * 
   * @param resource the resource
   * @return the associated source or {@code null} if the resource cannot be mapped
   */
  Source getSource(IFile resource);
}
