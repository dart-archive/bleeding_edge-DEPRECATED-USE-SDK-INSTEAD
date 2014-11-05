package com.google.dart.tools.core.utilities.download;

import junit.framework.TestCase;

public class UpdateDecoratorForUsageCountTest extends TestCase {

  public void test_decorateURL() throws Exception {
    String url = "http://foo";
    String newUrl = UpdateDecoratorForUsageCount.decorateURL(url);
    assertTrue(newUrl.contains("v="));
    // assert old functionality removed
    assertFalse(newUrl.contains("r="));
    assertFalse(newUrl.contains("cid="));
  }
}
