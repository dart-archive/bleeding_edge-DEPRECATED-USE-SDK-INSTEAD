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
package com.google.dart.tools.designer.editor;

import org.eclipse.jface.text.IDocument;

/**
 * Strategy for refresh after {@link IDocument} change in {@link UndoManager}.
 * 
 * @author scheglov_ke
 * @coverage XML.editor
 */
public interface IRefreshStrategy {
  /**
   * @return <code>true</code> if UI should be refreshed immediately after change.
   */
  boolean shouldImmediately();

  /**
   * @return <code>true</code> if UI should be refreshed after {@link #getDelay()} ms.
   */
  boolean shouldWithDelay();

  /**
   * @return <code>true</code> if UI should be refreshed on editor save.
   */
  boolean shouldOnSave();

  /**
   * @return the delay in milliseconds for refreshing UI.
   */
  int getDelay();

  IRefreshStrategy IMMEDIATELY = new IRefreshStrategy() {
    @Override
    public boolean shouldImmediately() {
      return true;
    }

    @Override
    public boolean shouldWithDelay() {
      return false;
    }

    @Override
    public boolean shouldOnSave() {
      return false;
    }

    @Override
    public int getDelay() {
      return 0;
    }
  };
}
