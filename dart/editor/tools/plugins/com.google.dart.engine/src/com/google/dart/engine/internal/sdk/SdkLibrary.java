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
package com.google.dart.engine.internal.sdk;

/**
 * Instances of the class {@code SdkLibrary} represent the information known about a single library
 * within the SDK.
 */
public class SdkLibrary {
  /**
   * The short name of the library. This is the name used after {@code dart:} in a URI.
   */
  private String shortName = null;

  /**
   * The path to the file defining the library. The path is relative to the {@code lib} directory
   * within the SDK.
   */
  private String path = null;

  /**
   * The name of the category containing the library. Unless otherwise specified in the libraries
   * file all libraries are assumed to be shared between server and client.
   */
  private String category = "Shared"; //$NON-NLS-1$

  /**
   * A flag indicating whether the library is documented.
   */
  private boolean documented = true;

  /**
   * A flag indicating whether the library is an implementation library.
   */
  private boolean implementation = false;

  /**
   * An encoding of which platforms this library is intended to work on.
   */
  private int platforms = 0;

  /**
   * The bit mask used to access the bit representing the flag indicating whether a library is
   * intended to work on the dart2js platform.
   */
  public static final int DART2JS_PLATFORM = 1;

  /**
   * The bit mask used to access the bit representing the flag indicating whether a library is
   * intended to work on the VM platform.
   */
  public static final int VM_PLATFORM = 2;

  /**
   * Initialize a newly created library to represent the library with the given name.
   * 
   * @param name the short name of the library
   */
  public SdkLibrary(String name) {
    this.shortName = name;
  }

  /**
   * Return the name of the category containing the library.
   * 
   * @return the name of the category containing the library
   */
  public String getCategory() {
    return category;
  }

  /**
   * Return the path to the file defining the library. The path is relative to the {@code lib}
   * directory within the SDK.
   * 
   * @return the path to the file defining the library
   */
  public String getPath() {
    return path;
  }

  /**
   * Return the short name of the library. This is the name used after {@code dart:} in a URI.
   * 
   * @return the short name of the library
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * Return {@code true} if this library can be compiled to JavaScript by dart2js.
   * 
   * @return {@code true} if this library can be compiled to JavaScript by dart2js
   */
  public boolean isDart2JsLibrary() {
    return (platforms & DART2JS_PLATFORM) != 0;
  }

  /**
   * Return {@code true} if the library is documented.
   * 
   * @return {@code true} if the library is documented
   */
  public boolean isDocumented() {
    return documented;
  }

  /**
   * Return {@code true} if the library is an implementation library.
   * 
   * @return {@code true} if the library is an implementation library
   */
  public boolean isImplementation() {
    return implementation;
  }

  /**
   * Return {@code true} if this library can be run on the VM.
   * 
   * @return {@code true} if this library can be run on the VM
   */
  public boolean isVmLibrary() {
    return (platforms & VM_PLATFORM) != 0;
  }

  /**
   * Set the name of the category containing the library to the given name.
   * 
   * @param category the name of the category containing the library
   */
  public void setCategory(String category) {
    this.category = category;
  }

  /**
   * Record that this library can be compiled to JavaScript by dart2js.
   */
  public void setDart2JsLibrary() {
    platforms |= DART2JS_PLATFORM;
  }

  /**
   * Set whether the library is documented to match the given value.
   * 
   * @param documented {@code true} if the library is documented
   */
  public void setDocumented(boolean documented) {
    this.documented = documented;
  }

  /**
   * Set whether the library is an implementation library to match the given value.
   * 
   * @param implementation {@code true} if the library is an implementation library
   */
  public void setImplementation(boolean implementation) {
    this.implementation = implementation;
  }

  /**
   * Set the path to the file defining the library to the given path. The path is relative to the
   * {@code lib} directory within the SDK.
   * 
   * @param path the path to the file defining the library
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Record that this library can be run on the VM.
   */
  public void setVmLibrary() {
    platforms |= VM_PLATFORM;
  }
}
