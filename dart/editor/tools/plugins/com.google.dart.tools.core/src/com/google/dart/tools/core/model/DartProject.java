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
package com.google.dart.tools.core.model;

import com.google.dart.tools.core.DartCore;

import org.eclipse.core.resources.IProject;

import java.util.Hashtable;

/**
 * The interface <code>DartProject</code> defines the behavior of objects representing a project
 * that has a Dart nature.
 * 
 * @coverage dart.tools.core.model
 */
public interface DartProject extends ParentElement, OpenableElement {
  /**
   * Find the type defined in this project with the given qualified name.
   * 
   * @param typeName the name of the type to be returned
   * @return the type defined in this project with the given qualified name
   * @throws DartModelException if the types defined in this project cannot be determined for some
   *           reason
   * @deprecated Dart projects don't have a notion of a qualified type name; this method should be
   *             removed
   */
  @Deprecated
  public Type findType(String qualifiedTypeName) throws DartModelException;

  /**
   * Return an array containing all of the libraries defined in this project.
   * 
   * @return an array containing all of the libraries defined in this project
   * @throws DartModelException if the libraries defined in this project cannot be determined for
   *           some reason
   */
  public DartLibrary[] getDartLibraries() throws DartModelException;

  /**
   * Utility method for returning one option value only. Equivalent to
   * <code>this.getOptions(inheritDartCoreOptions).get(optionName)</code> Note that it may answer
   * <code>null</code> if this option does not exist, or if there is no custom value for it.
   * <p>
   * For a complete description of the configurable options, see
   * {@link DartCore#getDefaultOptions()}.
   * </p>
   * 
   * @param optionName the name of the option whose value is to be returned
   * @param inheritDartCoreOptions <code>true</code> if DartCore options should be inherited as well
   * @return the value of a given option
   */
  public String getOption(String optionName, boolean inheritDartCoreOptions);

  /**
   * Return the table containing the current custom options for this project. Projects remember
   * their custom options, in other words, only the options different from the the {@link DartCore}
   * global options for the workspace. A boolean argument allows to directly merge the project
   * options with global ones from {@link DartCore}.
   * <p>
   * For a complete description of the configurable options, see
   * {@link DartCore#getDefaultOptions()}.
   * </p>
   * 
   * @param inheritDartCoreOptions <code>true</code> if {@link DartCore} options should be inherited
   *          as well
   * @return table of current settings of all options
   */
  public Hashtable<String, String> getOptions(boolean inheritDartCoreOptions);

  /**
   * Return the project corresponding to this Dart project.
   * 
   * @return the project corresponding to this Dart project
   */
  public IProject getProject();
}
