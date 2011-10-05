/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Standard images provided by the JavaScript UI plug-in. This class offers access to the standard
 * images in two forms:
 * <ul>
 * <li>Use <code>ISharedImages.getImage(IMG_OBJS_<i>FOO</i>)</code> to access the shared standard
 * <code>Image</code> object (caller must not dispose of image).</li>
 * <li>Use <code>ISharedImages.getImageDescriptor(IMG_OBJS_<i>FOO</i>)</code> to access the standard
 * <code>ImageDescriptor</code> object (caller is responsible for disposing of any
 * <code>Image</code> objects it creates using this descriptor).</li>
 * </ul>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public interface ISharedImages {

  /**
   * Key to access the shared image or image descriptor for a JavaScript compilation unit.
   */
  public static final String IMG_OBJS_CUNIT = DartPluginImages.IMG_OBJS_CUNIT;

  /**
   * Key to access the shared image or image descriptor for a JavaScript class file.
   */
  public static final String IMG_OBJS_CFILE = DartPluginImages.IMG_OBJS_CFILE;

  /**
   * Key to access the shared image or image descriptor for a JAR archive.
   */
  public static final String IMG_OBJS_JAR = DartPluginImages.IMG_OBJS_JAR;

  /**
   * Key to access the shared image or image descriptor for a JAR with source.
   */
  public static final String IMG_OBJS_JAR_WITH_SOURCE = DartPluginImages.IMG_OBJS_JAR_WSRC;

  /**
   * Key to access the shared image or image descriptor for external archives.
   */
  public static final String IMG_OBJS_EXTERNAL_ARCHIVE = DartPluginImages.IMG_OBJS_EXTJAR;

  /**
   * Key to access the shared image or image descriptor for external archives with source.
   */
  public static final String IMG_OBJS_EXTERNAL_ARCHIVE_WITH_SOURCE = DartPluginImages.IMG_OBJS_EXTJAR_WSRC;

  /**
   * Key to access the shared image or image descriptor for a classpath variable entry.
   */
  public static final String IMG_OBJS_CLASSPATH_VAR_ENTRY = DartPluginImages.IMG_OBJS_ENV_VAR;

  /**
   * Key to access the shared image or image descriptor for a library (class path container).
   */
  public static final String IMG_OBJS_LIBRARY = DartPluginImages.IMG_OBJS_LIBRARY;

  /**
   * Key to access the shared image or image descriptor for a package fragment root.
   */
  public static final String IMG_OBJS_PACKFRAG_ROOT = DartPluginImages.IMG_OBJS_PACKFRAG_ROOT;

  /**
   * Key to access the shared image or image descriptor for a package.
   */
  public static final String IMG_OBJS_PACKAGE = DartPluginImages.IMG_OBJS_PACKAGE;

  /**
   * Key to access the shared image or image descriptor for an empty package.
   */
  public static final String IMG_OBJS_EMPTY_PACKAGE = DartPluginImages.IMG_OBJS_EMPTY_PACKAGE;

  /**
   * Key to access the shared image or image descriptor for a logical package.
   */
  public static final String IMG_OBJS_LOGICAL_PACKAGE = DartPluginImages.IMG_OBJS_LOGICAL_PACKAGE;

  /**
   * Key to access the shared image or image descriptor for an empty logical package.
   */
  public static final String IMG_OBJS_EMPTY_LOGICAL_PACKAGE = DartPluginImages.IMG_OBJS_EMPTY_LOGICAL_PACKAGE;

  /**
   * Key to access the shared image or image descriptor for a class.
   */
  public static final String IMG_OBJS_CLASS = DartPluginImages.IMG_OBJS_CLASS;

  /**
   * Key to access the shared image or image descriptor for a class with default visibility.
   */
  public static final String IMG_OBJS_CLASS_DEFAULT = DartPluginImages.IMG_OBJS_CLASS_DEFAULT;

  /**
   * Key to access the shared image or image descriptor for a public inner class.
   */
  public static final String IMG_OBJS_INNER_CLASS_PUBLIC = DartPluginImages.IMG_OBJS_INNER_CLASS_PUBLIC;

  /**
   * Key to access the shared image or image descriptor for a inner class with default visibility.
   */
  public static final String IMG_OBJS_INNER_CLASS_DEFAULT = DartPluginImages.IMG_OBJS_INNER_CLASS_DEFAULT;

  /**
   * Key to access the shared image or image descriptor for a protected inner class.
   */
  public static final String IMG_OBJS_INNER_CLASS_PROTECTED = DartPluginImages.IMG_OBJS_INNER_CLASS_PROTECTED;

  /**
   * Key to access the shared image or image descriptor for a private inner class.
   */
  public static final String IMG_OBJS_INNER_CLASS_PRIVATE = DartPluginImages.IMG_OBJS_INNER_CLASS_PRIVATE;

  /**
   * Key to access the shared image or image descriptor for an interface.
   */
  public static final String IMG_OBJS_INTERFACE = DartPluginImages.IMG_OBJS_INTERFACE;

  /**
   * Key to access the shared image or image descriptor for an interface with default visibility.
   */
  public static final String IMG_OBJS_INTERFACE_DEFAULT = DartPluginImages.IMG_OBJS_INTERFACE_DEFAULT;

  /**
   * Key to access the shared image or image descriptor for a public inner interface.
   */
  public static final String IMG_OBJS_INNER_INTERFACE_PUBLIC = DartPluginImages.IMG_OBJS_INNER_INTERFACE_PUBLIC;

  /**
   * Key to access the shared image or image descriptor for an inner interface with default
   * visibility.
   */
  public static final String IMG_OBJS_INNER_INTERFACE_DEFAULT = DartPluginImages.IMG_OBJS_INNER_INTERFACE_DEFAULT;

  /**
   * Key to access the shared image or image descriptor for a protected inner interface.
   */
  public static final String IMG_OBJS_INNER_INTERFACE_PROTECTED = DartPluginImages.IMG_OBJS_INNER_INTERFACE_PROTECTED;

  /**
   * Key to access the shared image or image descriptor for a private inner interface.
   */
  public static final String IMG_OBJS_INNER_INTERFACE_PRIVATE = DartPluginImages.IMG_OBJS_INNER_INTERFACE_PRIVATE;

  /**
   * Key to access the shared image or image descriptor for a package declaration.
   */
  public static final String IMG_OBJS_PACKDECL = DartPluginImages.IMG_OBJS_PACKDECL;

  /**
   * Key to access the shared image or image descriptor for an import container.
   */
  public static final String IMG_OBJS_IMPCONT = DartPluginImages.IMG_OBJS_IMPCONT;

  /**
   * Key to access the shared image or image descriptor for an import statement.
   */
  public static final String IMG_OBJS_IMPDECL = DartPluginImages.IMG_OBJS_IMPDECL;

  /** Key to access the shared image or image descriptor for a public member. */
  public static final String IMG_OBJS_PUBLIC = DartPluginImages.IMG_MISC_PUBLIC;

  /** Key to access the shared image or image descriptor for a protected member. */
  public static final String IMG_OBJS_PROTECTED = DartPluginImages.IMG_MISC_PROTECTED;

  /** Key to access the shared image or image descriptor for a private member. */
  public static final String IMG_OBJS_PRIVATE = DartPluginImages.IMG_MISC_PRIVATE;

  /**
   * Key to access the shared image or image descriptor for class members with default visibility.
   */
  public static final String IMG_OBJS_DEFAULT = DartPluginImages.IMG_MISC_DEFAULT;

  /**
   * Key to access the shared image or image descriptor for a public field.
   */
  public static final String IMG_FIELD_PUBLIC = DartPluginImages.IMG_FIELD_PUBLIC;

  /**
   * Key to access the shared image or image descriptor for a protected field.
   */
  public static final String IMG_FIELD_PROTECTED = DartPluginImages.IMG_FIELD_PROTECTED;

  /**
   * Key to access the shared image or image descriptor for a private field.
   */
  public static final String IMG_FIELD_PRIVATE = DartPluginImages.IMG_FIELD_PRIVATE;

  /**
   * Key to access the shared image or image descriptor for a field with default visibility.
   */
  public static final String IMG_FIELD_DEFAULT = DartPluginImages.IMG_FIELD_DEFAULT;

  /**
   * Key to access the shared image or image descriptor for a local variable.
   */
  public static final String IMG_OBJS_LOCAL_VARIABLE = DartPluginImages.IMG_OBJS_LOCAL_VARIABLE;

  /**
   * Key to access the shared image or image descriptor for a enum type.
   */
  public static final String IMG_OBJS_ENUM = DartPluginImages.IMG_OBJS_ENUM;

  /**
   * Key to access the shared image or image descriptor for a enum type with default visibility.
   */
  public static final String IMG_OBJS_ENUM_DEFAULT = DartPluginImages.IMG_OBJS_ENUM_DEFAULT;

  /**
   * Key to access the shared image or image descriptor for a enum type with protected visibility.
   */
  public static final String IMG_OBJS_ENUM_PROTECTED = DartPluginImages.IMG_OBJS_ENUM_PROTECTED;

  /**
   * Key to access the shared image or image descriptor for a enum type with private visibility.
   */
  public static final String IMG_OBJS_ENUM_PRIVATE = DartPluginImages.IMG_OBJS_ENUM_PRIVATE;

  /**
   * Key to access the shared image or image descriptor for a annotation type.
   */
  public static final String IMG_OBJS_ANNOTATION = DartPluginImages.IMG_OBJS_ANNOTATION;

  /**
   * Key to access the shared image or image descriptor for a annotation type with default
   * visibility.
   */
  public static final String IMG_OBJS_ANNOTATION_DEFAULT = DartPluginImages.IMG_OBJS_ANNOTATION_DEFAULT;

  /**
   * Key to access the shared image or image descriptor for a annotation type with protected
   * visibility.
   */
  public static final String IMG_OBJS_ANNOTATION_PROTECTED = DartPluginImages.IMG_OBJS_ANNOTATION_PROTECTED;

  /**
   * Key to access the shared image or image descriptor for a annotation type with private
   * visibility.
   */
  public static final String IMG_OBJS_ANNOTATION_PRIVATE = DartPluginImages.IMG_OBJS_ANNOTATION_PRIVATE;

  /**
   * Key to access the shared image or image descriptor for javadoc tags.
   */
  public static final String IMG_OBJS_JAVADOCTAG = DartPluginImages.IMG_OBJS_JAVADOCTAG;

  /**
   * Returns the shared image managed under the given key.
   * <p>
   * Note that clients <b>must not</b> dispose the image returned by this method.
   * </p>
   * 
   * @param key the image key; one of the <code>IMG_OBJS_* </code> constants
   * @return the shared image managed under the given key, or <code>null</code> if none
   */
  public Image getImage(String key);

  /**
   * Returns the image descriptor managed under the given key.
   * 
   * @param key the image key; one of the <code>IMG_OBJS_* </code> constants
   * @return the image descriptor managed under the given key, or <code>null</code> if none
   */
  public ImageDescriptor getImageDescriptor(String key);
}
