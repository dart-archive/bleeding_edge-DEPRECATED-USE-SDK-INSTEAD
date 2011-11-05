// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class TokenizerTest {
  static void main() {
    print('start TokenizerTest');
    testSourceLocations();
    testBasics();
    testIncomplete();
    testInterpolation();
    print('finished TokenizerTest');
  }

  static void checkLocation(source, position, expectedLine, expectedCol) {
    final loc = source.getLocationMessage('TEST', position, position, false);
    final expectedLoc = source.filename + ':$expectedLine:$expectedCol: TEST';
    Expect.equals(expectedLoc, loc);
  }

  static void testSourceLocations() {
    var filename = 'test.dart';
    var source = new SourceFile(filename, '012\n45\n7\n9');
    checkLocation(source, 0, 1, 1);
    checkLocation(source, 1, 1, 2);
    checkLocation(source, 2, 1, 3);
    checkLocation(source, 3, 1, 4);

    checkLocation(source, 4, 2, 1);
    checkLocation(source, 5, 2, 2);
    checkLocation(source, 6, 2, 3);

    checkLocation(source, 7, 3, 1);
    checkLocation(source, 8, 3, 2);

    checkLocation(source, 9, 4, 1);
    checkLocation(source, 10, 4, 2);
  }

  static void testBasics() {
    tokenTest('==> =>> +++',
              ['==', '>', '=>', '>', '++', '+']);

    tokenTest('0123q 0123 0xabcg 3.14 1e10',
             ['0123q', '0123', '0xabc', 'g', '3.14', '1e10'],
             [TokenKind.ERROR, TokenKind.INTEGER, TokenKind.HEX_INTEGER,
              TokenKind.IDENTIFIER, TokenKind.DOUBLE, TokenKind.DOUBLE]);

    tokenTest('\$"foo" @"bar" \$abc @\'foo\' \$\'bar\'',
              ['\$"foo"', '@"bar"', '\$abc', "@'foo'", "\$'bar'"],
              [TokenKind.STRING, TokenKind.STRING, TokenKind.IDENTIFIER,
               TokenKind.STRING, TokenKind.STRING]);


    tokenTest('"hello \\"world" // comment',
              ['"hello \\"world"', '// comment'],
              [TokenKind.STRING, TokenKind.COMMENT]);

    tokenTest('a_34xy _32x a.b.c',
              ['a_34xy', '_32x', 'a', '.', 'b', '.', 'c']);

    tokenTest('forclass for+ class in while',
              ['forclass', 'for', '+', 'class', 'in', 'while'],
              [TokenKind.IDENTIFIER, TokenKind.FOR, TokenKind.ADD,
               TokenKind.CLASS, TokenKind.IN, TokenKind.WHILE]);
  }

  static void testIncomplete() {
    tokenTest('"hello', ['"hello'],
              [TokenKind.INCOMPLETE_STRING]);
    tokenTest('"""hello', ['"""hello'],
              [TokenKind.INCOMPLETE_MULTILINE_STRING_DQ]);
    tokenTest("'''hello", ["'''hello"],
              [TokenKind.INCOMPLETE_MULTILINE_STRING_SQ]);
    tokenTest('/* Comment * * /', ['/* Comment * * /'],
              [TokenKind.INCOMPLETE_COMMENT]);

    tokenTest('@', ['@'], [TokenKind.ERROR]);
  }

  static void testInterpolation() {
    tokenTest("foo''", ["foo", "''"],
      [TokenKind.IDENTIFIER, TokenKind.STRING]);
    tokenTest("'hello'", ["'hello'"],
      [TokenKind.STRING]);
    tokenTest("'hello \$foo more'", ["'hello \$", "foo", " more'"],
      [TokenKind.INCOMPLETE_STRING, TokenKind.IDENTIFIER, TokenKind.STRING]);

    tokenTest("'\${2 + x} = \${y('bar')}'",
      ["'\$", "{", "2", "+", "x", "}", " = \$",
        "{", "y", "(", "'bar'", ")", "}", "'"]);

    tokenTest('"hello \$foo"', ['"hello \$', "foo", '"'],
      [TokenKind.INCOMPLETE_STRING, TokenKind.IDENTIFIER, TokenKind.STRING]);


    tokenTest("'hello \$foo'", ["'hello \$", "foo", "'"],
      [TokenKind.INCOMPLETE_STRING, TokenKind.IDENTIFIER, TokenKind.STRING]);

  }

  static void tokenTest(String code, List<String> expected,
                        [List<int> kinds=null]) {
    final source = new SourceFile('test.dart', code);
    final tokenizer = new Tokenizer(source, false);
    for (int i=0; i < expected.length; i++) {
      final token = tokenizer.next();
      //print('${token.text} ${TokenKind.kindToString(token.kind)}');
      Expect.equals(expected[i], token.text);
      if (kinds != null) {
        Expect.equals(kinds[i], token.kind);
      }
    }
    var t = tokenizer.next();
    Expect.equals(TokenKind.END_OF_FILE, tokenizer.next().kind);
  }
}
