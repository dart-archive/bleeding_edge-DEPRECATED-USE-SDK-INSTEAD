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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;

/**
 * The class <code>AdapterUtilities</code> defines utility methods for adapting objects.
 */
public class AdapterUtilities {

  /**
   * If it is possible to adapt the given object to the given type, this returns the adapter.
   * Performs the following checks:
   * <ol>
   * <li>Returns <code>sourceObject</code> if it is an instance of the adapter type.</li>
   * <li>If sourceObject implements IAdaptable, it is queried for adapters.</li>
   * <li>If sourceObject is not an instance of PlatformObject (which would have already done so),
   * the adapter manager is queried for adapters</li>
   * </ol>
   * Otherwise returns null.
   * 
   * @param sourceObject object to adapt, or null
   * @param adapterType type to adapt to
   * @return a representation of sourceObject that is assignable to the adapter type, or null if no
   *         such representation exists
   */
  @SuppressWarnings("unchecked")
  public static <T> T getAdapter(Object sourceObject, Class<T> adapterType) {
    Assert.isNotNull(adapterType);
    if (sourceObject == null) {
      return null;
    }
    if (adapterType.isInstance(sourceObject)) {
      return (T) sourceObject;
    }

    if (sourceObject instanceof IAdaptable) {
      IAdaptable adaptable = (IAdaptable) sourceObject;

      Object result = adaptable.getAdapter(adapterType);
      if (result != null) {
        // Sanity-check
        Assert.isTrue(adapterType.isInstance(result));
        return (T) result;
      }
    }

    if (!(sourceObject instanceof PlatformObject)) {
      Object result = Platform.getAdapterManager().getAdapter(sourceObject, adapterType);
      if (result != null) {
        return (T) result;
      }
    }

    return null;
  }

}
