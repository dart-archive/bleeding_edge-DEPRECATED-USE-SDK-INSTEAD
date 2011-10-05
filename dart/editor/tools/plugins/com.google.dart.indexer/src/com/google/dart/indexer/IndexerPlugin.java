/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer;

import com.google.dart.indexer.utilities.logging.Logger;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class IndexerPlugin extends Plugin {
  public static final String PLUGIN_ID = IndexerPlugin.class.getPackage().getName();

  private static IndexerPlugin plugin;

  // private static BundleContext context;
  private static Logger logger;

  public static IndexerPlugin getDefault() {
    return plugin;
  }

  public static Logger getLogger() {
    return logger;
  }

  // static BundleContext getContext() {
  // return context;
  // }

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    super.start(bundleContext);
    // context = bundleContext;
    plugin = this;
    logger = new Logger(this);
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    plugin = null;
    // context = null;
    super.stop(bundleContext);
  }
}
