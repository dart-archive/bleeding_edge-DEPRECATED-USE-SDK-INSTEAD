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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * The class {@code MemoryUtilities} defines utility methods related to memory usage.
 */
public final class MemoryUtilities {
  /**
   * Instances of the class {@code MemoryUsage} represent information about the amount of memory
   * used by the objects in an object graph.
   */
  public static class MemoryUsage {
    /**
     * The number of bytes in a kilobyte.
     */
    private static final long K = 1024L;

    /**
     * The number of bytes in a megabyte.
     */
    private static final long M = K * K;

    /**
     * The total amount of memory being used.
     */
    private long totalSize = 0L;

    /**
     * The number of objects whose size is included in the total.
     */
    private long totalCount = 0L;

    /**
     * A table mapping classes to information about the amount of space used by instances of that
     * class.
     */
    private HashMap<Class<?>, Accumulator> classUsageMap = new HashMap<Class<?>, Accumulator>();

    /**
     * Initialize a newly create object to record no memory used by zero objects.
     */
    public MemoryUsage() {
      super();
    }

    /**
     * Record that an instance of the given class uses the given number of bytes of memory.
     * 
     * @param objectClass the class of object using the memory
     * @param size the amount of memory used by the object
     */
    public void addMemory(Class<?> objectClass, long size) {
      totalSize += size;
      totalCount++;
      Accumulator accumulator = classUsageMap.get(objectClass);
      if (accumulator == null) {
        accumulator = new Accumulator();
        classUsageMap.put(objectClass, accumulator);
      }
      accumulator.addMemory(size);
    }

    /**
     * Add the memory used by objects in the graph represented by the given memory usage data to the
     * totals maintained by this object.
     * 
     * @param usageData the usage data to be added to the usage data maintained by this object
     */
    public void addMemory(MemoryUsage usageData) {
      totalSize += usageData.totalSize;
      totalCount += usageData.totalCount;
      for (Map.Entry<Class<?>, Accumulator> entry : usageData.classUsageMap.entrySet()) {
        Class<?> usedClass = entry.getKey();
        Accumulator accumulator = classUsageMap.get(usedClass);
        if (accumulator == null) {
          classUsageMap.put(usedClass, entry.getValue());
        } else {
          accumulator.addMemory(entry.getValue());
        }
      }
    }

    /**
     * Write a summary of the memory used by the objects in the object graph to the given writer.
     * 
     * @param writer the writer to which the summary is to be written
     * @return
     */
    public void writeSummary(PrintWriter writer) {
      printSize(writer, "Total memory: ", totalSize);
      writer.println();
      writer.print("Total count:  ");
      writer.println(totalCount);
      writer.println();
      Class<?>[] usedClasses = classUsageMap.keySet().toArray(new Class<?>[classUsageMap.size()]);
      Arrays.sort(usedClasses, new Comparator<Class<?>>() {
        @Override
        public int compare(Class<?> firstClass, Class<?> secondClass) {
          return firstClass.getName().compareTo(secondClass.getName());
        }
      });
      for (Class<?> usedClass : usedClasses) {
        Accumulator accumulator = classUsageMap.get(usedClass);
        printSize(writer, "  ", accumulator.totalSize);
        writer.print(" [");
        writer.print(accumulator.totalCount);
        writer.print("] ");
        writer.println(usedClass.getName());
      }
    }

    /**
     * Write a number representing memory size to the given writer.
     * 
     * @param writer the writer to which the size is to be written
     * @param label the label to be written before the size is written
     * @param size the number of bytes to be written
     */
    private void printSize(PrintWriter writer, String label, long size) {
      writer.print(label);
      if (size < K) {
        writer.print(size);
        writer.print(" bytes");
      } else if (size < M) {
        writer.print(size / K);
        writer.print(" K (");
        writer.print(size);
        writer.print(" bytes)");
      } else {
        writer.print(size / M);
        writer.print(" M (");
        writer.print(size);
        writer.print(" bytes)");
      }
    }
  }

  /**
   * Instances of the class {@code Accumulator} represent information about the amount of memory
   * used by instances of a specific class.
   */
  private static class Accumulator {
    /**
     * The total amount of memory being used.
     */
    private long totalSize = 0L;

    /**
     * The number of objects whose size is included in the total.
     */
    private long totalCount = 0L;

    /**
     * Initialize a newly created accumulator to be zero.
     */
    public Accumulator() {
      super();
    }

    /**
     * Add the memory usage represented by the given accumulator to this accumulator.
     * 
     * @param accumulator the accumulator whose usage is to be added
     */
    public void addMemory(Accumulator accumulator) {
      totalSize += accumulator.totalSize;
      totalCount += accumulator.totalCount;
    }

    /**
     * Record that the given amount of memory is being used by a single instance of a class.
     * 
     * @param size the amount of memory being used
     */
    public void addMemory(long size) {
      totalSize += size;
      totalCount++;
    }
  }

  /**
   * An array containing the amount by which a value must be incremented in order to round it up to
   * the nearest multiple of four when the index into the array is the remainder after dividing the
   * value by four.
   */
  private static final long[] ROUND_UP_AMOUNT = {0L, 3L, 2L, 1L};

  /**
   * Return an object representing the approximate size in bytes of the object graph containing the
   * given object and all objects reachable from it. The actual size of an individual object depends
   * on the implementation of the virtual machine running on the current platform and cannot be
   * computed accurately. However, the value returned is close enough to allow the sizes of two
   * different object structures to be compared with a reasonable degree of confidence.
   * <p>
   * Note that this method cannot be used to find the size of primitive values. Primitive values
   * will be automatically boxed and this method will return the size of the boxed primitive, not
   * the size of the primitive itself.
   * 
   * @param object the object at the root of the graph whose size is to be returned
   * @return the approximate size of the object graph containing the given object
   */
  public static MemoryUsage measureMemoryUsage(Object object) {
    return measureMemoryUsage(object, Predicates.alwaysTrue());
  }

  /**
   * Return an object representing the approximate size in bytes of the object graph containing the
   * given object and all objects reachable from it. The actual size of an individual object depends
   * on the implementation of the virtual machine running on the current platform and cannot be
   * computed accurately. However, the value returned is close enough to allow the sizes of two
   * different object structures to be compared with a reasonable degree of confidence.
   * <p>
   * Note that this method cannot be used to find the size of primitive values. Primitive values
   * will be automatically boxed and this method will return the size of the boxed primitive, not
   * the size of the primitive itself.
   * 
   * @param object the object at the root of the graph whose size is to be returned
   * @param isIncluded a predicate that returns {@code true} if the argument is an object that is
   *          part of the object graph whose size is being computed
   * @return the approximate size of the object graph containing the given object
   */
  public static MemoryUsage measureMemoryUsage(Object object, Predicate<Object> isIncluded) {
    MemoryUsage usageData = new MemoryUsage();
    HashSet<Object> visitedObjects = new HashSet<Object>();
    HashSet<Object> objectsToVisit = new HashSet<Object>();
    objectsToVisit.add(object);
    while (!objectsToVisit.isEmpty()) {
      Object nextObject = objectsToVisit.iterator().next();
      objectsToVisit.remove(nextObject);
      if (isIncluded.apply(nextObject) && visitedObjects.add(nextObject)) {
        addObjectToGraph(nextObject, usageData, visitedObjects, objectsToVisit);
      }
    }
    return usageData;
  }

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
    return sizeOfGraph(object, Predicates.alwaysTrue());
  }

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
   * @param isIncluded a predicate that returns {@code true} if the argument is an object that is
   *          part of the object graph whose size is being computed
   * @return the approximate size of the object graph containing the given object
   */
  public static long sizeOfGraph(Object object, Predicate<Object> isIncluded) {
    long size = 0L;
    HashSet<Object> visitedObjects = new HashSet<Object>();
    HashSet<Object> objectsToVisit = new HashSet<Object>();
    objectsToVisit.add(object);
    while (!objectsToVisit.isEmpty()) {
      Object nextObject = objectsToVisit.iterator().next();
      objectsToVisit.remove(nextObject);
      if (isIncluded.apply(nextObject) && visitedObjects.add(nextObject)) {
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
   * Compute the size of the given object and add any objects referenced by it to the set of objects
   * needing to be visited.
   * 
   * @param object the object to be added to the object graph
   * @param usageData the usage data for the object graph containing the object
   * @param visitedObjects the objects that are already part of the graph
   * @param objectsToVisit the objects to be added to the object graph
   * @return the size of the given object
   */
  private static void addObjectToGraph(Object object, MemoryUsage usageData,
      HashSet<Object> visitedObjects, HashSet<Object> objectsToVisit) {
    Class<?> objectClass = object.getClass();
    if (objectClass.isArray()) {
      Class<?> componentType = objectClass.getComponentType();
      int length = Array.getLength(object);
      if (!componentType.isPrimitive()) {
        for (int i = 0; i < length; i++) {
          Object elementValue = Array.get(object, i);
          if (elementValue != null && !visitedObjects.contains(elementValue)
              && !(elementValue instanceof Class)) {
            objectsToVisit.add(elementValue);
          }
        }
      }
      usageData.addMemory(objectClass, 12L + roundToWord(length * getElementSize(componentType)));
    } else {
      long size = 8L;
      Class<?> currentClass = objectClass;
      while (currentClass != null) {
        Field[] fields = currentClass.getDeclaredFields();
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
                if (fieldValue != null && !visitedObjects.contains(fieldValue)
                    && !(fieldValue instanceof Class)) {
                  objectsToVisit.add(fieldValue);
                }
              } catch (Exception exception) {
                // Ignored.
              }
            }
          }
        }
        currentClass = currentClass.getSuperclass();
      }
      usageData.addMemory(objectClass, size);
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
