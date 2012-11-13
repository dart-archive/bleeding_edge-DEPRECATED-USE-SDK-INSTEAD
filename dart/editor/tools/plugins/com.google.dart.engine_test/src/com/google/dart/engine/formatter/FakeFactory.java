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
package com.google.dart.engine.formatter;

import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.formatter.edit.EditStore;

/**
 * Helper for creating faked implementations for testing purposes.
 */
public class FakeFactory {

  /**
   * A test recorder.
   */
  public static class FakeRecorder extends EditRecorder<String> {

    private final StringBuilder builder = new StringBuilder();

    protected FakeRecorder() {
      super(createScanner(), createStore());
    }

    @Override
    public String buildEdit() {
      return builder.toString();
    }

  }

  /**
   * A test scanner.
   */
  public static class FakeScanner implements Scanner {

  }

  /**
   * A test edit store.
   */
  public static class FakeStore implements EditStore {

    @Override
    public void addEdit(int offset, int length, String replacement) {
      // TODO Auto-generated method stub
    }

    @Override
    public int getCurrentEditIndex() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public Edit getLastEdit() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void insert(int insertPosition, String insertedString) {
      // TODO Auto-generated method stub
    }

    @Override
    public void reset() {
      // TODO Auto-generated method stub
    }

  }

  /**
   * Create a test recorder.
   * 
   * @return a test recorder
   */
  public static FakeRecorder createRecorder() {
    return new FakeRecorder();
  }

  /**
   * Create a test scanner.
   * 
   * @return a test scanner
   */
  public static FakeScanner createScanner() {
    return new FakeScanner();
  }

  /**
   * Create a test edit store.
   * 
   * @return a test edit store
   */
  public static FakeStore createStore() {
    return new FakeStore();
  }

}
