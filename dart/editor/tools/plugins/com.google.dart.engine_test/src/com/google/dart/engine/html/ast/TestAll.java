package com.google.dart.engine.html.ast;

import com.google.dart.engine.ExtendedTestSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAll {
  public static Test suite() {
    TestSuite suite = new ExtendedTestSuite("Tests in " + TestAll.class.getPackage().getName());
    suite.addTest(com.google.dart.engine.html.ast.visitor.TestAll.suite());
    return suite;
  }
}
