package com.google.dart.tools.ui.internal.text.dart;

import junit.framework.TestCase;

public class DartServerProposalTest extends TestCase {

  public void test_match() throws Exception {

    // Simple match
    assertMatch("foo", "f", true);
    assertMatch("foo", "fo", true);
    assertMatch("foo", "foo", true);
    assertMatch("foo", "fooX", false);
    assertMatch("_foo", "_f", true);
    assertMatch("_foo", "_fo", true);
    assertMatch("_foo", "_foo", true);
    assertMatch("_foo", "_fooX", false);

    // CamelCase matching
    assertMatch("OneClass", "O", true);
    assertMatch("OneClass", "OC", true);
    assertMatch("OneClass", "OCl", true);
    assertMatch("OneClass", "OnC", true);
    assertMatch("OneClass", "Ol", false);
    assertMatch("OneClass", "OnCa", false);
    assertMatch("_OneClass", "_O", true);
    assertMatch("_OneClass", "_OC", true);
    assertMatch("_OneClass", "_OCl", true);
    assertMatch("_OneClass", "_OnC", true);
    assertMatch("_OneClass", "_Ol", false);
    assertMatch("_OneClass", "_OnCa", false);

    // Ignore leading _ when matching
    assertMatch("_foo", "f", true);
    assertMatch("_foo", "fo", true);
    assertMatch("_foo", "foo", true);
    assertMatch("_foo", "fooX", false);
    assertMatch("_OneClass", "O", true);
    assertMatch("_OneClass", "OC", true);
    assertMatch("_OneClass", "OCl", true);
    assertMatch("_OneClass", "OnC", true);
    assertMatch("_OneClass", "Ol", false);
    assertMatch("_OneClass", "OnCa", false);
  }

  private void assertMatch(String completion, String textEntered, boolean expected) {
    boolean actual = DartServerProposal.match(textEntered, completion);
    assertEquals(expected, actual);
  }
}
