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

package com.google.dart.tools.core.instrumentation;

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * A manager class to retrieve the current IInstrumentationLogger instance.
 */
public class InstrumentationLogger {
  private static class NullLogger implements IInstrumentationLogger {
    @Override
    public void debug(String tag, String text) {
    }

    @Override
    public void error(String tag, String text) {
    }

    @Override
    public String getSessionId() {
      return null;
    }

    @Override
    public String getUserId() {
      return null;
    }

    @Override
    public void info(String tag, String text) {
    }

    @Override
    public void verbose(String tag, String text) {
    }
  }

  private static final String EXTENSION_POINT_ID = DartCore.PLUGIN_ID + ".instrumentationLogger";

  private static IInstrumentationLogger logger = null;

  /**
   * Ensure that the instrumentation system has started and if a logger is available that it is
   * registered
   */
  public static void ensureLoggerStarted() {
    if (logger == null) {
      init();
    }
  }

  public static IInstrumentationLogger getLogger() {
    ensureLoggerStarted();

    return logger;
  }

  private static void init() {
    IExtensionRegistry registery = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registery.getExtensionPoint(EXTENSION_POINT_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

    if (elements.length > 0) {
      if (elements.length > 1) {
        DartCore.logError("Error, more then one instrumentation logger contributed", null);
      }

      IConfigurationElement element = elements[0];

      try {

        //Setup old logger
        logger = (IInstrumentationLogger) element.createExecutableExtension("class");

        //Setup new logger

        if (logger instanceof com.google.dart.engine.utilities.instrumentation.InstrumentationLogger) {
          Instrumentation.setLogger((com.google.dart.engine.utilities.instrumentation.InstrumentationLogger) logger);
        }

        return;
      } catch (Throwable t) {
        DartCore.logError(t);
      }
    }

    logger = new NullLogger();
  }

  private InstrumentationLogger() {

  }

}
