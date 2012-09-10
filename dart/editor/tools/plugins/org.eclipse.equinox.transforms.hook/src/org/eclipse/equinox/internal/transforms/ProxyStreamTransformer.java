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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * A proxy stream transformer is a transformer instance that relies on reflection to obtain the
 * "getInputStream" method from an underlying object. This class is useful due to the restrictions
 * in the builder that prevent transformer providers from directly implementing
 * {@link StreamTransformer} due to visibility and builder issues related to referring to classes
 * within fragments.
 */
public class ProxyStreamTransformer extends StreamTransformer {

  private Method method;
  private Object object;

  /**
   * Create a new proxy transformer based on the given object.
   * 
   * @param object the object to proxy
   * @throws SecurityException thrown if there is an issue utilizing the reflection methods
   * @throws NoSuchMethodException thrown if the provided object does not have a "getInputStream"
   *           method that takes an {@link InputStream} and an {@link URL}
   */
  public ProxyStreamTransformer(Object object) throws SecurityException, NoSuchMethodException {
    this.object = object;
    method = object.getClass().getMethod(
        "getInputStream", new Class[] {InputStream.class, URL.class}); //$NON-NLS-1$
    Class<?> returnType = method.getReturnType();
    if (!returnType.equals(InputStream.class)) {
      throw new NoSuchMethodException();
    }

  }

  @Override
  public InputStream getInputStream(InputStream inputStream, URL transformerUrl) throws IOException {
    try {
      return (InputStream) method.invoke(object, new Object[] {inputStream, transformerUrl});
    } catch (IllegalArgumentException e) {
      throw new IOException(e.getMessage());
    } catch (IllegalAccessException e) {
      throw new IOException(e.getMessage());
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
    }
    return null;
  }

  /**
   * Get the object that is being proxied.
   * 
   * @return the object. Never <code>null</code>.
   */
  public Object getTransformer() {
    return object;
  }
}
