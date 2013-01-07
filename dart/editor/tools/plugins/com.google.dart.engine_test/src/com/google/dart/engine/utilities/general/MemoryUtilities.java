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
package com.google.dart.engine.utilities.general;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;

/**
 * The class {@code MemoryUtilities} defines utility methods related to memory usage.
 */
public final class MemoryUtilities {
  /**
   * An array containing the amount by which a value must be incremented in order to round it up to
   * the nearest multiple of four when the index into the array is the remainder after dividing the
   * value by four.
   */
  private static final long[] ROUND_UP_AMOUNT = {0L, 3L, 2L, 1L};

  /**
   * Return the approximate size in bytes of the object graph containing the given object and all
   * objects reachable from it. The actual size of an individual object depends on the
   * implementation of the virtual machine running on the current platform and cannot be computed
   * accurately. However, the value returned is close enough to allow the sizes of two different
   * object structures to be compared with a reasonable degree of confidence.
   * <p>
   * Note that this method cannot be used to find the size of primitive values. Primitive values
   * will be automatically boxed and this method will return the size of the boxed primitive, not
   * the size of the primitive itself.
   * 
   * @param object the object at the root of the graph whose size is to be returned
   * @return the approximate size of the object graph containing the given object
   */
  public static long sizeOfGraph(Object object) {
    long size = 0L;
    HashSet<Object> visitedObjects = new HashSet<Object>();
    HashSet<Object> objectsToVisit = new HashSet<Object>();
    objectsToVisit.add(object);
    while (!objectsToVisit.isEmpty()) {
      Object nextObject = objectsToVisit.iterator().next();
      objectsToVisit.remove(nextObject);
      if (visitedObjects.add(nextObject)) {
        size += addObjectToGraph(nextObject, visitedObjects, objectsToVisit);
      }
    }
    return size;
  }

  /**
   * Compute the size of the given object and add any objects referenced by it to the set of objects
   * needing to be visited.
   * 
   * @param object the object to be added to the object graph
   * @param visitedObjects the objects that are already part of the graph
   * @param objectsToVisit the objects to be added to the object graph
   * @return the size of the given object
   */
  private static long addObjectToGraph(Object object, HashSet<Object> visitedObjects,
      HashSet<Object> objectsToVisit) {
    Class<?> objectClass = object.getClass();
    if (objectClass.isArray()) {
      Class<?> componentType = objectClass.getComponentType();
      int length = Array.getLength(object);
      if (!componentType.isPrimitive()) {
        for (int i = 0; i < length; i++) {
          Object elementValue = Array.get(object, i);
          if (elementValue != null && !visitedObjects.contains(elementValue)) {
            objectsToVisit.add(elementValue);
          }
        }
      }
      return 12L + roundToWord(length * getElementSize(componentType));
    } else {
      long size = 8L;
      while (objectClass != null) {
        Field[] fields = objectClass.getDeclaredFields();
        for (Field field : fields) {
          if (!Modifier.isStatic(field.getModifiers())) {
            Class<?> fieldType = field.getType();
            if (fieldType == long.class || fieldType == double.class) {
              size += 8L;
            } else {
              size += 4L;
            }
            if (!fieldType.isPrimitive() && !field.isEnumConstant()) {
              try {
                field.setAccessible(true);
                Object fieldValue = field.get(object);
                if (fieldValue != null && !visitedObjects.contains(fieldValue)) {
                  objectsToVisit.add(fieldValue);
                }
              } catch (Exception exception) {
                // Ignored.
              }
            }
          }
        }
        objectClass = objectClass.getSuperclass();
      }
      return size;
    }
  }

  /**
   * Return the approximate number of bytes required to store a value of the given type in an array.
   * 
   * @param componentType the type of the value to be stored
   * @return the approximate number of bytes required to store a value of the given type in an array
   */
  private static long getElementSize(Class<?> componentType) {
    if (componentType.isPrimitive()) {
      if (componentType == boolean.class) {
        return 1L;
      } else if (componentType == byte.class) {
        return 1L;
      } else if (componentType == char.class) {
        return 2L;
      } else if (componentType == short.class) {
        return 2L;
      } else if (componentType == int.class) {
        return 4L;
      } else if (componentType == long.class) {
        return 8L;
      } else if (componentType == float.class) {
        return 4L;
      } else if (componentType == double.class) {
        return 8L;
      }
    }
    return 4L;
  }

  /**
   * Return the smallest multiple of four (4) that is greater than or equal to the given value.
   * 
   * @param value the value to be rounded up to the nearest multiple of four
   * @return the smallest multiple of four that is greater than or equal to the value
   */
  private static long roundToWord(long value) {
    return value + ROUND_UP_AMOUNT[(int) (value % 4)];
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private MemoryUtilities() {
    super();
  }
}
