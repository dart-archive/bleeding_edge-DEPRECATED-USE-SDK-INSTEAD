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
package com.google.dart.engine.source;

/**
 * Instances of the class {@code TestSource} implement a source object that can be used for testing
 * purposes.
 */
public class TestSource implements Source {
  @Override
  public void getContents(ContentReceiver receiver) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getFullName() {
    return null;
  }

  @Override
  public String getShortName() {
    return null;
  }

  @Override
  public boolean isInSystemLibrary() {
    return false;
  }

  @Override
  public Source resolve(String uri) {
    return null;
  }
}
