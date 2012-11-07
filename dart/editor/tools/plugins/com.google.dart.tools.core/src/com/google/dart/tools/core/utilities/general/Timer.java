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

package com.google.dart.tools.core.utilities.general;

import com.google.dart.tools.core.DartCoreDebug;

import java.text.NumberFormat;

/**
 * A simple timer class. The output is controlled by an options flag.
 */
public class Timer {
  private final String name;
  private final long startTime;

  public Timer(String name) {
    this.name = name;
    this.startTime = System.nanoTime();
  }

  public void stop() {
    if (DartCoreDebug.PERF_TIMER) {
      long elapsedMillis = (System.nanoTime() - startTime) / 1000000;

      // "save in 100ms"
      System.out.println(name + " in " + NumberFormat.getInstance().format(elapsedMillis) + "ms");
    }
  }

}
