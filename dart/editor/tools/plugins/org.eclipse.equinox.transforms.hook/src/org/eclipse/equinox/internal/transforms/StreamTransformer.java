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
import java.net.URL;

/**
 * This class represents the fundamental building block of the transformer system. Implementations
 * of this class are capable of transforming an input stream based on a given transformer url. The
 * meaning and content of this URL are unspecified - it is the transformers responsibility to
 * interpret these as need be.
 */
public abstract class StreamTransformer {
  /**
   * Provided a transformed version of the provided input stream.
   * 
   * @param inputStream the original stream
   * @param transformerUrl an url that may be used by the transformer in determining the proper
   *          transform to invoke.
   * @return the transformed stream
   * @throws IOException thrown if there is an issue invoking the transform
   */
  public abstract InputStream getInputStream(InputStream inputStream, URL transformerUrl)
      throws IOException;
}
