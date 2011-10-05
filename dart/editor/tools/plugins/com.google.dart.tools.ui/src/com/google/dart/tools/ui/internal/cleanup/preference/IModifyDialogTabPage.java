/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.cleanup.preference;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;

import java.util.Map;

/**
 * @since 3.4
 */
public interface IModifyDialogTabPage {

  public interface IModificationListener {

    void updateStatus(IStatus status);

    void valuesModified();

  }

  /**
   * Create the contents of this tab page.
   * 
   * @param parent the parent composite
   * @return created content control
   */
  public Composite createContents(Composite parent);

  /**
   * This is called when the page becomes visible. Common tasks to do include:
   * <ul>
   * <li>Updating the preview.</li>
   * <li>Setting the focus</li>
   * </ul>
   */
  public void makeVisible();

  /**
   * Each tab page should remember where its last focus was, and reset it correctly within this
   * method. This method is only called after initialization on the first tab page to be displayed
   * in order to restore the focus of the last session.
   */
  public void setInitialFocus();

  /**
   * A modify listener which must be informed whenever a value in the map passed to
   * {@link #setWorkingValues(Map)} changes. The listener can also be informed about status changes.
   * 
   * @param modifyListener the listener to inform
   */
  public void setModifyListener(IModificationListener modifyListener);

  /**
   * A map containing key value pairs this tab page is must modify.
   * 
   * @param workingValues the values to work with
   */
  public void setWorkingValues(Map<String, String> workingValues);

}
