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

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogPage;

/**
 * Interface to be implemented by contributors to the extension point
 * <code>com.google.dart.tools.search.searchPages</code>. Represents a page in the search dialog.
 * Implemented typically subclass {@link DialogPage}.
 * <p>
 * The search dialog calls the {@link #performAction} method when the 'Search' button is pressed.
 * </p>
 * <p>
 * If the search page additionally implements {@link IReplacePage}, a 'Replace' button will be shown
 * in the search dialog.
 * </p>
 * 
 * @see org.eclipse.jface.dialogs.IDialogPage
 * @see org.eclipse.jface.dialogs.DialogPage
 */
public interface ISearchPage extends IDialogPage {

  /**
   * Performs the action for this page. The search dialog calls this method when the Search button
   * is pressed.
   * 
   * @return <code>true</code> if the dialog can be closed after execution
   */
  public boolean performAction();

  /**
   * Sets the container of this page. The search dialog calls this method to initialize this page.
   * Implementations may store the reference to the container.
   * 
   * @param container the container for this page
   */
  public void setContainer(ISearchPageContainer container);
}
