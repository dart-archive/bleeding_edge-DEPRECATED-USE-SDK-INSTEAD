// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library driver;

import 'package:analysis_server/plugin/plugin.dart';
import 'package:analysis_server/src/server/driver.dart';
import 'package:analyzer/instrumentation/instrumentation.dart';

/**
 * An object that can be used to start an analysis server.
 */
abstract class ServerStarter {
  /**
   * Initialize a newly created starter to start up an analysis server.
   */
  factory ServerStarter() = Driver;

  /**
   * Set the instrumentation [server] that is to be used by the analysis server.
   */
  void set instrumentationServer(InstrumentationServer server);

  /**
   * Set the [plugins] that are defined outside the analysis_server package.
   */
  void set userDefinedPlugins(List<Plugin> plugins);

  /**
   * Use the given command-line [arguments] to start this server.
   */
  void start(List<String> arguments);
}
