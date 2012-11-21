/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.command.analyze;

import com.google.common.io.Closeables;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;

/**
 * An error formatter that scans the source file and prints the error line and some context around
 * it. This formatter has two modes: with or without color. When using colors, it prints the error
 * message in red, and it highlights the portion of the line containing the error in red. Without
 * colors, it prints an extra line underlying the portion of the line containing the error.
 */
public class CommandLineErrorFormatter {

  public static enum ErrorFormat {
    NORMAL, // Library/File, line, message
    MACHINE, // All information including severity, subsystem, etc
  }

  private final PrintStream outputStream;
  private final ErrorFormat errorFormat;

  public static final String ERROR_BOLD_COLOR = "\033[31;1m";
  public static final String ERROR_COLOR = "\033[31m";

  // Mix ANSI with xterm colors, giving ANSI priority.  The terminal should ignore xterm codes
  // if it doesn't support them.
  public static final String WARNING_BOLD_COLOR = "\033[33;1m\033[38;5;202m";
  public static final String WARNING_COLOR = "\033[33m\033[38;5;208m";

  public static final String NO_COLOR = "\033[0m";

  /**
   * Returns the closest value in {@code [start,end]} to the given value. If the given range is
   * entirely empty, then {@code start} is returned.
   */
  private static int between(int val, int start, int end) {
    return Math.max(start, Math.min(val, end));
  }

  private final boolean useColor;

  public CommandLineErrorFormatter(PrintStream outputStream, boolean useColor,
      ErrorFormat errorFormat) {
    this.outputStream = outputStream;
    this.errorFormat = errorFormat;
    this.useColor = useColor;
  }

  public void format(AnalysisError event, LineInfo lineInfo) {
    Source sourceFile = event.getSource();

    BufferedReader reader = null;
    try {
      final String[] fileContent = new String[1];
      Source.ContentReceiver receiver = new Source.ContentReceiver() {
        @Override
        public void accept(CharBuffer contents) {
          fileContent[0] = contents.toString();
        }

        @Override
        public void accept(String contents) {
          fileContent[0] = contents;
        }
      };
      sourceFile.getContents(receiver);
      Reader sourceReader = new StringReader(fileContent[0]);
      if (sourceReader != null) {
        reader = new BufferedReader(sourceReader);
      }

      // get the error line and the line above it (note: line starts at 1)
      LineInfo.Location location = lineInfo.getLocation(event.getOffset());
      int line = location.getLineNumber();
      String lineBefore = null;
      String lineText = null;

      if (reader != null) {
        lineBefore = getLineAt(reader, line - 1);
        lineText = getLineAt(reader, 1);
      }

      // if there is no line to highlight, default to the basic error formatter
      if (lineText == null) {
        plainFormat(event, lineInfo);
        return;
      }

      // get column/length and ensure they are within the line limits.
      int col = location.getColumnNumber() - 1;
      int length = event.getLength();
      col = between(col, 0, lineText.length());
      length = between(length, 0, lineText.length() - col);
      length = length == 0 ? lineText.length() - col : length;

      // print the error message
      StringBuilder buf = new StringBuilder();
      if (useColor) {
        buf.append(event.getErrorCode().getErrorSeverity() == ErrorSeverity.WARNING
            ? WARNING_BOLD_COLOR : ERROR_BOLD_COLOR);
      }
      if (errorFormat == ErrorFormat.MACHINE) {
        buf.append(String.format(
            "%s|%s|%s|%s|%d|%d|%d|%s",
            escapePipe(event.getErrorCode().getErrorSeverity().toString()),
            escapePipe(event.getErrorCode().getSubSystem().toString()),
            escapePipe(event.getErrorCode().toString()),
            escapePipe(sourceFile.getFullName()),
            location.getLineNumber(),
            1 + col,
            length,
            escapePipe(event.getMessage())));
      } else {
        String sourceName = sourceFile.getFullName();
        String includeFrom = getImportString(sourceFile);
        buf.append(String.format(
            "%s:%d: %s%s",
            sourceName,
            location.getLineNumber(),
            event.getMessage(),
            includeFrom));
      }
      if (useColor) {
        buf.append(NO_COLOR);
      }
      buf.append("\n");
      // show the previous line for context
      if (lineBefore != null) {
        buf.append(String.format("%6d: %s\n", line - 1, lineBefore));
      }

      if (useColor) {
        // highlight error in red
        buf.append(String.format(
            "%6d: %s%s%s%s%s\n",
            line,
            lineText.substring(0, col),
            event.getErrorCode().getErrorSeverity() == ErrorSeverity.WARNING ? WARNING_COLOR
                : ERROR_COLOR,
            lineText.substring(col, col + length),
            NO_COLOR,
            lineText.substring(col + length)));
      } else {
        // print the error line without formatting
        buf.append(String.format("%6d: %s\n", line, lineText));

        // underline error portion
        buf.append("        ");
        for (int i = 0; i < col; ++i) {
          buf.append(' ');
        }
        buf.append('~');
        if (length > 1) {
          for (int i = 0; i < length - 2; ++i) {
            buf.append('~');
          }
          buf.append('~');
        }
        buf.append('\n');
      }

      outputStream.print(buf.toString());
    } catch (Exception exception) {
      plainFormat(event, lineInfo);
    } finally {
      if (reader != null) {
        Closeables.closeQuietly(reader);
      }
    }
  }

  public String getImportString(Source sourceFile) {
    String includeFrom = "";
    // TODO(zundel): figure out how to extract 'imported from' 
//    if (sourceFile instanceof DartSource) {
//      LibrarySource lib = ((DartSource) sourceFile).getLibrary();
//      if (!sourceFile.getUri().equals(lib.getUri())) {
//        includeFrom = " (sourced from " + lib.getUri() + ")";
//      }
//    }
    return includeFrom;
  }

  private String escapePipe(String input) {
    StringBuilder result = new StringBuilder();
    for (char c : input.toCharArray()) {
      if (c == '\\' || c == '|') {
        result.append('\\');
      }
      result.append(c);
    }
    return result.toString();
  }

  private String getLineAt(BufferedReader reader, int line) throws IOException {
    if (line <= 0) {
      return null;
    }
    String currentLine = null;
    // TODO(sigmund): do something more efficient - we currently do a linear
    // scan of the file every time an error is reported. This will not scale
    // when many errors are reported on the same file.
    while ((currentLine = reader.readLine()) != null && line-- > 1) {
    }
    return currentLine;
  }

  private void plainFormat(AnalysisError event, LineInfo lineInfo) {
    String sourceName = "<unknown-source-file>";
    Source sourceFile = event.getSource();
    String includeFrom = getImportString(sourceFile);

    if (sourceFile != null) {
      sourceName = sourceFile.getFullName();
    }
    LineInfo.Location location = lineInfo.getLocation(event.getOffset());
    outputStream.printf(
        "%s:%d:%d: %s%s\n",
        sourceName,
        location.getLineNumber(),
        location.getColumnNumber(),
        event.getMessage(),
        includeFrom);
  }
}
