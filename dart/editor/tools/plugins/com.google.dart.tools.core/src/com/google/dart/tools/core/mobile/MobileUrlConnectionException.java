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

  private final String pageUrl;

  public MobileUrlConnectionException(String pageUrl) {
    super(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, 0, "Failed to access URL from mobile: "
        + pageUrl + ".\n\nCheck port forwarding in Chrome and try again.", null));
    this.pageUrl = pageUrl;
  }

  public String getPageUrl() {
    return pageUrl;
  }
}
