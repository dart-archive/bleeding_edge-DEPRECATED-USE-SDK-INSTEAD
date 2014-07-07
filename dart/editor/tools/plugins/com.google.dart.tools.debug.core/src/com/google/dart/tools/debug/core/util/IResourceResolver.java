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

package com.google.dart.tools.debug.core.util;

import org.eclipse.core.resources.IResource;

import java.io.File;

/**
 * A resolver class used to convert from IResources to urls. Note : Some methods involve
 * communication overheads based on type of resolver, so clients should not call the resolver on the
 * UI thread, to avoid blocking the UI.
 * 
 * @see ResourceServer
 */
public interface IResourceResolver {

  /**
   * Given an File, return the corresponding URL.
   * 
   * @param file
   * @return
   */
  public String getUrlForFile(File file);

  /**
   * Given an IResource, return the corresponding URL.
   * 
   * @param resource
   * @return
   */
  public String getUrlForResource(IResource resource);

  /**
   * Given an IResource, return the corresponding regex used to match the resource.
   * 
   * @param resource
   * @return
   */
  public String getUrlRegexForResource(IResource resource);

  /**
   * Given a url, return the associated workspace resource. Return null if there is no such
   * resource.
   * 
   * @param url
   * @return
   */
  public IResource resolveUrl(String url);

}
