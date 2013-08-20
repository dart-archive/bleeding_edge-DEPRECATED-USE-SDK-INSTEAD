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

package com.google.dart.tools.ui.internal.util;

import org.eclipse.swt.program.Program;

/**
 * This class provides utility methods for external browsers.
 */
public class ExternalBrowserUtil {

  /**
   * Given some valid {@link String} url, this method opens the link in default external browser.
   * 
   * @param url some url to open in an external browser, if <code>null</code> is passed, this method
   *          does nothing
   */
  public static void openInExternalBrowser(String url) {
    if (url == null || url.isEmpty()) {
      return;
    }
    Program.launch(url);
    // FIXME(scheglov) this code allows some configuration, but does not work on Linux.
    // See https://code.google.com/p/dart/issues/detail?id=12560
    // If restore this code, restore also JavaDoc:
//    * Given some valid {@link String} url, this method opens the link in the external browser that
//    * the {@link PlatformUI} can find.
//    IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
//    try {
//      IWebBrowser browser = support.getExternalBrowser();
//      browser.openURL(new URL(url));
//    } catch (MalformedURLException e) {
//      DartToolsPlugin.log(e);
//    } catch (PartInitException e) {
//      DartToolsPlugin.log(e);
//    }
  }

}
