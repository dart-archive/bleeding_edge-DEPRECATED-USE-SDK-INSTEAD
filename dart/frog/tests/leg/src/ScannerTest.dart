// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('../../../leg/scanner/scannerlib.dart');
#import('../../../leg/scanner/scanner_implementation.dart');
#source('../../../leg/scanner/byte_strings.dart');
#source('../../../leg/scanner/byte_array_scanner.dart');

Token scan(List<int> bytes) => new ByteArrayScanner(bytes).tokenize();

bool isRunningOnJavaScript() => 1 === 1.0;

main() {
  // Google favorite: "Îñţérñåţîöñåļîžåţîờñ".
  Token token = scan([0xc3, 0x8e, 0xc3, 0xb1, 0xc5, 0xa3, 0xc3, 0xa9, 0x72,
                      0xc3, 0xb1, 0xc3, 0xa5, 0xc5, 0xa3, 0xc3, 0xae, 0xc3,
                      0xb6, 0xc3, 0xb1, 0xc3, 0xa5, 0xc4, 0xbc, 0xc3, 0xae,
                      0xc5, 0xbe, 0xc3, 0xa5, 0xc5, 0xa3, 0xc3, 0xae, 0xe1,
                      0xbb, 0x9d, 0xc3, 0xb1]);
  Expect.stringEquals("Îñţérñåţîöñåļîžåţîờñ", token.value.toString());

  // Blueberry porridge in Danish: "blåbærgrød".
  token = scan([0x62, 0x6c, 0xc3, 0xa5, 0x62, 0xc3, 0xa6, 0x72, 0x67, 0x72,
                0xc3, 0xb8, 0x64]);
  Expect.stringEquals("blåbærgrød", token.value.toString());

  // "சிவா அணாமாைல", that is "Siva Annamalai" in Tamil.
  token = scan([0xe0, 0xae, 0x9a, 0xe0, 0xae, 0xbf, 0xe0, 0xae, 0xb5, 0xe0,
                0xae, 0xbe, 0x20, 0xe0, 0xae, 0x85, 0xe0, 0xae, 0xa3, 0xe0,
                0xae, 0xbe, 0xe0, 0xae, 0xae, 0xe0, 0xae, 0xbe, 0xe0, 0xaf,
                0x88, 0xe0, 0xae, 0xb2]);
  Expect.stringEquals("சிவா", token.value.toString());
  Expect.stringEquals("அணாமாைல", token.next.toString());

  // "िसवा अणामालै", that is "Siva Annamalai" in Devanagari.
  token = scan([0xe0, 0xa4, 0xbf, 0xe0, 0xa4, 0xb8, 0xe0, 0xa4, 0xb5, 0xe0,
                0xa4, 0xbe, 0x20, 0xe0, 0xa4, 0x85, 0xe0, 0xa4, 0xa3, 0xe0,
                0xa4, 0xbe, 0xe0, 0xa4, 0xae, 0xe0, 0xa4, 0xbe, 0xe0, 0xa4,
                0xb2, 0xe0, 0xa5, 0x88]);
  Expect.stringEquals("िसवा", token.value.toString());
  Expect.stringEquals("अणामालै", token.next.toString());

  if (!isRunningOnJavaScript()) {
    // DESERET CAPITAL LETTER BEE, unicode 0x10412(0xD801+0xDC12)
    // UTF-8: F0 90 90 92
    token = scan([0xf0, 0x90, 0x90, 0x92]);
    Expect.stringEquals("𐐒", token.value.toString());
  } else {
    print('Skipping non-BMP character test');
  }
}
