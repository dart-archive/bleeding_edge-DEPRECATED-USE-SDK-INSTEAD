package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.AbstractDartCoreTest;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.test.util.FileUtilities;
import com.google.dart.tools.core.test.util.TestUtilities;

import java.io.File;
import java.io.IOException;

public abstract class AbstractDartAnalysisTest extends AbstractDartCoreTest {

  protected static final long FIVE_MINUTES_MS = 300000;

  protected static File tempDir;
  protected static File moneyDir;
  protected static File moneyLibFile;
  protected static File simpleMoneySrcFile;
  protected static File bankDir;
  protected static File bankLibFile;
  protected static File localBankFile;
  protected static File packagesDir;
  protected static File pubspecFile;
  protected static File customerDir;
  protected static File customerLibFile;
  protected static File nestedAppFile;
  protected static File nestedLibFile;

  protected static void setUpBankExample() throws IOException {
    tempDir = TestUtilities.createTempDirectory();

    moneyDir = new File(tempDir, "Money");
    TestUtilities.copyPluginRelativeContent("Money", moneyDir);
    moneyLibFile = new File(moneyDir, "money.dart");
    assertTrue(moneyLibFile.exists());
    simpleMoneySrcFile = new File(moneyDir, "simple_money.dart");
    assertTrue(simpleMoneySrcFile.exists());

    bankDir = new File(tempDir, "Bank");
    TestUtilities.copyPluginRelativeContent("Bank", bankDir);
    bankLibFile = new File(bankDir, "bank.dart");
    assertTrue(bankLibFile.exists());
    localBankFile = new File(bankDir, "localbank.dart");
    packagesDir = new File(bankDir, DartCore.PACKAGES_DIRECTORY_NAME);
    assertTrue(packagesDir.exists());
    pubspecFile = new File(bankDir, DartCore.PUBSPEC_FILE_NAME);
    assertTrue(pubspecFile.exists());
    // editor resolves package: refs to the canonical file
    customerDir = new File(packagesDir, "customer").getCanonicalFile();
    customerLibFile = new File(customerDir, "customer.dart");
    assertTrue(customerLibFile.exists());

    File nestedDir = new File(bankDir, "nested");
    nestedAppFile = new File(nestedDir, "nestedApp.dart");
    assertTrue(nestedAppFile.exists());
    nestedLibFile = new File(nestedDir, "nestedLib.dart");
    assertTrue(nestedLibFile.exists());
    assertTrue(new File(nestedDir, "packages").mkdir());
  }

  protected static void tearDownBankExample() {
    FileUtilities.delete(tempDir);
    tempDir = null;
  }

  public AbstractDartAnalysisTest() {
    super();
  }

  public AbstractDartAnalysisTest(String name) {
    super(name);
  }

}
