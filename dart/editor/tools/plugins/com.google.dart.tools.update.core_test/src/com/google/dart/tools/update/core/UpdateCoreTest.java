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
package com.google.dart.tools.update.core;

import junit.framework.TestCase;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class UpdateCoreTest extends TestCase {

  public void test_enableAutoDownload() {
    boolean original = UpdateCore.isAutoDownloadEnabled();
    try {
      UpdateCore.enableAutoDownload(true);
      assertTrue(UpdateCore.isAutoDownloadEnabled());
      UpdateCore.enableAutoDownload(false);
      assertFalse(UpdateCore.isAutoDownloadEnabled());
    } finally {
      UpdateCore.enableAutoDownload(original);
    }
  }

  public void test_getNextUpdateTime() {
    IEclipsePreferences prefs = UpdateCore.getInstance().getPreferences();
    GregorianCalendar date = new GregorianCalendar();
    date = new GregorianCalendar(date.get(Calendar.YEAR), date.get(Calendar.MONTH),
        date.get(Calendar.DAY_OF_MONTH));
    long today = date.getTimeInMillis();
    date.add(GregorianCalendar.DATE, 1);
    long tomorrow = date.getTimeInMillis();

    // If no pref, then returns today as time to check
    prefs.remove(UpdateCore.PREFS_LAST_UPDATE_CHECK);
    assertEquals(today, UpdateCore.getNextUpdateTime());

    // If pref, then return next day
    prefs.putLong(UpdateCore.PREFS_LAST_UPDATE_CHECK, today);
    assertEquals(tomorrow, UpdateCore.getNextUpdateTime());
  }

  public void test_updateChecked() throws Exception {
    IEclipsePreferences prefs = UpdateCore.getInstance().getPreferences();
    long now = System.currentTimeMillis();
    GregorianCalendar date = new GregorianCalendar();
    date = new GregorianCalendar(date.get(Calendar.YEAR), date.get(Calendar.MONTH),
        date.get(Calendar.DAY_OF_MONTH));
    long today = date.getTimeInMillis();
    date.add(GregorianCalendar.DATE, 1);
    long tomorrow = date.getTimeInMillis();

    // Always records the current date
    prefs.remove(UpdateCore.PREFS_LAST_UPDATE_CHECK);
    assertEquals(0, prefs.getLong(UpdateCore.PREFS_LAST_UPDATE_CHECK, 0));
    UpdateCore.updateChecked();
    assertEquals(today, prefs.getLong(UpdateCore.PREFS_LAST_UPDATE_CHECK, 0));

    prefs.putLong(UpdateCore.PREFS_LAST_UPDATE_CHECK, today);
    UpdateCore.updateChecked();
    assertEquals(today, prefs.getLong(UpdateCore.PREFS_LAST_UPDATE_CHECK, 0));

    prefs.putLong(UpdateCore.PREFS_LAST_UPDATE_CHECK, now);
    UpdateCore.updateChecked();
    assertEquals(today, prefs.getLong(UpdateCore.PREFS_LAST_UPDATE_CHECK, 0));

    prefs.putLong(UpdateCore.PREFS_LAST_UPDATE_CHECK, tomorrow);
    UpdateCore.updateChecked();
    assertEquals(today, prefs.getLong(UpdateCore.PREFS_LAST_UPDATE_CHECK, 0));
  }
}
