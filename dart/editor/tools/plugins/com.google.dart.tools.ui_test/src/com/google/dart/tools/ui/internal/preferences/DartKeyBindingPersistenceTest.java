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
package com.google.dart.tools.ui.internal.preferences;

import com.google.dart.tools.core.DartCore;
import com.google.dart.ui.test.UIThreadTestCase;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;

public class DartKeyBindingPersistenceTest extends UIThreadTestCase {

  private static String PLATFORM_CTRL;

  static {
    if (DartCore.isMac()) {
      PLATFORM_CTRL = "COMMAND";
    } else {
      PLATFORM_CTRL = "CTRL";
    }
  }

  public void testExportPrefs() {
    DartKeyBindingPersistence persist = getBindingPersist();
    File file = new File("testExportPrefs");
    try {
      try {
        persist.writeFile(file, "UTF-8");
      } catch (CoreException ex) {
        fail(ex.getMessage());
      }
      assertTrue(file.exists() && file.canRead());
      assertTrue(file.length() > 0L);
    } finally {
      file.delete();
    }
  }

  public void testImportPrefs() {
    DartKeyBindingPersistence persist = getBindingPersist();
    assertTrue(hasKeyBinding(persist, "Close", PLATFORM_CTRL + "+W"));
    File file = new File("testImportPrefs");
    try {
      try {
        persist.writeFile(file, "UTF-8");
      } catch (CoreException ex) {
        fail(ex.getMessage());
      }
      persist = getBindingPersist();
      try {
        persist.readFile(file, "UTF-8");
      } catch (CoreException ex) {
        fail(ex.getMessage());
      }
      assertTrue(file.exists() && file.canRead());
      assertTrue(file.length() > 0L);
      assertTrue(hasKeyBinding(persist, "Close", PLATFORM_CTRL + "+W"));
    } finally {
      file.delete();
    }
  }

  public void testResetPrefs() {
    DartKeyBindingPersistence persist = getBindingPersist();
    assertTrue(hasKeyBinding(persist, "Close", PLATFORM_CTRL + "+W"));
    persist = getBindingPersist();
    try {
      Reader reader = new StringReader("<dartKeyBindings/>");
      persist.readFrom(reader);
    } catch (CoreException ex) {
      fail(ex.getMessage());
    }
    assertFalse(hasKeyBinding(persist, "Close", PLATFORM_CTRL + "+W"));
    persist = getBindingPersist();
    try {
      persist.resetBindings();
    } catch (CoreException ex) {
      fail(ex.getMessage());
    }
    assertTrue(hasKeyBinding(persist, "Close", PLATFORM_CTRL + "+W"));
  }

  private DartKeyBindingPersistence getBindingPersist() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IActivityManager act = workbench.getActivitySupport().getActivityManager();
    IBindingService bind = (IBindingService) workbench.getService(IBindingService.class);
    ICommandService cmd = (ICommandService) workbench.getService(ICommandService.class);
    DartKeyBindingPersistence persist = new DartKeyBindingPersistence(act, bind, cmd);
    return persist;
  }

  private boolean hasKeyBinding(DartKeyBindingPersistence persist, String commandName,
      String keySequence) {
    try {
      Binding bind = persist.findBinding(commandName, null, null);
      if (bind == null) {
        return false;
      }
      return keySequence.equals(bind.getTriggerSequence().toString());
    } catch (NotDefinedException ex) {
      // fall through
    }
    return false;
  }
}
