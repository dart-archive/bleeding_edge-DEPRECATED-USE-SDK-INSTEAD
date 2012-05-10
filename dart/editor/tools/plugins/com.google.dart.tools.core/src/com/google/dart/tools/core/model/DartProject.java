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
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * The interface <code>DartProject</code> defines the behavior of objects representing a project
 * that has a Dart nature.
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
   * Find the type defined in this project with the given qualified name.
   * 
   * @param qualifiedTypeName the name of the type to be returned
   * @param owner
   * @return the type defined in this project with the given qualified name
   * @throws DartModelException if the types defined in this project cannot be determined for some
   *           reason
   * @deprecated Dart projects don't have a notion of a qualified type name; this method should be
   *             removed
   */
  @Deprecated
  public Type findType(String qualifiedTypeName, WorkingCopyOwner owner) throws DartModelException;

  /**
   * Find all of the types defined in this project with the given name.
   * 
   * @param typeName the name of the types to be returned
   * @return all of the types defined in this project with the given name
   * @throws DartModelException if the types defined in this project cannot be determined for some
   *           reason
   */
  public Type[] findTypes(String typeName) throws DartModelException;

  /**
   * Return the output location for this project's artifacts.
   * <p>
   * The artifact output location is where derived resources are ordinarily generated. Examples of
   * derived resources include source mapping files (.map) and secondary JavaScript (.js).
   * 
   * @return the workspace-relative absolute path of the default output folder
   * @see #getOutputLocation()
   */
  public IPath getArtifactLocation();

  /**
   * Return an array containing all of the libraries defined in this project.
   * 
   * @return an array containing all of the libraries defined in this project
   * @throws DartModelException if the libraries defined in this project cannot be determined for
   *           some reason
   */
  public DartLibrary[] getDartLibraries() throws DartModelException;

  /**
   * Return the library associated with the given resource.
   * 
   * @param resource the resource associated with the library to be returned
   * @return the library associated with the given resource
   * @throws DartModelException if the children of this project cannot be accessed for some reason
   */
  public DartLibrary getDartLibrary(IResource resource) throws DartModelException;

  /**
   * Return the library associated with the given resource path.
   * 
   * @param resourcePath the resource path associated with the library to be returned
   * @return the library associated with the given resource path
   * @throws DartModelException if the children of this project cannot be accessed for some reason
   */
  public DartLibrary getDartLibrary(String resourcePath) throws DartModelException;

  /**
   * Return the default output location. This is used when resetting the project's output location
   * to the default.
   * 
   * @return the default output location
   */
  public IPath getDefaultOutputFullPath();

  /**
   * Return the table containing the mapping between the Html files in the project which have a dart
   * related script tag and the corresponding library definition file.
   * 
   * @return table of html file to library file mapping
   * @throws CoreException
   */
  public HashMap<String, List<String>> getHtmlMapping() throws CoreException;

  /**
   * Return an array of non-Dart resources directly contained in this project. It does not
   * transitively answer non-Dart resources contained in folders; these would have to be explicitly
   * iterated over.
   * <p>
   * Non-Dart resources includes other files and folders located in the project not accounted for by
   * any of its libraries.
   * 
   * @return an array of non-Dart resources (<code>IFile</code>s and/or <code>IFolder</code>s)
   *         directly contained in this project
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public IResource[] getNonDartResources() throws DartModelException;

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
   * Return the output location for this project as a workspace- relative absolute path.
   * <p>
   * The output location is where the main derived resources are generated. Examples of these
   * resources are .app.js files.
   * 
   * @return the workspace-relative absolute path of the default output folder
   * @throws DartModelException if this element does not exist
   * @see #setOutputLocation(org.eclipse.core.runtime.IPath, IProgressMonitor)
   * @see IClasspathEntry#getOutputLocation()
   */
  public IPath getOutputLocation() throws DartModelException;

  /**
   * Return the project corresponding to this Dart project.
   * 
   * @return the project corresponding to this Dart project
   */
  public IProject getProject();

  // /**
  // * Create and return a new evaluation context.
  // *
  // * @return the evaluation context that was created
  // */
  // public EvaluationContext newEvaluationContext();

  /**
   * Return <code>true</code> if this project has been built at least once and thus has a build
   * state.
   * 
   * @return <code>true</code> if this project has been built at least once
   */
  public boolean hasBuildState();

  /**
   * Removes the new {@link IFile} as a top-level library in this project.
   * 
   * @param file the new library file
   */
  public boolean removeLibraryFile(IFile file);

  /**
   * Helper method for setting one option value only.
   * <p>
   * Equivalent to:
   * 
   * <pre>
   *  Map options = this.getOptions(false);
   *  map.put(optionName, optionValue);
   *  this.setOptions(map);
   * </pre>
   * <p>
   * For a complete description of the configurable options, see
   * {@link DartCore#getDefaultOptions()}.
   * 
   * @param optionName the name of an option
   * @param optionValue the value of the option to set. If <code>null</code>, then the option is
   *          removed from the project preferences.
   * @throws NullPointerException if <code>optionName</code> is <code>null</code> (see
   *           {@link org.osgi.service.prefs.Preferences#put(String, String)})
   */
  public void setOption(String optionName, String optionValue);

  /**
   * Sets the project custom options. All and only the options explicitly included in the given
   * table are remembered; all previous option settings are forgotten, including ones not explicitly
   * mentioned.
   * <p>
   * For a complete description of the configurable options, see
   * {@link DartCore#getDefaultOptions()}.
   * 
   * @param newOptions the new options, or <code>null</code> to flush all custom options (clients
   *          will automatically get the global DartCore options).
   */
  public void setOptions(Map<String, String> newOptions);

  /**
   * Sets the default output location of this project to the location described by the given
   * workspace-relative absolute path.
   * <p>
   * The default output location is where derived resources are ordinarily generated. Examples of
   * derived resources include JavaScript (.js) files and metadata (.meta) files.
   * 
   * @param path the workspace-relative absolute path of the default output folder
   * @param monitor the progress monitor
   * @throws DartModelException if the classpath could not be set. Reasons include:
   *           <ul>
   *           <li>This Dart element does not exist (ELEMENT_DOES_NOT_EXIST) </li> <li>The path
   *           refers to a location not contained in this project (<code>PATH_OUTSIDE_PROJECT</code>
   *           ) <li>The path is not an absolute path ( <code>RELATIVE_PATH</code>) <li>The output
   *           location is being modified during resource change event notification (CORE_EXCEPTION)
   *           </ul>
   * @see #getOutputLocation()
   */
  public void setOutputLocation(IPath path, IProgressMonitor monitor) throws DartModelException;

  /**
   * Updates the html file to library mapping table
   * 
   * @param htmlFileName the name of the html file
   * @param libraries the list of libraries referenced in the html file
   * @param add true adds the entry if true, false removes the entry
   * @throws DartModelException
   */
  public void updateHtmlMapping(String htmlFileName, List<String> libraries, boolean add)
      throws DartModelException;

}
