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
package com.google.dart.tools.ui.internal.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;

/**
 * A utility class to work with IStatus.
 */
public class StatusUtil {

  /**
   * Applies the status to the status line of a dialog page.
   */
  public static void applyToStatusLine(DialogPage page, IStatus status) {
    String message = status.getMessage();
    switch (status.getSeverity()) {
      case IStatus.OK:
        page.setMessage(message, IMessageProvider.NONE);
        page.setErrorMessage(null);
        break;
      case IStatus.WARNING:
        page.setMessage(message, IMessageProvider.WARNING);
        page.setErrorMessage(null);
        break;
      case IStatus.INFO:
        page.setMessage(message, IMessageProvider.INFORMATION);
        page.setErrorMessage(null);
        break;
      default:
        if (message.length() == 0) {
          message = null;
        }
        page.setMessage(null);
        page.setErrorMessage(message);
        break;
    }
  }

  /**
   * Compares two instances of <code>IStatus</code>. The more severe is returned: An error is more
   * severe than a warning, and a warning is more severe than ok. If the two stati have the same
   * severity, the second is returned.
   */
  public static IStatus getMoreSevere(IStatus s1, IStatus s2) {
    if (s1.getSeverity() > s2.getSeverity()) {
      return s1;
    } else {
      return s2;
    }
  }

  /**
   * Finds the most severe status from a array of stati. An error is more severe than a warning, and
   * a warning is more severe than ok.
   */
  public static IStatus getMostSevere(IStatus[] status) {
    IStatus max = null;
    for (int i = 0; i < status.length; i++) {
      IStatus curr = status[i];
      if (curr.matches(IStatus.ERROR)) {
        return curr;
      }
      if (max == null || curr.getSeverity() > max.getSeverity()) {
        max = curr;
      }
    }
    return max;
  }
}
