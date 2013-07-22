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

package com.google.dart.engine.source;

/**
 * A SourceFactory subclass that exposes the getSource() and getModificationStamp() methods.
 */
public class TestSourceFactory extends SourceFactory {

  public TestSourceFactory(ContentCache contentCache, UriResolver... resolvers) {
    super(contentCache, resolvers);
  }

  public TestSourceFactory(UriResolver... resolvers) {
    super(resolvers);
  }

  @Override
  protected String getContents(Source source) {
    return super.getContents(source);
  }

  @Override
  protected Long getModificationStamp(Source source) {
    return super.getModificationStamp(source);
  }
}
