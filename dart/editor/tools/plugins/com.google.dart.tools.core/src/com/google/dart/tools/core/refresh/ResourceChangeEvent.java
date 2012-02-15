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
package com.google.dart.tools.core.refresh;

import org.eclipse.core.resources.IFile;

import java.util.List;

/**
 * Instances of the class <code>ResourceChangeEvent</code> represent a collection of files that have
 * been changed. The collection is divided into three parts: files that have been added, files whose
 * content has changed, and files that have been deleted.
 */
public class ResourceChangeEvent {
  /**
   * A list of the files that have been added.
   */
  private List<IFile> addedFiles;

  /**
   * A list of the files whose content has changed.
   */
  private List<IFile> modifiedFiles;

  /**
   * A list of the files that have been deleted.
   */
  private List<IFile> deletedFiles;

  /**
   * Initialize a newly created event representing the files that have been changed.
   * 
   * @param addedFiles the files that have been added
   * @param modifiedFiles the files whose content has changed
   * @param deletedFiles the files that have been deleted
   */
  public ResourceChangeEvent(List<IFile> addedFiles, List<IFile> modifiedFiles,
      List<IFile> deletedFiles) {
    this.addedFiles = addedFiles;
    this.modifiedFiles = modifiedFiles;
    this.deletedFiles = deletedFiles;
  }

  /**
   * Return a list of the files that have been added.
   * 
   * @return a list of the files that have been added
   */
  public List<IFile> getAddedFiles() {
    return addedFiles;
  }

  /**
   * Return a list of the files that have been deleted.
   * 
   * @return a list of the files that have been deleted
   */
  public List<IFile> getDeletedFiles() {
    return deletedFiles;
  }

  /**
   * Return a list of the files whose content has changed.
   * 
   * @return a list of the files whose content has changed
   */
  public List<IFile> getModifiedFiles() {
    return modifiedFiles;
  }
}
