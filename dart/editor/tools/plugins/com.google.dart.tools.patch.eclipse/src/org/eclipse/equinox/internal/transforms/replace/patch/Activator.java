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
package org.eclipse.equinox.internal.transforms.replace.patch;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.net.URL;
import java.util.Dictionary;
import java.util.Properties;

public class Activator implements BundleActivator {

  private ServiceRegistration registration;

  @Override
  public void start(BundleContext context) throws Exception {
    Dictionary properties = new Properties();
    properties.put("equinox.transformerType", "replace"); //$NON-NLS-1$ //$NON-NLS-2$
    registration = context.registerService(URL.class.getName(),
        context.getBundle().getEntry("/transform.csv"), properties); //$NON-NLS-1$
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    if (registration != null) {
      registration.unregister();
    }
  }
}
