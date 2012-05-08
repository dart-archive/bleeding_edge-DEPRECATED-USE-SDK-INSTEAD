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
package com.google.dart.tools.search2.internal.ui.text;

import com.google.dart.tools.search.ui.text.Match;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;

public class Highlighter {
  private IFileBufferListener fBufferListener;

  public Highlighter() {
    fBufferListener = new IFileBufferListener() {
      public void bufferCreated(IFileBuffer buffer) {
      }

      public void bufferDisposed(IFileBuffer buffer) {
      }

      public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {
      }

      public void bufferContentReplaced(IFileBuffer buffer) {
        handleContentReplaced(buffer);
      }

      public void stateChanging(IFileBuffer buffer) {
      }

      public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty) {
      }

      public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {
      }

      public void underlyingFileMoved(IFileBuffer buffer, IPath path) {
      }

      public void underlyingFileDeleted(IFileBuffer buffer) {
      }

      public void stateChangeFailed(IFileBuffer buffer) {
      }
    };
    FileBuffers.getTextFileBufferManager().addFileBufferListener(fBufferListener);
  }

  /**
   * Adds highlighting for the given matches
   * 
   * @param matches the matches to add highlighting
   */
  public void addHighlights(Match[] matches) {
  }

  /**
   * Removes highlighting for the given matches
   * 
   * @param matches the matches to remove the highlighting
   */
  public void removeHighlights(Match[] matches) {
  }

  /**
   * Removes all highlighting
   */
  public void removeAll() {
  }

  /**
   * Called when the highlighter is disposed.
   */
  public void dispose() {
    FileBuffers.getTextFileBufferManager().removeFileBufferListener(fBufferListener);
  }

  /**
   * Notifies that a buffer has its content changed
   * 
   * @param buffer the buffer
   */
  protected void handleContentReplaced(IFileBuffer buffer) {
  }

}
