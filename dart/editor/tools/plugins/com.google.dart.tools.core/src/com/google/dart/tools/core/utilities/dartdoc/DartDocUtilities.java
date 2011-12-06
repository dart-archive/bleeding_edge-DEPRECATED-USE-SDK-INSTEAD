package com.google.dart.tools.core.utilities.dartdoc;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartDocumentable;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.ast.DartElementLocator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * A utility class for dealing with Dart doc text.
 */
public final class DartDocUtilities {

  /**
   * Convert from a Dart doc string with slashes and stars to a plain text representation of the
   * comment.
   * 
   * @param str
   * @return
   */
  public static String cleanDartDoc(String str) {
    // Remove /** */
    if (str.startsWith("/**")) {
      str = str.substring(3);
    }

    if (str.endsWith("*/")) {
      str = str.substring(0, str.length() - 2);
    }

    str = str.trim();

    // Remove leading '* ', and turn empty lines into \n's.
    StringBuilder builder = new StringBuilder();

    BufferedReader reader = new BufferedReader(new StringReader(str));

    try {
      String line = reader.readLine();
      int lineCount = 0;

      while (line != null) {
        line = line.trim();

        if (line.startsWith("*")) {
          line = line.substring(1).trim();
        }

        if (line.length() == 0) {
          lineCount = 0;
          builder.append("\n\n");
        } else {
          if (lineCount > 0) {
            builder.append(" ");
          }

          builder.append(line);
          lineCount++;
        }

        line = reader.readLine();
      }
    } catch (IOException exception) {
      // this will never be thrown
    }

    return builder.toString();
  }

  /**
   * Return the raw Dart doc string for the element in the given location, if any.
   * 
   * @param compilationUnit
   * @param unit
   * @param start
   * @param end
   * @return
   * @throws DartModelException
   */
  public static String getDartDoc(CompilationUnit compilationUnit, DartUnit unit, int start, int end)
      throws DartModelException {
    DartElementLocator locator = new DartElementLocator(compilationUnit, start, end);

    DartElement element = locator.searchWithin(unit);

    if (element instanceof DartDocumentable) {
      DartDocumentable documentable = (DartDocumentable) element;

      if (documentable.getDartDocRange() != null) {
        SourceRange range = documentable.getDartDocRange();

        String dartDoc = element.getOpenable().getBuffer().getText(range.getOffset(),
            range.getLength());

        return cleanDartDoc(dartDoc);
      }
    }

    return null;
  }

  private DartDocUtilities() {

  }

}
