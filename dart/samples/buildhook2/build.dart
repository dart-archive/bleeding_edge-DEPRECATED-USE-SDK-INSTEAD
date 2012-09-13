// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("dart:io");
#import("package:args/args.dart");

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
  print("running build.dart...");

  processArgs();

  if (cleanBuild) {
    handleCleanCommand();
  } else if (fullBuild) {
    handleFullBuild();
  } else {
    handleChangedFiles(changedFiles);
    handleRemovedFiles(removedFiles);
  }

  // Return a non-zero code to indicate a build failure.
  //exit(1);
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

/**
 * Delete all generated files.
 */
void handleCleanCommand() {
  Directory current = new Directory.current();
  current.list(true).onFile = _maybeClean;
}

/**
 * Recursively scan the current directory looking for .foo files to process.
 */
void handleFullBuild() {
  var files = <String>[];
  var lister = new Directory.current().list(true);

  lister.onFile = (file) => files.add(file);
  lister.onDone = (_) => handleChangedFiles(files);
}

/**
 * Process the given list of changed files.
 */
void handleChangedFiles(List<String> files) {
  files.forEach(_processFile);
}

/**
 * Process the given list of removed files.
 */
void handleRemovedFiles(List<String> files) {

}

/**
 * Convert a .foo file to a .foobar file.
 */
void _processFile(String arg) {
  if (arg.endsWith(".foo")) {
    print("processing: ${arg}");

    File file = new File(arg);

    String contents = file.readAsTextSync();

    File outFile = new File("${arg}bar");

    OutputStream out = outFile.openOutputStream();
    out.writeString("Processed from ${file.name}:\n");
    if (contents != null) {
      out.writeString(contents);
    }
    out.close();

    print("wrote     : ${outFile.name}");
  }
}

/**
 * If this file is a generated file (based on the extension), delete it.
 */
void _maybeClean(String file) {
  if (file.endsWith(".foobar")) {
    new File(file).delete();
  }
}
