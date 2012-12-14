package com.google.dart.tools.core;

import com.google.dart.tools.core.test.util.DartCoreTestLog;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * Common {@link TestCase} superclass that asserts no unexpected Eclipse log entries and provides a
 * setup method ({@link #setUpOnce()}) that is called once before all tests in the class are
 * executed and a teardown method ({@link #tearDownOnce()}) that is called once after all tests in
 * the class have been executed.
 */
public abstract class AbstractDartCoreTest extends TestCase {

  /**
   * The test log
   */
  protected static final DartCoreTestLog LOG = DartCoreTestLog.getLog();

  /**
   * Used to determine when {@link #setUpOnce()} and {@link #tearDownOnce()} should be called.
   */
  private static final HashMap<Class<?>, Integer> TEST_COUNTS = new HashMap<Class<?>, Integer>();

  /**
   * Called once prior to the receiver's first test.
   */
  public static void setUpOnce() throws Exception {
  }

  /**
   * Called once after the receiver's last test.
   */
  public static void tearDownOnce() throws Exception {
  }

  /**
   * Answer the number of test methods in the specified class
   * 
   * @param testClass the test class (not <code>null</code>)
   * @return the number of test methods
   */
  private static int countTestMethods(Class<?> testClass) {
    int count = 0;
    for (Method method : testClass.getDeclaredMethods()) {
      if (isTestMethod(method)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Answer <code>true</code> if the specified method has name that starts with "test", is public
   * but not static, and has no arguments.
   */
  private static boolean isTestMethod(Method method) {
    if (!method.getName().startsWith("test")) {
      return false;
    }
    int modifiers = method.getModifiers();
    if ((modifiers & Modifier.PUBLIC) == 0 || (modifiers & Modifier.STATIC) != 0) {
      return false;
    }
    if (method.getParameterTypes().length > 0) {
      return false;
    }
    return true;
  }

  public AbstractDartCoreTest() {
    super();
  }

  public AbstractDartCoreTest(String name) {
    super(name);
  }

  /**
   * Extend superclass implementation to set up and tear down the DartCore test log and invoke
   * {@link #setUpOnce()} and {@link #tearDownOnce()} if defined.
   */
  @Override
  public void runBare() throws Throwable {
    Throwable exception = null;
    if (!TEST_COUNTS.containsKey(getClass())) {
      TEST_COUNTS.put(getClass(), countTestMethods(getClass()));
      invokeStaticMethodIfExists(getClass(), "setUpOnce");
    }
    LOG.setUp();
    try {
      super.runBare();
    } catch (Throwable running) {
      exception = running;
    } finally {
      try {
        LOG.tearDown();
        int count = TEST_COUNTS.get(getClass()) - 1;
        TEST_COUNTS.put(getClass(), count);
        if (count == 0) {
          invokeStaticMethodIfExists(getClass(), "tearDownOnce");
        }
      } catch (Throwable tearingDown) {
        if (exception == null) {
          exception = tearingDown;
        }
      }
    }
    if (exception != null) {
      throw exception;
    }
  }

  /**
   * Extend superclass implementation to assert no unexpected DartCore log entries
   */
  @Override
  protected void runTest() throws Throwable {
    super.runTest();
    LOG.assertEmpty();
  }

  /**
   * Execute the specified method if it exists
   * 
   * @param testClass the class containing the static method to be executed (not <code>null</code>)
   * @param methodName the name of the static method (not <code>null</code>)
   */
  private void invokeStaticMethodIfExists(Class<?> testClass, String methodName) throws Exception {
    Method method;
    try {
      method = testClass.getDeclaredMethod(methodName, new Class<?>[] {});
    } catch (NoSuchMethodException e) {
      return;
    }
    if ((method.getModifiers() & Modifier.PUBLIC) == 0
        || (method.getModifiers() & Modifier.STATIC) == 0) {
      throw new IllegalAccessException("Expected " + testClass.getSimpleName() + "#" + methodName
          + " to be a public static method");
    }
    method.invoke(null);
  }
}
