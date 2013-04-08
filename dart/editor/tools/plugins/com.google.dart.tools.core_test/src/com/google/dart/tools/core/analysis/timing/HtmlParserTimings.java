package com.google.dart.tools.core.analysis.timing;

import com.google.dart.engine.html.parser.HtmlParseResult;
import com.google.dart.engine.html.parser.HtmlParser;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 * Evaluate time to parse html files in a directory hierarchy using old and new HtmlParsers.
 */
public class HtmlParserTimings extends TestCase {
  interface ParseFileRunner {
    void run(File htmlFile) throws Exception;
  }
  interface ParseStringRunner {
    void run(File htmlFile, String contents) throws Exception;
  }

  final ParseFileRunner newParseFile = new ParseFileRunner() {
    private SourceFactory sourceFactory = new SourceFactory();

    @Override
    public void run(File htmlFile) throws Exception {
      Source source = new FileBasedSource(sourceFactory.getContentCache(), htmlFile);
      HtmlParseResult parseResult = new HtmlParser(source).parse(source);
      assertNotNull(parseResult);
    }
  };

  final ParseStringRunner newParseString = new ParseStringRunner() {
    private SourceFactory sourceFactory = new SourceFactory();

    @Override
    public void run(File htmlFile, String contents) throws Exception {
      Source source = new FileBasedSource(sourceFactory.getContentCache(), htmlFile);
      sourceFactory.setContents(source, contents);
      HtmlParseResult parseResult = new HtmlParser(source).parse(source);
      assertNotNull(parseResult);
    }
  };

  final ParseFileRunner oldParseFile = new ParseFileRunner() {
    @Override
    public void run(File htmlFile) throws IOException {
      String contents = FileUtilities.getContents(htmlFile);
      Object parseResult = new com.google.dart.tools.core.html.HtmlParser(contents).parse();
      assertNotNull(parseResult);
    }
  };

  final ParseStringRunner oldParseString = new ParseStringRunner() {
    @Override
    public void run(File htmlFile, String contents) throws IOException {
      Object parseResult = new com.google.dart.tools.core.html.HtmlParser(contents).parse();
      assertNotNull(parseResult);
    }
  };

  private File rootDir;
  private int fileCount;
  private long parseTime;

  public void test_timings() throws Exception {
    long newTime = timeParsingFile("New Html Parser - parse files", newParseFile);
    long oldTime = timeParsingFile("Old Html Parser - parse files", oldParseFile);
    System.out.println();
    System.out.println("Improvement: " + (((double) newTime) / oldTime) * 100 + " %");

    System.out.println();

    newTime = timeParsingString("New Html Parser - parse strings", newParseString);
    oldTime = timeParsingString("Old Html Parser - parse strings", oldParseString);
    System.out.println();
    System.out.println("Improvement: " + (((double) newTime) / oldTime) * 100 + " %");
  }

  @Override
  protected void setUp() throws Exception {
    String key = getClass().getSimpleName() + ".dir";
    String dirPath = System.getProperty(key);
    assertNotNull("Must define system property " + key, dirPath);
    rootDir = new File(dirPath);
    assertTrue("Must exist " + dirPath, rootDir.exists());
  }

  private long timeParsingFile(String message, ParseFileRunner runner) throws Exception {
    System.out.println(message);
    // Skip 1st two iterations
    traverse(rootDir, runner);
    traverse(rootDir, runner);
    fileCount = 0;
    parseTime = 0;
    for (int count = 0; count < 10; count++) {
      traverse(rootDir, runner);
    }
    System.out.println("  " + fileCount + " files in " + parseTime + " ms");
    System.out.println("  " + (((double) parseTime) / fileCount) + " ms/file");
    return parseTime;
  }

  private long timeParsingString(String message, ParseStringRunner runner) throws Exception {
    System.out.println(message);
    // Skip 1st two iterations
    traverse(rootDir, runner);
    traverse(rootDir, runner);
    fileCount = 0;
    parseTime = 0;
    for (int count = 0; count < 10; count++) {
      traverse(rootDir, runner);
    }
    System.out.println("  " + fileCount + " files in " + parseTime + " ms");
    System.out.println("  " + (((double) parseTime) / fileCount) + " ms/file");
    return parseTime;
  }

  private void traverse(File dir, ParseFileRunner runner) throws Exception {
    String[] names = dir.list();
    for (String name : names) {
      File file = new File(dir, name);
      if (file.isDirectory()) {
        traverse(file, runner);
      } else if (file.getName().endsWith(".html")) {
        long start = System.currentTimeMillis();
        runner.run(file);
        parseTime += System.currentTimeMillis() - start;
        fileCount++;
      }
    }
  }

  private void traverse(File dir, ParseStringRunner runner) throws Exception {
    String[] names = dir.list();
    for (String name : names) {
      File file = new File(dir, name);
      if (file.isDirectory()) {
        traverse(file, runner);
      } else if (file.getName().endsWith(".html")) {
        String contents = FileUtilities.getContents(file);
        long start = System.currentTimeMillis();
        runner.run(file, contents);
        parseTime += System.currentTimeMillis() - start;
        fileCount++;
      }
    }
  }
}
