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
package com.google.dart.tools.search.ui;

/**
 * An extension interface to <code>ISearchPage</code>. If clients implement
 * <code>IReplacePage</code> in addition to <code>ISearchPage</code>, a "Replace" button will be
 * shown in the search dialog.
 */
public interface IReplacePage {

  /**
   * Performs the replace action for this page. The search dialog calls this method when the Replace
   * button is pressed.
   * 
   * @return <code>true</code> if the dialog can be closed after execution
   */
  public boolean performReplace();

}
