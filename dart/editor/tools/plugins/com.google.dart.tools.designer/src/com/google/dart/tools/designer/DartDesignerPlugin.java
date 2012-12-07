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
package com.google.dart.tools.designer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wb.internal.core.BundleResourceProvider;
import org.osgi.framework.BundleContext;

import java.io.InputStream;

/**
 * The activator class controls the plug-in life cycle.
 * 
 * @author scheglov_ke
 * @coverage XML
 */
public class DartDesignerPlugin extends AbstractUIPlugin {
  public static final String PLUGIN_ID = "com.google.dart.tools.designer"; //$NON-NLS-1$
  private static DartDesignerPlugin m_plugin;

  ////////////////////////////////////////////////////////////////////////////
  //
  // Life cycle
  //
  ////////////////////////////////////////////////////////////////////////////
  @Override
  public void stop(BundleContext context) throws Exception {
    m_plugin = null;
    super.stop(context);
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    m_plugin = this;
  }

  /**
   * Returns the shared instance.
   */
  public static DartDesignerPlugin getDefault() {
    return m_plugin;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Resources
  //
  ////////////////////////////////////////////////////////////////////////////
  private static final BundleResourceProvider m_resourceProvider = BundleResourceProvider.get(PLUGIN_ID);

  /**
   * @return the {@link InputStream} for file from plugin directory.
   */
  public static InputStream getFile(String path) {
    return m_resourceProvider.getFile(path);
  }

  /**
   * @return the {@link Image} from "icons" directory, with caching.
   */
  public static Image getImage(String path) {
    return m_resourceProvider.getImage("icons/" + path);
  }

  /**
   * @return the {@link ImageDescriptor} from "icons" directory.
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return m_resourceProvider.getImageDescriptor("icons/" + path);
  }

}
