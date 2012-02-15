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
package com.google.dart.tools.core.internal.directoryset;

/**
 * This interface is used by the {@link DirectorySetManager} to send {@link DirectorySetEvent}s to
 * interested components.
 * 
 * @see DirectorySetManager
 * @see DirectorySetListener
 * @see FilesView
 */
public interface DirectorySetListener {
  /**
   * This methods is called from the {@link DirectorySetManager} whenever a changes has happened to
   * the set of directories in the manager
   * 
   * @param event an event representing the change which occurred
   */
  public void directorySetChange(DirectorySetEvent event);
}
