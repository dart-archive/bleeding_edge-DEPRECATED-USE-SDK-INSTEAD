package com.google.dart.tools.core.internal.refactoring.descriptors;

import com.google.common.collect.Lists;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

public class DartRefactoringDescriptorUtil {

  private static final String LOWER_CASE_FALSE = "false"; //$NON-NLS-1$
  private static final String LOWER_CASE_TRUE = "true"; //$NON-NLS-1$

  /**
   * Converts the specified element to an input handle.
   * 
   * @param project the project, or <code>null</code> for the workspace
   * @param element the element
   * @return a corresponding input handle. Note: if the given project is not the
   *         <code>element</code>'s project, then the full handle is returned
   */
  public static String elementToHandle(final String project, final DartElement element) {
    final String handle = element.getHandleIdentifier();
    if (project != null && !(element instanceof DartProject)) {
      DartProject dartProject = element.getDartProject();
      if (project.equals(dartProject.getElementName())) {
        final String id = dartProject.getHandleIdentifier();
        return handle.substring(id.length());
      }
    }
    return handle;
  }

  /**
   * Create the name for accessing the ith element of an attribute.
   * 
   * @param attribute the base attribute
   * @param index the index that should be accessed
   * @return the attribute name for the ith element of an attribute
   */
  public static String getAttributeName(String attribute, int index) {
    return attribute + index;
  }

  /**
   * Retrieves a <code>boolean</code> attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key in the map
   * @return the <code>boolean</code> value of the attribute
   * @throws IllegalArgumentException if the attribute does not exist or is not a boolean
   */
  public static boolean getBoolean(Map<String, String> map, String attribute)
      throws IllegalArgumentException {
    String value = getString(map, attribute).toLowerCase();
    //Boolean.valueOf(value) does not complain about wrong values
    if (LOWER_CASE_TRUE.equals(value)) {
      return true;
    }
    if (LOWER_CASE_FALSE.equals(value)) {
      return false;
    }
    throw new IllegalArgumentException(
        "The attribute '" + attribute + "' does not contain a valid boolean: '" + value + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }

  /**
   * Retrieves a <code>boolean</code> attribute from map. If the attribute does not exist it returns
   * the default value
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key in the map
   * @param defaultValue the default value to use if the attribute does not exist
   * @return the <code>boolean</code> value of the attribute or the specified default value if the
   *         attribute does not exist
   * @throws IllegalArgumentException if the attribute does not contain a valid value
   */
  public static boolean getBoolean(Map<String, String> map, String attribute, boolean defaultValue)
      throws IllegalArgumentException {
    String value = getString(map, attribute, true);
    if (value == null) {
      return defaultValue;
    }
    value = value.toLowerCase();
    //Boolean.valueOf(value) does not complain about wrong values
    if (LOWER_CASE_TRUE.equals(value)) {
      return true;
    }
    if (LOWER_CASE_FALSE.equals(value)) {
      return false;
    }
    throw new IllegalArgumentException(
        "The attribute '" + attribute + "' does not contain a valid boolean: '" + value + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }

  /**
   * Retrieves a <code>boolean[]</code> attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param countAttribute the attribute that contains the number of elements
   * @param arrayAttribute the attribute name where the values are stored. The index starting from
   *          '0' is appended to this
   * @param offset the starting index for arrayAttribute
   * @return the <code>boolean[]</code>
   * @throws IllegalArgumentException if any of the attribute does not exist or is not a boolean
   */
  public static boolean[] getBooleanArray(Map<String, String> map, String countAttribute,
      String arrayAttribute, int offset) throws IllegalArgumentException {
    int count = getInt(map, countAttribute);
    boolean[] result = new boolean[count];
    for (int i = 0; i < count; i++) {
      result[i] = getBoolean(map, getAttributeName(arrayAttribute, i + offset));
    }
    return result;
  }

  /**
   * Retrieves a <code>{@link DartElement}</code> attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key in the map
   * @param project the project for resolving the Dart element. Can be <code>null</code> for
   *          workspace
   * @return a {@link DartElement} or <code>null</code>
   * @throws IllegalArgumentException if the attribute does not exist or is not a Dart element
   * @see #handleToElement(WorkingCopyOwner, String, String, boolean)
   */
  public static DartElement getDartElement(Map<String, String> map, String attribute, String project)
      throws IllegalArgumentException {
    return getDartElement(map, attribute, project, false);
  }

  /**
   * Retrieves a <code>{@link DartElement}</code> attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key in the map
   * @param project the project for resolving the Dart element. Can be <code>null</code> for
   *          workspace
   * @param allowNull if <code>true</code> a <code>null</code> will be returned if the attribute
   *          does not exist
   * @return a {@link DartElement} or <code>null</code>
   * @throws IllegalArgumentException if the attribute does not exist
   * @see #handleToElement(WorkingCopyOwner, String, String, boolean)
   */
  public static DartElement getDartElement(Map<String, String> map, String attribute,
      String project, boolean allowNull) throws IllegalArgumentException {
    String handle = getString(map, attribute, allowNull);
    if (handle != null) {
      return handleToElement(null, project, handle, false);
    }
    return null;
  }

  /**
   * Retrieves a <code>DartElement[]</code> attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param countAttribute the attribute that contains the number of elements. Can be
   *          <code>null</code> to indicate that no count attribute exists
   * @param arrayAttribute the attribute name where the values are stored. The index starting from
   *          offset is appended to this
   * @param offset the starting index for arrayAttribute
   * @param project the project for resolving the Dart element. Can be <code>null</code> for
   *          workspace
   * @param arrayClass the component type for the resulting array. The resulting array can then be
   *          safely casted to arrayClass[]
   * @return the <code>DartElement[]</code>
   * @throws IllegalArgumentException if any of the attribute does not exist or is not a number
   */
  public static DartElement[] getDartElementArray(Map<String, String> map, String countAttribute,
      String arrayAttribute, int offset, String project, Class<?> arrayClass)
      throws IllegalArgumentException {
    if (countAttribute != null) {
      int count = getInt(map, countAttribute);
      DartElement[] result = (DartElement[]) Array.newInstance(arrayClass, count);
      for (int i = 0; i < count; i++) {
        result[i] = getDartElement(map, getAttributeName(arrayAttribute, i + offset), project);
      }
      return result;
    } else {
      List<DartElement> result = Lists.newArrayList();
      DartElement element = null;
      while ((element = getDartElement(map, arrayAttribute, project, true)) != null) {
        result.add(element);
      }
      return (DartElement[]) result.toArray((Object[]) Array.newInstance(arrayClass, result.size()));
    }
  }

  /**
   * Retrieves an <code>int</code> attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key in the map
   * @return the value of the attribute
   * @throws IllegalArgumentException if the attribute does not exist or is not a number
   */
  public static int getInt(Map<String, String> map, String attribute)
      throws IllegalArgumentException {
    String value = getString(map, attribute);
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "The attribute '" + attribute + "' does not contain a valid int '" + value + "'"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
    }
  }

  /**
   * Retrieves an <code>int</code> attribute from map. If the attribute does not exist it returns
   * the default value.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key in the map
   * @param defaultValue the default value to use if the attribute does not exist
   * @return the <code>int</code> value of the attribute or the specified default value if the
   *         attribute does not exist
   * @throws IllegalArgumentException if the attribute exists but is not a number
   */
  public static int getInt(Map<String, String> map, String attribute, int defaultValue)
      throws IllegalArgumentException {
    String value = getString(map, attribute, true);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "The attribute '" + attribute + "' does not contain a valid int '" + value + "'"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
    }
  }

  /**
   * Retrieves an <code>int[]</code> attribute from map.
   * 
   * @param countAttribute the attribute that contains the number of elements
   * @param arrayAttribute the attribute name where the values are stored. The index starting from
   *          '0' is appended to this
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @return the <code>int[]</code>
   * @throws IllegalArgumentException if any of the attribute does not exist or is not a number
   */
  public static int[] getIntArray(Map<String, String> map, String countAttribute,
      String arrayAttribute) throws IllegalArgumentException {
    int count = getInt(map, countAttribute);
    int[] result = new int[count];
    for (int i = 0; i < count; i++) {
      result[i] = getInt(map, getAttributeName(arrayAttribute, i));
    }
    return result;
  }

  /**
   * Retrieves and resolves an <code>{@link IResource}</code> attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key in the map
   * @param project the project for resolving the resource. Can be <code>null</code> for workspace
   * @return the <code>{@link IResource}</code>, or <code>null</code> if the resource does not exist
   * @throws IllegalArgumentException if the attribute does not exist
   * @see #handleToResource(String, String)
   */
  public static IPath getResourcePath(Map<String, String> map, String attribute, String project)
      throws IllegalArgumentException {
    String handle = getString(map, attribute);
    return handleToResourcePath(project, handle);
  }

  /**
   * Retrieves an <code>IResource[]</code> attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param countAttribute the attribute that contains the number of elements
   * @param arrayAttribute the attribute name where the values are stored. The index starting from
   *          offset is appended to this
   * @param offset the starting index for arrayAttribute
   * @param project the project for resolving the Dart element. Can be <code>null</code> for
   *          workspace
   * @return the <code>IResource[]</code>
   * @throws IllegalArgumentException if any of the attribute does not exist or is not a number
   */
  public static IPath[] getResourcePathArray(Map<String, String> map, String countAttribute,
      String arrayAttribute, int offset, String project) throws IllegalArgumentException {
    int count = getInt(map, countAttribute);
    IPath[] result = new IPath[count];
    for (int i = 0; i < count; i++) {
      result[i] = getResourcePath(map, getAttributeName(arrayAttribute, i + offset), project);
    }
    return result;
  }

  /**
   * Retrieves a {@link String} attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key in the map
   * @return the value of the attribute
   * @throws IllegalArgumentException if the value of the attribute is not a {@link String} or the
   *           attribute does not exist
   */
  public static String getString(Map<String, String> map, String attribute)
      throws IllegalArgumentException {
    return getString(map, attribute, false);
  }

  /**
   * Retrieves a {@link String} attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key in the map
   * @param allowNull if <code>true</code> a <code>null</code> will be returned if the attribute
   *          does not exist
   * @return the value of the attribute
   * @throws IllegalArgumentException if the value of the attribute is not a {@link String} or
   *           allowNull is <code>false</code> and the attribute does not exist
   */
  public static String getString(Map<String, String> map, String attribute, boolean allowNull)
      throws IllegalArgumentException {
    Object object = map.get(attribute);
    if (object == null) {
      if (allowNull) {
        return null;
      }
      throw new IllegalArgumentException(
          "The map does not contain the attribute '" + attribute + "'"); //$NON-NLS-1$//$NON-NLS-2$
    }
    if (object instanceof String) {
      String value = (String) object;
      return value;
    }
    throw new IllegalArgumentException(
        "The provided map does not contain a string for attribute '" + attribute + "'"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Retrieves an <code>String[]</code> attribute from map.
   * 
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param countAttribute the attribute that contains the number of elements
   * @param arrayAttribute the attribute name where the values are stored. The index starting from
   *          offset is appended to this
   * @param offset the starting index for arrayAttribute
   * @return the <code>String[]</code>
   * @throws IllegalArgumentException if any of the attribute does not exist or is not a number
   */
  public static String[] getStringArray(Map<String, String> map, String countAttribute,
      String arrayAttribute, int offset) throws IllegalArgumentException {
    int count = getInt(map, countAttribute);
    String[] result = new String[count];
    for (int i = 0; i < count; i++) {
      result[i] = getString(map, getAttributeName(arrayAttribute, i + offset));
    }
    return result;
  }

  /**
   * Converts an input handle back to the corresponding element.
   * 
   * @param owner the working copy owner
   * @param project the project, or <code>null</code> for the workspace
   * @param handle the input handle
   * @param check <code>true</code> to check for existence of the element, <code>false</code>
   *          otherwise
   * @return the corresponding Dart element, or <code>null</code> if no such element exists
   */
  public static DartElement handleToElement(final WorkingCopyOwner owner, final String project,
      final String handle, final boolean check) {
    DartElement element = null;
    if (owner != null) {
      element = DartCore.create(handle, owner);
    } else {
      element = DartCore.create(handle);
    }
    if (element == null && project != null) {
      final DartProject dartProject = DartCore.create(ResourcesPlugin.getWorkspace().getRoot()).getDartProject(
          project);
      final String identifier = dartProject.getHandleIdentifier();
      if (owner != null) {
        element = DartCore.create(identifier + handle, owner);
      } else {
        element = DartCore.create(identifier + handle);
      }
    }
    if (check && element instanceof Method) {
      /*
       * Resolve the method based on simple names of parameter types (to accommodate for different
       * qualifications when refactoring is e.g. recorded in source but applied on binary method):
       */
      final Method method = (Method) element;
      final Method[] methods = method.getDeclaringType().findMethods(method);
      if (methods != null && methods.length > 0) {
        element = methods[0];
      }
    }
    if (element != null && (!check || element.exists())) {
      return element;
    }
    return null;
  }

  /**
   * Converts an input handle with the given prefix back to the corresponding resource. WARNING:
   * this method resolves the handle in the current workspace, since the type of the resource
   * (file/folder) cannot be determined from the handle alone (path never has a trailing separator).
   * 
   * @param project the project, or <code>null</code> for the workspace
   * @param handle the input handle
   * @return the corresponding resource, or <code>null</code> if no such resource exists
   */
  public static IResource handleToResource(final String project, final String handle) {
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    if ("".equals(handle)) {
      return null;
    }
    final IPath path = Path.fromPortableString(handle);
    if (path == null) {
      return null;
    }
    if (project != null && !project.isEmpty()) {
      return root.getProject(project).findMember(path);
    }
    return root.findMember(path);
  }

  /**
   * Converts an input handle with the given prefix back to the corresponding resource path.
   * 
   * @param project the project, or <code>null</code> for the workspace
   * @param handle the input handle
   * @return the corresponding resource path. Note: if the given handle is absolute, the project is
   *         not used to resolve.
   */
  public static IPath handleToResourcePath(final String project, final String handle) {
    final IPath path = Path.fromPortableString(handle);
    if (project != null && !project.isEmpty() && !path.isAbsolute()) {
      return new Path(project).append(path).makeAbsolute();
    }
    return path;
  }

  /**
   * @param map the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key in the map
   * @return <code>true</code> iff the map contains a boolean attribute for the key
   * @throws IllegalArgumentException if the attribute exists but is not a boolean
   */
  public static boolean hasBoolean(Map<String, String> map, String attribute)
      throws IllegalArgumentException {
    String string = getString(map, attribute, true);
    if (string == null) {
      return false;
    }

    //Boolean.valueOf(value) does not complain about wrong values
    String value = string.toLowerCase();
    if (LOWER_CASE_TRUE.equals(value)) {
      return true;
    }

    if (LOWER_CASE_FALSE.equals(value)) {
      return true;
    }
    throw new IllegalArgumentException(
        "The attribute '" + attribute + "' does not contain a valid boolean: '" + value + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }

  /**
   * Converts the specified resource to an input handle.
   * 
   * @param project the project, or <code>null</code> for the workspace
   * @param resourcePath the resource
   * @return the input handle Note: if the given project is not the resource's project, then the
   *         full handle is returned
   */
  public static String resourcePathToHandle(final String project, final IPath resourcePath) {
    if (project != null && !project.isEmpty() && resourcePath.segmentCount() != 1) {
      if (resourcePath.segment(0).equals(project)) {
        return resourcePath.removeFirstSegments(1).toPortableString();
      }
    }
    return resourcePath.toPortableString();
  }

  /**
   * Inserts the <code>boolean</code> into the map.
   * 
   * @param arguments the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key's name for the map
   * @param value the <code>boolean</code> to store
   * @throws IllegalArgumentException if the attribute name is invalid
   */
  public static void setBoolean(Map<String, String> arguments, String attribute, boolean value)
      throws IllegalArgumentException {
    setString(arguments, attribute, value ? LOWER_CASE_TRUE : LOWER_CASE_FALSE);
  }

  /**
   * Inserts the booleans into the map.
   * 
   * @param arguments arguments the map with <code>&lt;String, String&gt;</code> mapping
   * @param countAttribute the attribute where the number of resources will be stored. Can be
   *          <code>null</code> if no count attribute should be created
   * @param arrayAttribute the attribute where the resources will be stored
   * @param value the booleans to store
   * @param offset the offset to start at
   * @throws IllegalArgumentException if the attribute name is invalid or any of the booleans are
   *           null
   */
  public static void setBooleanArray(Map<String, String> arguments, String countAttribute,
      String arrayAttribute, boolean[] value, int offset) {
    if (value == null) {
      throw new IllegalArgumentException(
          "The values for arrayAttribute '" + arrayAttribute + "' may not be null"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (countAttribute != null) {
      setInt(arguments, countAttribute, value.length);
    }
    for (int i = 0; i < value.length; i++) {
      setBoolean(arguments, getAttributeName(arrayAttribute, i + offset), value[i]);
    }
  }

  /**
   * Inserts the <code>{@link DartElement}</code> into the map.
   * 
   * @param arguments the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key's name for the map
   * @param project the project of the element or <code>null</code>. Note: if the given project is
   *          not the <code>element</code>'s project, then the full handle is stored
   * @param element the element to store
   * @throws IllegalArgumentException if the attribute name is invalid, or the element is
   *           <code>null</code>
   */
  public static void setDartElement(Map<String, String> arguments, String attribute,
      String project, DartElement element) throws IllegalArgumentException {
    if (element == null) {
      throw new IllegalArgumentException(
          "The element for attribute '" + attribute + "' may not be null"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    setString(arguments, attribute, elementToHandle(project, element));
  }

  /**
   * Inserts the resources into the map.
   * 
   * @param arguments arguments the map with <code>&lt;String, String&gt;</code> mapping
   * @param countAttribute the attribute where the number of elements will be stored. Can be
   *          <code>null</code> if no count attribute should be created
   * @param arrayAttributePrefix the name prefix of the attributes where the elements will be stored
   * @param project the project of the elements or <code>null</code>
   * @param elements the elements to store
   * @param offset the offset to start at (usually 1)
   * @throws IllegalArgumentException if the attribute name is invalid or any of the elements are
   *           null
   */
  public static void setDartElementArray(Map<String, String> arguments, String countAttribute,
      String arrayAttributePrefix, String project, DartElement[] elements, int offset)
      throws IllegalArgumentException {
    if (elements == null) {
      throw new IllegalArgumentException(
          "The elements for arrayAttribute '" + arrayAttributePrefix + "' may not be null"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (countAttribute != null) {
      setInt(arguments, countAttribute, elements.length);
    }
    for (int i = 0; i < elements.length; i++) {
      DartElement element = elements[i];
      setDartElement(arguments, getAttributeName(arrayAttributePrefix, offset + i), project,
          element);
    }
  }

  /**
   * Inserts the <code>int</code> into the map.
   * 
   * @param arguments the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key's name for the map
   * @param value the <code>int</code> to store
   * @throws IllegalArgumentException if the attribute name is invalid
   */
  public static void setInt(Map<String, String> arguments, String attribute, int value)
      throws IllegalArgumentException {
    setString(arguments, attribute, Integer.toString(value));
  }

  /**
   * Inserts the <code>{@link IPath}</code> into the map.
   * 
   * @param arguments the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key's name for the map
   * @param project the project of the element or <code>null</code>. Note: if the given project is
   *          not the resource's project, then the full handle is stored
   * @param resourcePath the resource to store
   * @throws IllegalArgumentException if the attribute name is invalid, or the resource is
   *           <code>null</code>
   */
  public static void setResourcePath(Map<String, String> arguments, String attribute,
      String project, IPath resourcePath) throws IllegalArgumentException {
    if (resourcePath == null) {
      throw new IllegalArgumentException(
          "The resource for attribute '" + attribute + "' may not be null"); //$NON-NLS-1$//$NON-NLS-2$
    }
    setString(arguments, attribute, resourcePathToHandle(project, resourcePath));
  }

  /**
   * Inserts the resources into the map.
   * 
   * @param arguments arguments the map with <code>&lt;String, String&gt;</code> mapping
   * @param countAttribute the attribute where the number of resources will be stored. Can be
   *          <code>null</code> if no count attribute should be created
   * @param arrayAttribute the attribute where the resources will be stored
   * @param project the project of the resources or <code>null</code>
   * @param resourcePaths the resource paths to store
   * @param offset the offset to start at
   * @throws IllegalArgumentException if the attribute name is invalid or any of the resources are
   *           null
   */
  public static void setResourcePathArray(Map<String, String> arguments, String countAttribute,
      String arrayAttribute, String project, IPath[] resourcePaths, int offset)
      throws IllegalArgumentException {
    if (resourcePaths == null) {
      throw new IllegalArgumentException(
          "The resources for arrayAttribute '" + arrayAttribute + "' may not be null"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (countAttribute != null) {
      setInt(arguments, countAttribute, resourcePaths.length);
    }
    for (int i = 0; i < resourcePaths.length; i++) {
      IPath resourcePath = resourcePaths[i];
      setResourcePath(arguments, getAttributeName(arrayAttribute, offset + i), project,
          resourcePath);
    }
  }

  /**
   * Inserts the selection into the map.
   * 
   * @param arguments the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key's name for the map
   * @param offset the offset of the selection
   * @param length the length of the selection
   * @throws IllegalArgumentException if the attribute name is invalid
   */
  public static void setSelection(Map<String, String> arguments, String attribute, int offset,
      int length) throws IllegalArgumentException {
    String value = Integer.toString(offset) + " " + Integer.toString(length); //$NON-NLS-1$
    setString(arguments, attribute, value);
  }

  /**
   * Inserts the <code>{@link String}</code> into the map.
   * 
   * @param arguments the map with <code>&lt;String, String&gt;</code> mapping
   * @param attribute the key's name for the map
   * @param value the <code>{@link String}</code> to store. If <code>null</code> no insertion is
   *          performed
   * @throws IllegalArgumentException if the attribute name is invalid
   */
  public static void setString(Map<String, String> arguments, String attribute, String value)
      throws IllegalArgumentException {
    if (attribute == null || "".equals(attribute) || attribute.indexOf(' ') != -1) {
      throw new IllegalArgumentException(
          "Attribute '" + attribute + "' is not valid: '" + value + "'"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
    }
    if (value != null) {
      arguments.put(attribute, value);
    } else {
      arguments.remove(attribute);
    }
  }

  /**
   * Inserts the Strings into the map.
   * 
   * @param arguments arguments the map with <code>&lt;String, String&gt;</code> mapping
   * @param countAttribute the attribute where the number of resources will be stored. Can be
   *          <code>null</code> if no count attribute should be created
   * @param arrayAttribute the attribute where the resources will be stored
   * @param value the strings to store
   * @param offset the offset to start at
   * @throws IllegalArgumentException if the attribute name is invalid or any of the strings are
   *           null
   */
  public static void setStringArray(Map<String, String> arguments, String countAttribute,
      String arrayAttribute, String[] value, int offset) {
    if (value == null) {
      throw new IllegalArgumentException(
          "The values for arrayAttribute '" + arrayAttribute + "' may not be null"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (countAttribute != null) {
      setInt(arguments, countAttribute, value.length);
    }
    for (int i = 0; i < value.length; i++) {
      String string = value[i];
      setString(arguments, getAttributeName(arrayAttribute, i + offset), string);
    }
  }

}
