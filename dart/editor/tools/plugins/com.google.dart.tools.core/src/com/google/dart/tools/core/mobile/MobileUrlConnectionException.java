/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.core.mobile;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class MobileUrlConnectionException extends CoreException {

  public static final String SERVE_OVER_USB_TEXT = "Serve content over USB, uses localhost address";

  private static Status newStatus(String pageUrl, boolean localhostOverUsb) {
    StringBuilder msg = new StringBuilder();
    msg.append("Failed to access URL from mobile: ");
    msg.append(pageUrl);
    msg.append(".\n\n");
    if (localhostOverUsb) {
      msg.append("Check port forwarding in Chrome and try again.");
    } else {
      msg.append("Check wifi access permissions and try again.\n\n");
      msg.append("Alternately, open the Manage Launches dialog,");
      msg.append(" select the mobile launch configuration, and select \"");
      msg.append(SERVE_OVER_USB_TEXT);
      msg.append("\".\n");
    }
    return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, 0, msg.toString(), null);
  }

  private final String pageUrl;

  private final boolean localhostOverUsb;

  public MobileUrlConnectionException(String pageUrl, boolean localhostOverUsb) {
    super(newStatus(pageUrl, localhostOverUsb));
    this.pageUrl = pageUrl;
    this.localhostOverUsb = localhostOverUsb;
  }

  public String getPageUrl() {
    return pageUrl;
  }

  public boolean isLocalhostOverUsb() {
    return localhostOverUsb;
  }
}
