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

import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.HashMap;

/**
 * A dynamic list of transformers.
 */
public class TransformerList extends ServiceTracker<Object, Object> {

  /**
   * The stale state of this list. Set to true every time a new matching service instance is
   * detected.
   */
  private volatile boolean stale = true;

  /**
   * Local cache of transformers.
   */
  private HashMap<String, Object> transformers = new HashMap<String, Object>();

  /**
   * Create a new instance of this list.
   * 
   * @param context the context to track
   * @throws InvalidSyntaxException thrown if there's an issue listening for changes to the given
   *           transformer type
   */
  public TransformerList(BundleContext context) throws InvalidSyntaxException {
    super(context, context.createFilter("(&(objectClass=" //$NON-NLS-1$
        + Object.class.getName() + ")(" + TransformTuple.TRANSFORMER_TYPE //$NON-NLS-1$
        + "=*))"), null); //$NON-NLS-1$
    open();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public Object addingService(ServiceReference reference) {
    try {
      return super.addingService(reference);
    } finally {
      stale = true;
    }
  }

  /**
   * Return the transformer of the given type being monitored by this list. If the list is stale it
   * will first be rebuilt.
   * 
   * @param type the type of transformer
   * @return the transformer or null if no transformer of the given type is available.
   */
  public synchronized StreamTransformer getTransformer(String type) {
    if (stale) {
      rebuildTransformersMap();
    }
    return (StreamTransformer) transformers.get(type);
  }

  public synchronized boolean hasTransformers() {
    if (stale) {
      rebuildTransformersMap();
    }
    return transformers.size() > 0;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void modifiedService(ServiceReference reference, Object service) {
    super.modifiedService(reference, service);
    stale = true;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void removedService(ServiceReference reference, Object service) {
    super.removedService(reference, service);
    stale = true;
  }

  /**
   * Consults the bundle context for services of the transformer type and builds the internal cache.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private void rebuildTransformersMap() {
    transformers.clear();
    ServiceReference[] serviceReferences = getServiceReferences();
    stale = false;
    if (serviceReferences == null) {
      return;
    }

    for (int i = 0; i < serviceReferences.length; i++) {
      ServiceReference serviceReference = serviceReferences[i];
      String type = serviceReference.getProperty(TransformTuple.TRANSFORMER_TYPE).toString();
      if (type == null || transformers.get(type) != null) {
        continue;
      }
      Object object = getService(serviceReference);
      if (object instanceof StreamTransformer) {
        transformers.put(type, object);
      } else {
        ProxyStreamTransformer transformer;
        try {
          transformer = new ProxyStreamTransformer(object);
          transformers.put(type, transformer);
        } catch (SecurityException e) {
          TransformerHook.log(FrameworkLogEntry.ERROR, "Problem creating transformer", e); //$NON-NLS-1$
        } catch (NoSuchMethodException e) {
          TransformerHook.log(FrameworkLogEntry.ERROR, "Problem creating transformer", e); //$NON-NLS-1$
        }
      }
    }
  }
}
