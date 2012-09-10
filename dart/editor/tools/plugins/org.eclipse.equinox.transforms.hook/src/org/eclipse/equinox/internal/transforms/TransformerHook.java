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
package org.eclipse.equinox.internal.transforms;

import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.HookConfigurator;
import org.eclipse.osgi.baseadaptor.HookRegistry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Properties;

/**
 * The framework extension that is capable of applying transforms to bundle content.
 */
public class TransformerHook implements BundleFileWrapperFactoryHook, HookConfigurator, AdaptorHook {
  static void log(int severity, String msg, Throwable t) {
    if (TransformerHook.ADAPTOR == null) {
      System.err.println(msg);
      t.printStackTrace();
      return;
    }
    FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME,
        severity, 0, msg, 0, t, null);
    TransformerHook.ADAPTOR.getFrameworkLog().log(entry);
  }

  private TransformerList transformers;
  private TransformInstanceListData templates;

  private static BaseAdaptor ADAPTOR;

  @Override
  public void addHooks(HookRegistry hookRegistry) {
    hookRegistry.addAdaptorHook(this);
    hookRegistry.addBundleFileWrapperFactoryHook(this);
  }

  @Override
  public void addProperties(Properties properties) {
    // no properties to add
  }

  @Override
  public FrameworkLog createFrameworkLog() {
    return null;
  }

  @Override
  public void frameworkStart(BundleContext context) throws BundleException {
    try {
      this.transformers = new TransformerList(context);
    } catch (InvalidSyntaxException e) {
      throw new BundleException("Problem registering service tracker: transformers", e); //$NON-NLS-1$
    }
    try {
      this.templates = new TransformInstanceListData(context);
    } catch (InvalidSyntaxException e) {
      transformers.close();
      transformers = null;
      throw new BundleException("Problem registering service tracker: templates", e); //$NON-NLS-1$
    }

  }

  @Override
  public void frameworkStop(BundleContext context) {
    transformers.close();
    templates.close();
  }

  @Override
  public void frameworkStopping(BundleContext context) {
    //nothing to do here
  }

  @Override
  public void handleRuntimeError(Throwable error) {
    //no special handling by this framework extension
  }

  @Override
  public void initialize(BaseAdaptor adaptor) {
    TransformerHook.ADAPTOR = adaptor;
  }

  @Override
  public URLConnection mapLocationToURLConnection(String location) {
    return null;
  }

  public boolean matchDNChain(String pattern, String[] dnChain) {
    return false;
  }

  /**
   * @throws IOException
   */
  @Override
  public BundleFile wrapBundleFile(BundleFile bundleFile, Object content, BaseData data,
      boolean base) throws IOException {
    if (transformers == null || templates == null) {
      return null;
    }
    return new TransformedBundleFile(transformers, templates, data, bundleFile);
  }

  protected BundleContext getContext() {
    return TransformerHook.ADAPTOR.getContext();
  }
}
