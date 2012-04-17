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
package com.google.dart.tools.update.core;

import com.google.dart.tools.update.core.internal.UpdateUtils;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Documents changes in promoted editor integration builds.
 */
public class ChangeLog {

  private final String url;

  /**
   * Create a change log.
   * 
   * @param url the backing url
   */
  public ChangeLog(String url) {
    this.url = url;
  }

  /**
   * Read the contents of the change log as a string.
   * 
   * @return a string representation of the changelog.
   * @throws MalformedURLException
   * @throws IOException
   */
  public String getContents() throws MalformedURLException, IOException {
    return UpdateUtils.readUrlStream(url);
  }

}
