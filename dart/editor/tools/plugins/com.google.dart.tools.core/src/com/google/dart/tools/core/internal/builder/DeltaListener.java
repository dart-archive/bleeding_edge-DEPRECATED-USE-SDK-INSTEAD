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

/**
 * Used by {@link DeltaProcessor} to communicate Dart project changes
 */
public interface DeltaListener {

  /**
   * Called when a source file in the "packages" directory has been added
   */
  void packageSourceAdded(SourceDeltaEvent event);

  /**
   * Called when a source file in the "packages" directory has changed
   */
  void packageSourceChanged(SourceDeltaEvent event);

  /**
   * Called when a folder containing source files in the "packages" directory or the "packages"
   * directory itself has been removed
   */
  void packageSourceContainerRemoved(SourceContainerDeltaEvent event);

  /**
   * Called when a source file in the "packages" directory has been removed
   */
  void packageSourceRemoved(SourceDeltaEvent event);

  /**
   * Called when a pubspec.yaml file has been added
   */
  void pubspecAdded(ResourceDeltaEvent event);

  /**
   * Called when a pubspec.yaml file has changed
   */
  void pubspecChanged(ResourceDeltaEvent event);

  /**
   * Called when a pubspec.yaml file has been removed
   */
  void pubspecRemoved(ResourceDeltaEvent event);

  /**
   * Called when a source file has been added
   */
  void sourceAdded(SourceDeltaEvent event);

  /**
   * Called when a source file has changed
   */
  void sourceChanged(SourceDeltaEvent event);

  /**
   * Called when a folder containing source files has been removed
   */
  void sourceContainerRemoved(SourceContainerDeltaEvent event);

  /**
   * Called when a source file has been removed
   */
  void sourceRemoved(SourceDeltaEvent event);

  /**
   * Called before traversing resources in a context
   */
  void visitContext(ResourceDeltaEvent event);
}
