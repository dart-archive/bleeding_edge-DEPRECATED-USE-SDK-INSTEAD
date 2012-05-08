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

import com.google.dart.tools.search.ui.text.AbstractTextSearchResult;

import java.util.HashMap;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class AnnotationManagers {
  static {
    fgManagerMap = new HashMap<IWorkbenchWindow, WindowAnnotationManager>();
    IWindowListener listener = new IWindowListener() {
      public void windowActivated(IWorkbenchWindow window) {
        // ignore
      }

      public void windowDeactivated(IWorkbenchWindow window) {
        // ignore
      }

      public void windowClosed(IWorkbenchWindow window) {
        disposeAnnotationManager(window);
      }

      public void windowOpened(IWorkbenchWindow window) {
        // ignore
      }
    };
    PlatformUI.getWorkbench().addWindowListener(listener);
  }

  private static HashMap<IWorkbenchWindow, WindowAnnotationManager> fgManagerMap;

  private static void disposeAnnotationManager(IWorkbenchWindow window) {
    WindowAnnotationManager mgr = fgManagerMap.remove(window);
    if (mgr != null)
      mgr.dispose();
  }

  public static void addSearchResult(IWorkbenchWindow window, AbstractTextSearchResult newResult) {
    getWindowAnnotationManager(window).addSearchResult(newResult);
  }

  public static void removeSearchResult(IWorkbenchWindow window, AbstractTextSearchResult result) {
    getWindowAnnotationManager(window).removeSearchResult(result);
  }

  private static WindowAnnotationManager getWindowAnnotationManager(IWorkbenchWindow window) {
    WindowAnnotationManager mgr = fgManagerMap.get(window);
    if (mgr == null) {
      mgr = new WindowAnnotationManager(window);
      fgManagerMap.put(window, mgr);
    }
    return mgr;
  }

}
