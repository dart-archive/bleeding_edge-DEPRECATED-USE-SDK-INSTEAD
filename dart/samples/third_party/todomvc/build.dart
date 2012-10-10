// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:io';
import 'package:args/args.dart';
import 'package:web_components/dwc.dart' as dwc;

bool cleanBuild;
bool fullBuild;
List<String> changedFiles;
List<String> removedFiles;

/**
 * This build script is invoked automatically by the Editor whenever a file
 * in the project changes. It must be placed in the root of a project and named
 * 'build.dart'. See the source code of [processArgs] for information about the
 * legal command line options.
 */
void main() {
  processArgs();

  if (cleanBuild) {
    handleCleanCommand();
  } else if (fullBuild || changedFiles.some(_isInputFile)
      || removedFiles.some(_isInputFile)) {

    // Do a full build if any input file changed
    // TODO(jmesserly): we need a better way to detect the input file
    dwc.run(['web/main.html']);
  }
}

bool _isGeneratedFile(String filePath) {
  return new Path.fromNative(filePath).filename.startsWith('_');
}

// TODO(jmesserly): a lot of this build logic (like argument parsing) is common
// between tools, we should refactor it so it isn't copied into this file.
bool _isInputFile(String path) {
  return (path.endsWith(".dart") || path.endsWith(".html"))
      && !_isGeneratedFile(path);
}

/** Delete all generated files. */
void handleCleanCommand() {
  new Directory('web').list(false).onFile = (String path) {
    if (_isGeneratedFile(path)) {
      // TODO(jmesserly): we need a cleaner way to do this with dart:io.
      // The bug is that DirectoryLister returns native paths, so you need to
      // use Path.fromNative to work around this. Ideally we could just write:
      //    new File(path).delete();
      new File.fromPath(new Path.fromNative(path)).delete();
    }
  };
}

/**
 * Handle the --changed, --removed, --clean and --help command-line args.
 */
void processArgs() {
  var parser = new ArgParser();
  parser.addOption("changed", help: "the file has changed since the last build",
      allowMultiple: true);
  parser.addOption("removed", help: "the file was removed since the last build",
      allowMultiple: true);
  parser.addFlag("clean", negatable: false, help: "remove any build artifacts");
  parser.addFlag("help", negatable: false, help: "displays this help and exit");
  var args = parser.parse(new Options().arguments);
  if (args["help"]) {
    print(parser.getUsage());
    exit(0);
  }

  changedFiles = args["changed"];
  removedFiles = args["removed"];
  cleanBuild = args["clean"];
  fullBuild = changedFiles.isEmpty() && removedFiles.isEmpty() && !cleanBuild;
}
