package com.google.dart.tools.core.utilities.collections;

import com.google.dart.compiler.util.Lists;

import junit.framework.TestCase;

import java.io.File;
import java.util.List;

public class FileSetTest extends TestCase {
  static File relA = new File("A");
  static File A = relA.getAbsoluteFile();
  static File base = A.getParentFile();
  static File B = new File(base, "B");
  static File AA = new File(A, "AA");
  static File AB = new File(A, "AB");
  static File AAA = new File(AA, "AAA");
  static File AAB = new File(AA, "AAB");
  static File[] allFiles = new File[] {A, B, AA, AB, AAA, AAB};

  public void test_absolute() throws Exception {
    assertTrue(base.isAbsolute());
  }

  public void test_absolutePath() throws Exception {
    FileSet set = new FileSet();
    assertTrue(set.add(A));
    assertTrue(set.contains(relA));
    set = new FileSet();
    assertTrue(set.add(relA));
    assertTrue(set.contains(A));
  }

  public void test_addA() throws Exception {
    FileSet set = new FileSet();
    assertTrue(set.add(A));
    assertContains(set, A, AA, AB, AAA, AAB);
    assertFalse(set.add(A));
    assertContains(set, A, AA, AB, AAA, AAB);
  }

  public void test_addAA() throws Exception {
    FileSet set = new FileSet();
    assertTrue(set.add(AA));
    assertContains(set, AA, AAA, AAB);
    assertTrue(set.add(A));
    assertContains(set, A, AA, AB, AAA, AAB);
  }

  public void test_addAandAAA() throws Exception {
    FileSet set = new FileSet();
    assertTrue(set.add(A));
    assertFalse(set.add(AAA));
    assertContains(set, A, AA, AB, AAA, AAB);
  }

  public void test_addABandB() throws Exception {
    FileSet set = new FileSet();
    assertTrue(set.add(AB));
    assertContains(set, AB);
    assertTrue(set.add(B));
    assertContains(set, AB, B);
  }

  public void test_addAll() throws Exception {
    FileSet set = new FileSet();
    assertTrue(set.add(base));
    assertContains(set, allFiles);
  }

  public void test_new() throws Exception {
    FileSet set = new FileSet();
    assertContains(set);
  }

  public void test_removeA() throws Exception {
    FileSet set = new FileSet();
    assertFalse(set.remove(A));
    assertContains(set);
    assertTrue(set.add(A));
    assertTrue(set.remove(A));
    assertContains(set);
    assertTrue(set.add(AA));
    assertContains(set, AA, AAA, AAB);
    assertTrue(set.remove(A));
    assertContains(set);
  }

  public void test_removeAA() throws Exception {
    FileSet set = new FileSet();
    assertTrue(set.add(A));
    assertContains(set, A, AA, AB, AAA, AAB);
    assertTrue(set.remove(AA));
    assertContains(set, A, AB);
    assertTrue(set.add(AAA));
    assertContains(set, A, AB, AAA);
    assertTrue(set.remove(AA));
    assertContains(set, A, AB);
    assertFalse(set.remove(AA));
    assertContains(set, A, AB);
    assertTrue(set.add(AA));
    assertContains(set, A, AA, AB, AAA, AAB);
  }

  public void test_removeAAA() throws Exception {
    FileSet set = new FileSet();
    assertTrue(set.add(A));
    assertTrue(set.remove(AAA));
    assertContains(set, A, AA, AB, AAB);
  }

  public void test_removeAaddAAA() throws Exception {
    FileSet set = new FileSet();
    set.add(base);
    set.remove(A);
    set.add(AAA);
    assertContains(set, B, AAA);
  }

  private void assertContains(FileSet actual, File... files) {
    List<File> expected = Lists.create(files);
    for (File file : allFiles) {
      if (expected.contains(file)) {
        if (!actual.contains(file)) {
          fail("Expected set to contain " + file.getName());
        }
      } else {
        if (actual.contains(file)) {
          fail("Set should not contain " + file.getName());
        }
      }
    }
  }
}
