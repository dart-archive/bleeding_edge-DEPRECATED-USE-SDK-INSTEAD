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

import org.eclipse.core.resources.IFile;

import java.io.File;

/**
 * Represents a file on disk either with a resource {@link IFile} or a file {@link File}
 */
public interface IFileInfo {

  /**
   * Answer the {@link File} representation
   */
  File getFile();

  /**
   * Answer the {@link IFile} or {@code null} if there is no resource representation
   */
  IFile getResource();

}
