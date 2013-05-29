/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.compiler.util;

import com.google.dart.compiler.DartSource;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;

/**
 *
 */
public class DartSourceString implements DartSource {

  /**
   * @param elementName
   * @param source
   */
  public DartSourceString(String elementName, String source) {
    // TODO Auto-generated constructor stub
  }

  @Override
  public boolean exists() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public long getLastModified() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Reader getSourceReader() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getUniqueIdentifier() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public URI getUri() {
    // TODO Auto-generated method stub
    return null;
  }

}
