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

import java.net.URL;
import java.util.regex.Pattern;

/**
 * Class that represents an association between a bundle pattern, a path pattern, and the location
 * of a transformer to apply to any resource that matches both the bundle and path pattern.
 */
public class TransformTuple {

  /**
   * Constant used when registering transform tuples to identify the type of transformer they should
   * be assigned to.
   */
  public static final String TRANSFORMER_TYPE = "equinox.transformerType"; //$NON-NLS-1$
  public Pattern bundlePattern;
  public Pattern pathPattern;
  public URL transformerUrl;
}
