/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.compiler.Source;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class MockSource implements Source {
  private final String name;
  private final URI uri;

  public MockSource(String name) throws URISyntaxException {
    this.name = name;
    this.uri = new URI(name);
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public long getLastModified() {
    return 0;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public URI getUri() {
    return uri;
  }
}
