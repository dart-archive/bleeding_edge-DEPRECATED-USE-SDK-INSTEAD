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
package com.google.dart.engine.utilities.collection;

import java.util.EmptyStackException;

/**
 * Instances of the class <code>IntStack</code> implement a stack whole elements are
 * <code>int</code> values rather than objects.
 * 
 * @coverage dart.engine.utilities
 */
public class IntStack {
  /**
   * An array holding the elements of the stack.
   */
  private int[] values;

  /**
   * The index of the first element past the top of the stack.
   */
  private int top;

  /**
   * The initial capacity that is used when the client does not specify an initial capacity.
   */
  private static final int DEFAULT_INITIAL_CAPACITY = 10;

  /**
   * Initialize a newly created stack to be empty.
   */
  public IntStack() {
    this(DEFAULT_INITIAL_CAPACITY);
  }

  /**
   * Initialize a newly created stack to be empty but to be optimized to hold up to the given number
   * of elements.
   * 
   * @param initialCapacity the initial capacity of the stack
   */
  public IntStack(int initialCapacity) {
    values = new int[initialCapacity];
    top = 0;
  }

  /**
   * Remove all of the elements from this stack, leaving it empty.
   */
  public void clear() {
    top = 0;
  }

  /**
   * Increment the top value by the specified amount.
   * 
   * @param value the amount by which the top value is to be incremented
   */
  public void increment(int value) {
    if (top == 0) {
      throw new EmptyStackException();
    }
    values[top - 1] += value;
  }

  /**
   * Return {@code true} if this stack is empty.
   * 
   * @return {@code true} if this stack is empty
   */
  public boolean isEmpty() {
    return top == 0;
  }

  /**
   * Return the top value on the stack without removing that value from the stack.
   * 
   * @return the top value on the stack
   */
  public int peek() {
    if (top == 0) {
      throw new EmptyStackException();
    }
    return values[top - 1];
  }

  /**
   * Pop the top value from the stack and return it.
   * 
   * @return the value that was removed from the stack
   */
  public int pop() {
    if (top == 0) {
      throw new EmptyStackException();
    }
    return values[--top];
  }

  /**
   * Push the given value onto the top of the stack.
   * 
   * @param value the value to be pushed onto the top of the stack
   */
  public void push(int value) {
    int currentSize;
    int[] newValues;

    currentSize = values.length;
    if (top == currentSize) {
      newValues = new int[currentSize + 10];
      System.arraycopy(values, 0, newValues, 0, currentSize);
      values = newValues;
    }
    values[top++] = value;
  }

  /**
   * Replace the top value on the stack with the given value. This is equivalent to
   * 
   * <pre>
   *   stack.pop();
   *   stack.push(value);
   * </pre>
   * 
   * @param value the value that will replace the value on the top of the stack
   */
  public void replaceTop(int value) {
    if (top == 0) {
      throw new EmptyStackException();
    }
    values[top - 1] = value;
  }

  /**
   * Return the number of elements that are currently on this stack.
   * 
   * @return the number of elements that are currently on this stack
   */
  public int size() {
    return top;
  }

  /**
   * Return a textual representation of the stack.
   * 
   * @return a textual representation of the stack
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('[');
    for (int i = 0; i < top; i++) {
      if (i > 0) {
        builder.append(", ");
      }
      builder.append(values[i]);
    }
    builder.append(']');
    return builder.toString();
  }
}
