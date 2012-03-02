// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

main() {
  {
    // Generates identical compile time constants.
    var s1 = "abcdefgh";
    var s2 = "abcd" "efgh";
    var s3 = "ab" "cd" "ef" "gh";
    var s4 = "a" "b" "c" "d" "e" "f" "g" "h";
    var s5 = "a" 'b' @"c" @'d' """e""" '''f''' @"""g""" @'''h''';
    Expect.isTrue(s1 === s2);
    Expect.isTrue(s1 === s3);
    Expect.isTrue(s1 === s4);
    Expect.isTrue(s1 === s5);
  }
  {
    // Separating whitespace isn't necessary for the tokenizer.
    var s1 = "abcdefgh";
    var s2 = "abcd""efgh";
    var s3 = "ab""cd""ef""gh";
    var s4 = "a""b""c""d""e""f""g""h";
    var s5 = "a"'b'@"c"@'d'"""e"""'''f'''@"""g"""@'''h''';
    Expect.isTrue(s1 === s2);
    Expect.isTrue(s1 === s3);
    Expect.isTrue(s1 === s4);
    Expect.isTrue(s1 === s5);
    // "a""""""b""" should be tokenized as "a" """""b""", aka. "a" '""b'.
    Expect.isTrue('a""b' === "a""""""b""");
    // """a""""""""b""" is 'a' '""b'.
    Expect.isTrue('a""b' === """a""""""""b""");
    // Raw strings.
    Expect.isTrue('ab' === "a"@"b");
    Expect.isTrue('ab' === @"a""b");
    Expect.isTrue('ab' === @"a"@"b");
  }

  // Newlines are just whitespace.
  var ms1 = "abc"
  "def"
  "ghi"
  "jkl";
  Expect.isTrue("abcdefghijkl" === ms1);

  // Works with multiline strings too.
  var ms2 = """abc
  def"""
  """
  ghi
  jkl
  """;
  Expect.isTrue("abc\n  def  ghi\n  jkl\n  " === ms2, "Multiline: $ms2");

  // Binds stronger than property access (it's considered one literal).
  Expect.equals(5, "ab" "cde".length, "Associativity");
}
