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
package com.google.dart.tools.core.model;

import com.google.dart.tools.core.DartCore;

/**
 * The interface <code>DartModelStatusConstants</code> defines status codes used with Dart model
 * status objects.
 * <p>
 * This interface declares constants only; it is not intended to be implemented or extended.
 */
public interface DartModelStatusConstants {
  public static Runnable R = new Runnable() {
    @Override
    public void run() {
      DartCore.notYetImplemented();
      // Renumber the constants from 1 once they are reasonably complete.
    }
  };
  public static final int BAD_TEXT_EDIT_LOCATION = 0;
  public static final int CORE_EXCEPTION = 1;
  public static final int ELEMENT_DOES_NOT_EXIST = 2;
  public static final int INDEX_OUT_OF_BOUNDS = 0;
  public static final int INVALID_CONTENTS = 3;
  public static final int INVALID_DESTINATION = 0;
  public static final int INVALID_ELEMENT_TYPES = 0;
  public static final int INVALID_NAME = 4;
  public static final int INVALID_RESOURCE = 0;
  public static final int INVALID_SIBLING = 5;
  public static final int IO_EXCEPTION = 6;
  public static final int NAME_COLLISION = 7;
  public static final int NO_ELEMENTS_TO_PROCESS = 8;
  public static final int NULL_NAME = 0;
  public static final int READ_ONLY = 9;
  public static final int UPDATE_CONFLICT = 0;
}
