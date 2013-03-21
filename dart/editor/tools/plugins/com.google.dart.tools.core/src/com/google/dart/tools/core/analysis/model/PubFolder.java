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
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.tools.core.pub.PubspecModel;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;

/**
 * Represents a project or folder within a project containing a pubspec file
 */
public interface PubFolder {

  /**
   * Answer the analysis context used for analyzing sources contained within the receiver
   * 
   * @return the analysis context (not {@code null})
   */
  AnalysisContext getContext();

  /**
   * The pubspec model representing the pubspec.yaml file
   * 
   * @return the pubspec model (not {@code null}
   */
  PubspecModel getPubspec() throws CoreException, IOException;

  /**
   * Answer the container associated with the receiver and containing the pubspec file
   * 
   * @return the container (not {@code null})
   */
  IContainer getResource();

  /**
   * Answer the {@link DartSdk} used when constructing the analysis context.
   * 
   * @return the sdk (not {@code null})
   */
  DartSdk getSdk();

  /**
   * Updates the pubspec model, called when the pubspec.yaml file is changed
   */
  void invalidatePubspec() throws CoreException, IOException;
}
