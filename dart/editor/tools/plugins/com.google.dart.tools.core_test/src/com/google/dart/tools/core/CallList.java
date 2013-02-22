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
package com.google.dart.tools.core;

import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * Used by tests to track calls made to mocked objects.
 */
public class CallList {

  public static class Call {
    protected final Object target;
    protected final String methodName;
    protected final Object[] args;

    public Call(Object target, String methodName, Object... args) {
      this.target = target;
      this.methodName = methodName;
      this.args = args;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Call) {
        Call call = (Call) obj;
        return equalTarget(call.target) && equalMethodName(call.methodName)
            && equalArguments(call.args);
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      PrintStringWriter writer = new PrintStringWriter();
      writer.print("Call[");
      print(writer, "");
      writer.print("]");
      return writer.toString();
    }

    protected boolean equalArgument(int index, Object arg1, Object arg2) {
      if (arg1 == arg2) {
        return true;
      }
      if (arg1 == null) {
        return arg2 == null;
      }
      return arg1.equals(arg2);
    }

    protected boolean equalArguments(Object[] otherArgs) {
      if (args.length != otherArgs.length) {
        return false;
      }
      for (int index = 0; index < args.length; index++) {
        if (!equalArgument(index, args[index], otherArgs[index])) {
          return false;
        }
      }
      return true;
    }

    protected boolean equalMethodName(String otherMethodName) {
      if (methodName == null) {
        return otherMethodName == null;
      }
      return methodName.equals(otherMethodName);
    }

    protected boolean equalTarget(Object otherTarget) {
      if (target == otherTarget) {
        return true;
      }
      if (target == null) {
        return otherTarget == null;
      }
      return target.equals(otherTarget);
    }

    protected void print(PrintStringWriter writer, String indent) {
      printTarget(writer, indent, target);
      printMethodName(writer, indent + "    ");
      printArguments(writer, indent + "    ", args);
    }

    protected void printArgument(PrintStringWriter writer, String indent, Object arg) {
      writer.print(indent);
      writer.println(arg != null ? arg.toString() : "null");
    }

    protected void printArguments(PrintStringWriter writer, String indent, Object[] args) {
      for (Object arg : args) {
        printArgument(writer, indent, arg);
      }
    }

    protected void printMethodName(PrintStringWriter writer, String indent) {
      writer.print(indent);
      writer.println(methodName);
    }

    protected void printTarget(PrintStringWriter writer, String indent, Object target) {
      writer.print(indent);
      writer.println(target != null ? target.toString() : "null");
    }
  }

  private final ArrayList<Call> calls = new ArrayList<Call>();

  /**
   * Add a call to the list of calls
   * 
   * @param call the call that was added (not {@code null})
   */
  public void add(Call call) {
    if (call == null) {
      throw new IllegalArgumentException();
    }
    calls.add(call);
  }

  /**
   * Add a call to the list of calls
   * 
   * @param target the call target (not {@code null})
   * @param methodName the method that was called on the target (not {@code null})
   * @param args zero or more arguments for the call
   */
  public void add(Object target, String methodName, Object... args) {
    add(new Call(target, methodName, args));
  }

  /**
   * Assert that the list contains the specified call, and remove that call from the list.
   * 
   * @param call the call (not {@code null})
   */
  public void assertCall(Call call) {
    if (!calls.remove(call)) {
      fail(call);
    }
  }

  /**
   * Assert that the list contains the specified call, and remove that call from the list.
   * 
   * @param target the call target (not {@code null})
   * @param methodName the method name (not {@code null})
   * @param args zero or more call arguments
   */
  public void assertCall(Object target, String methodName, Object... args) {
    assertCall(new Call(target, methodName, args));
  }

  /**
   * Assert that the list contains the specified call or not depending upon whether it was expected.
   * 
   * @param expected {@code true} if the call is expected, else {@code false}
   * @param call the call (not {@code null})
   */
  public void assertExpectedCall(boolean expected, Call call) {
    if (expected) {
      assertCall(call);
    } else {
      assertNoCall(call);
    }
  }

  /**
   * Assert that the list contains the specified call or not depending upon whether it was expected.
   * 
   * @param expected {@code true} if the call is expected, else {@code false}
   * @param target the call target (not {@code null})
   * @param methodName the method name (not {@code null})
   * @param args zero or more call arguments
   */
  public void assertExpectedCall(boolean expected, Object target, String methodName, Object... args) {
    assertExpectedCall(expected, new Call(target, methodName, args));
  }

  /**
   * Assert that the list does NOT contain the specified call.
   * 
   * @param call the call (not {@code null})
   */
  public void assertNoCall(Call call) {
    if (calls.contains(call)) {
      PrintStringWriter writer = new PrintStringWriter();
      writer.println("Did NOT expect:");
      call.print(writer, "  ");
      TestCase.fail(writer.toString().trim());
    }
  }

  /**
   * Assert that the list does NOT contain the specified call.
   * 
   * @param target the call target (not {@code null})
   * @param methodName the method name (not {@code null})
   * @param args zero or more call arguments
   */
  public void assertNoCall(Object target, String methodName, Object... args) {
    assertNoCall(new Call(target, methodName, args));
  }

  /**
   * Assert that there are no more calls in the list
   */
  public void assertNoCalls() {
    if (calls.size() > 0) {
      fail("No more calls");
    }
  }

  private void fail(Object expected) {
    PrintStringWriter writer = new PrintStringWriter();
    writer.println("Expected:");
    if (expected instanceof Call) {
      ((Call) expected).print(writer, "    ");
    } else {
      writer.println(expected != null ? expected.toString() : "null");
    }
    writer.println("Actual:");
    for (Call call : calls) {
      call.print(writer, "    ");
    }
    TestCase.fail(writer.toString().trim());
  }
}
