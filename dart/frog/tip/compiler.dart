// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
#library("tip");
#import('dart:dom', prefix: 'dom');
#import('../lang.dart');
#import('../file_system_dom.dart');

/** The filename to use for Dart code compiled directly from the browser. */
final String DART_FILENAME = 'input.dart';

/** Helper to get my base url. */
String getRootPath(dom.Window window) {
  String url = window.location.href;
  final tail = url.lastIndexOf('/', url.length);
  final dir = url.substring(0, tail);
  return dir;
}

/**
 * This is the entry point used by autodart.js.
 */
// TODO(jimhug): Better JS integration story.
String compile(String code) {
  world.files.writeString(DART_FILENAME, code);
  world.reset();
  var success = world.compile();

  if (success) {
    return world.getGeneratedCode();
  } else {
    print('compilation failed');
    return "";
  }
}

/**
 * Load the contents of a file as a String.  Load relative paths with
 * respect to the containing HTML page.
 */
String load(String filename) {
  final fileSystem = new DomFileSystem();
  return fileSystem.readAll(filename);
}

/**
 * Invoke this script in the given window's context.  Use
 * this frame's window by default.
 */
void inject(String code, [dom.Window window = null,
                          String name = 'generated.js']) {
  if (window == null) window = dom.window;

  final document = window.document;
  final script = document.createElement('script');
  // TODO(vsm): Enable debugging of injected code.  This sourceURL
  // trick only appears to work for eval'ed code, not script injected
  // code.
  // Append sourceURL to enable debugging.
  script.innerHTML = code + '\n//@ sourceURL=$name';
  script.type = 'application/javascript';
  document.body.appendChild(script);
}

/**
 * Compile all dart scripts in a window and inject/invoke the corresponding JS.
 */
void frogify(String systemPath, String userPath, [dom.Window window = null]) {
  if (window == null) window = dom.window;

  initialize(systemPath, userPath);
  final document = window.document;
  int n = document.scripts.length;
  // TODO(vsm): Implement foreach iteration on native DOM types.  This
  // should be for (var script in document.scripts) { ... }.
  for (int i = 0; i < n; ++i) {
    final script = document.scripts[i];
    if (script.type == 'application/dart') {
      final src = script.src;
      var input;
      var name;
      if (src == '') {
        input = script.innerHTML;
        name = null;
      } else {
        input = world.files.readAll(src);
        name = '$src.js';
      }
      final output = compile(input);
      inject(output, window, name);
    }
  }
}

/**
 * This implements all of the logic for tip.html.
 */
// TODO(jimhug): Need better integration story for JS and for chrome
//  extension packaging.  Once that's available, this gets move to its own
//  file and hopefully its own library/app.
void runTip() {
  // TODO(vsm): dom.document is null here, fix dom library?
  final document = dom.window.document;
  final console = document.getElementById('console');
  console.innerHTML = '';

  final textarea = document.getElementById('code');
  final text = textarea.value;

  final generated = compile(text);
  final codearea = document.getElementById('generated');
  codearea.value = generated;

  // Scroll generated code and output console to the bottoms.
  codearea.scrollTop = codearea.scrollHeight;
  console.scrollTop = console.scrollHeight;

  // Clear the output iframe and inject the generated JS as a script.
  final iframe = document.getElementById('output');
  inject(generated, iframe.contentWindow);
}

void initialize(String systemPath, [String userPath = null]) {
  parseOptions(systemPath, [null, null, DART_FILENAME]);
  initializeWorld(new DomFileSystem(userPath));
}

void main() {
  initialize(getRootPath(dom.window));

  // TODO(jimhug): This is evidence of unholy alliance of JS and Dart here.
  if (false) {
    compile('void main() {}');
    runTip();
  }
}
