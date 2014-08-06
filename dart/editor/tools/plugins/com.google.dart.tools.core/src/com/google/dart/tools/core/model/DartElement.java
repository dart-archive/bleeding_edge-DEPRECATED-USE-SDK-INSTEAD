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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

/**
 * The interface <code>DartElement</code> defines the behavior of objects representing some element
 * within the Dart model. Dart model elements are exposed to clients as handles to the actual
 * underlying element. The Dart model may hand out any number of handles for each element. Handles
 * that refer to the same element are guaranteed to be equal, but not necessarily identical.
 * <p>
 * Methods annotated as "handle-only" do not require underlying elements to exist. Methods that
 * require underlying elements to exist throw a {@link DartModelException} when an underlying
 * element is missing. The method {@link DartModelException#isDoesNotExist()} can be used to
 * recognize this common special case.
 * <p>
 * This interface is not intended to be implemented by clients.
 * 
 * @coverage dart.tools.core.model
 */
public interface DartElement extends IAdaptable {
  /**
   * A constant representing a Dart model (workspace level object). A Dart element with this type
   * can be safely cast to {@link DartModel}.
   */
  public static int DART_MODEL = 1;

  /**
   * A constant representing a Dart project. A Dart element with this type can be safely cast to
   * {@link DartProject}.
   */
  public static int DART_PROJECT = 2;

  /**
   * A constant representing an HTML file. A Dart element with this type can be safely cast to
   * {@link HTMLFile}.
   */
  public static int HTML_FILE = 3;

  /**
   * A constant representing a Dart library. A Dart element with this type can be safely cast to
   * {@link DartLibrary}.
   */
  public static int LIBRARY = 4;

  /**
   * A constant representing a library folder. A Dart element with this type can be safely cast to
   * {@link DartLibraryFolder}.
   */
  public static int DART_LIBRARY_FOLDER = 16;

  /**
   * A constant representing a Dart compilation unit. A Dart element with this type can be safely
   * cast to {@link CompilationUnit}.
   */
  public static int COMPILATION_UNIT = 7;

  /**
   * A constant representing a type (a class or interface). A Dart element with this type can be
   * safely cast to {@link Type}.
   */
  public static int TYPE = 8;

  /**
   * A constant representing a field. A Dart element with this type can be safely cast to
   * {@link Field}.
   */
  public static int FIELD = 10;

  /*
   * A constant representing a method or constructor. A Dart element with this type can be safely
   * cast to {@link Method}.
   */
  //public static int METHOD = 11;

  /**
   * A constant representing an import container. A Dart element with this type can be safely cast
   * to {@link DartImportContainer}.
   */
  public static int IMPORT_CONTAINER = 12;

  /*
   * A constant representing an import. A Dart element with this type can be safely cast to
   * {@link DartImport}.
   */
  //public static int IMPORT = 13;

  /**
   * A constant representing a function. A Dart element with this type can be safely cast to
   * {@link DartFunction}.
   */
  public static int FUNCTION = 14;

  /*
   * A constant representing a function type alias. A Dart element with this type can be safely cast
   * to {@link DartFunctionTypeAlias}.
   */
  //public static int FUNCTION_TYPE_ALIAS = 15;

  /**
   * A constant representing a class type alias.
   */
  public static int CLASS_TYPE_ALIAS = 18;

  /*
   * A constant representing a local variable or parameter. A Dart element with this type can be
   * safely cast to {@link DartVariableDeclaration}.
   */
  //public static int VARIABLE = 17;

  /**
   * A constant representing a type parameter. A Dart element with this type can be safely cast to
   * {@link DartTypeParameter}.
   */
  public static int TYPE_PARAMETER = 19;

  /**
   * An empty array of elements.
   */
  public static final DartElement[] EMPTY_ARRAY = new DartElement[0];

  /**
   * Return <code>true</code> if this Dart element exists in the model.
   * <p>
   * Dart elements are handle objects that may or may not be backed by an actual element. Dart
   * elements that are backed by an actual element are said to "exist", and this method returns
   * <code>true</code>. For Dart elements that are not working copies, it is always the case that if
   * the element exists, then its parent also exists (provided it has one) and includes the element
   * as one of its children. It is therefore possible to navigated to any existing Dart element from
   * the root of the Dart model along a chain of existing Dart elements. On the other hand, working
   * copies are said to exist until they are destroyed (with <code>WorkingCopy.destroy</code>).
   * Unlike regular Dart elements, a working copy never shows up among the children of its parent
   * element (which may or may not exist).
   * 
   * @return <code>true</code> if this element exists in the Dart model
   */
  public boolean exists();

  /**
   * Return the first ancestor of this element that has the given type, or <code>null</code> if no
   * such ancestor can be found.
   * 
   * @param ancestorClass the class of element to be returned
   * @return the first ancestor of this element that has the given type
   */
  public <E extends DartElement> E getAncestor(Class<E> ancestorClass);

  /**
   * Return the children of this element, or an empty array if there are no children.
   * 
   * @return the children of this element
   * @throws DartModelException if the children of the element cannot be determined
   */
  public DartElement[] getChildren() throws DartModelException;

  /**
   * Return the resource that corresponds directly to this element, or <code>null</code> if there is
   * no resource that corresponds directly to this element.
   * <p>
   * For example, the corresponding resource for a <code>CompilationUnit</code> is its underlying
   * <code>IFile</code>. There is no corresponding resource for <code>Type</code>s,
   * <code>Method</code>s, <code>Field</code>s, etc.
   * 
   * @return the resource that corresponds directly to this element
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its corresponding resource
   */
  public IResource getCorrespondingResource() throws DartModelException;

  /**
   * Return the Dart model that contains this element, or <code>null</code> if this element is not
   * contained in the model. This is a handle-only method.
   * 
   * @return the Dart model that contains this element
   */
  public DartModel getDartModel();

  /**
   * Return the Dart project that contains this element, or <code>null</code> if this element is not
   * contained in any project. This is a handle-only method.
   * 
   * @return the Dart project that contains this element
   */
  public DartProject getDartProject();

  /**
   * Return the name of this element as it should appear in the user interface. This is a
   * handle-only method.
   * 
   * @return the name of this element
   */
  public String getElementName();

  /**
   * Return the type of this element, encoded as an integer. The returned value will be one of the
   * constants declared in {@link DartElement}. This is a handle-only method.
   * 
   * @return the type of this element
   */
  public int getElementType();

  /**
   * Return an identifier that can be used to identify this element. The format of the identifier is
   * not specified, but the identifier is stable across workspace sessions and can be used to
   * recreate this handle via the {@link DartCore#create(String)} method.
   * 
   * @return an identifier that can be used to identify this element
   */
  public String getHandleIdentifier();

  /**
   * Return the first openable parent. If this element is openable, the element itself is returned.
   * Return <code>null</code> if this element doesn't have an openable parent. This is a handle-only
   * method.
   * 
   * @return the first openable parent
   */
  public OpenableElement getOpenable();

  /**
   * Return the parent of this element, or <code>null</code> if this element does not have a parent.
   * This is a handle-only method.
   * 
   * @return the parent of this element
   */
  public DartElement getParent();

  /**
   * Return the path to the innermost resource enclosing this element. If this element is not
   * included in an external library, the path returned is the full, absolute path to the underlying
   * resource, relative to the workbench. If this element is included in an external library, the
   * path returned is the absolute path to the archive or to the folder in the file system. This is
   * a handle-only method.
   * 
   * @return the path to the innermost resource enclosing this element
   */
  public IPath getPath();

  /**
   * Return the innermost resource enclosing this element, or <code>null</code> if this element is
   * not enclosed in a resource. This is a handle-only method.
   * 
   * @return the innermost resource enclosing this element
   */
  public IResource getResource();

  /**
   * Return the smallest underlying resource that contains this element, or <code>null</code> if
   * this element is not contained in a resource.
   * 
   * @return the underlying resource, or <code>null</code> if none
   * @throws DartModelException if this element does not exist or if an exception occurs while
   *           accessing its underlying resource
   */
  public IResource getUnderlyingResource() throws DartModelException;

  /**
   * Return <code>true</code> if this Dart element is read-only. An element is read-only if its
   * structure cannot be modified by the Dart model.
   * <p>
   * Note this is different from IResource.isReadOnly(). For example, .jar files are read-only as
   * the Dart model doesn't know how to add/remove elements in this file, but the underlying IFile
   * can be writable.
   * <p>
   * This is a handle-only method.
   * 
   * @return <code>true</code> if this element is read-only
   */
  public boolean isReadOnly();
}
