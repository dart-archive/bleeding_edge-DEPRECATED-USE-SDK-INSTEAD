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
package com.google.dart.engine.sdk;

/**
 * Represents a single library in the SDK
 */
public interface SdkLibrary {

  /**
   * Return the name of the category containing the library.
   * 
   * @return the name of the category containing the library
   */
  public String getCategory();

  /**
   * Return the path to the file defining the library. The path is relative to the {@code lib}
   * directory within the SDK.
   * 
   * @return the path to the file defining the library
   */
  public String getPath();

  /**
   * Return the short name of the library. This is the URI of the library, including {@code dart:}.
   * 
   * @return the short name of the library
   */
  public String getShortName();

  /**
   * Return {@code true} if this library can be compiled to JavaScript by dart2js.
   * 
   * @return {@code true} if this library can be compiled to JavaScript by dart2js
   */
  public boolean isDart2JsLibrary();

  /**
   * Return {@code true} if the library is documented.
   * 
   * @return {@code true} if the library is documented
   */
  public boolean isDocumented();

  /**
   * Return {@code true} if the library is an implementation library.
   * 
   * @return {@code true} if the library is an implementation library
   */
  public boolean isImplementation();

  /**
   * Return {@code true} if library is internal can be used only by other SDK libraries.
   * 
   * @return {@code true} if library is internal can be used only by other SDK libraries
   */
  public boolean isInternal();

  /**
   * Return {@code true} if library can be used for both client and server.
   * 
   * @return {@code true} if this library can be used for both client and server.
   */
  public boolean isShared();

  /**
   * Return {@code true} if this library can be run on the VM.
   * 
   * @return {@code true} if this library can be run on the VM
   */
  public boolean isVmLibrary();

}
