package com.google.dart.tools.debug.ui.internal;

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Helper class for instrumenting launch and debug services
 */
public class DebugInstrumentationUtilities {

  public static void recordLaunchConfiguration(DartLaunchConfigWrapper launch,
      InstrumentationBuilder instrumentation) {

    instrumentation.data("LaunchConfig-ApplicationName", launch.getApplicationName());
    instrumentation.data("LaunchConfig-getProjectName", launch.getProjectName());
    instrumentation.data("LaunchConfig-getUrl", launch.getUrl());
    instrumentation.data("LaunchConfig-getWorkingDirectory", launch.getWorkingDirectory());

    instrumentation.metric("LaunchConfig-getArguments", launch.getArguments());
    instrumentation.metric("LaunchConfig-getBrowserName", launch.getBrowserName());
    instrumentation.metric("LaunchConfig-getCheckedMode", String.valueOf(launch.getCheckedMode()));
    instrumentation.metric("LaunchConfig-getLastLaunchTime", launch.getLastLaunchTime());
    instrumentation.metric(
        "LaunchConfig-getShouldLaunchFile",
        String.valueOf(launch.getShouldLaunchFile()));
    instrumentation.metric(
        "LaunchConfig-getShowLaunchOutput",
        String.valueOf(launch.getShowLaunchOutput()));
    instrumentation.metric(
        "LaunchConfig-getUseDefaultBrowser",
        String.valueOf(launch.getUseDefaultBrowser()));
    instrumentation.metric(
        "LaunchConfig-getUseWebComponents",
        String.valueOf(launch.getUseWebComponents()));
    instrumentation.metric("LaunchConfig-getVmArgumentsAsArray", launch.getVmArgumentsAsArray());

  }

  public static void recordLaunchConfiguration(ILaunchConfiguration launch,
      InstrumentationBuilder instrumentation) {

    try {
      instrumentation.metric("launchConfig-getCategory", launch.getCategory());
      instrumentation.metric("launchConfig-getClass", launch.getClass().toString());

      instrumentation.data("launchConfig-getName", launch.getName());

    } catch (Exception e) {
    }
  }

}
