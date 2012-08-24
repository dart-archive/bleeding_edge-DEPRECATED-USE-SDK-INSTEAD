// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("dart:io");

bool cleanBuild;
bool fullBuild;
List<String> changedFiles;
List<String> removedFiles;

/**
 * This build script is invoked automatically by the Editor whenever a file
 * in the project changes. It must be placed in the root of a project and named
 * 'build.dart'. The legal command-line parameters are:
 *
 * * <none>: a full build is requested
 * * --clean: remove any build artifacts
 * * --changed=one.foo: the file has changed since the last build
 * * --removed=one.foo: the file has been removed since the last build
 *
 * Any error code other then 0 returned by the script is considered an error.
 * The error, and any stdout or stderr text, will by printed to the console.
 */
void main() {
  print("running build.dart...");

  // TODO: change this script to use the pkg/args library
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
 * Handle the -clean and -changed command-line args.
 */
void processArgs() {
  changedFiles = [];
  removedFiles = [];

  List<String> args = new Options().arguments;

  for (String arg in args) {
    if (arg == "--clean") {
      cleanBuild = true;
    } else if (arg.startsWith("--changed=")) {
      changedFiles.add(arg.substring("--changed=".length));
    } else if (arg.startsWith("--removed=")) {
      removedFiles.add(arg.substring("--removed=".length));
    }
  }

  fullBuild = changedFiles.isEmpty() && removedFiles.isEmpty() && !cleanBuild;
}

/**
 * Delete all generated .foobar files.
 */
void handleCleanCommand() {
  Directory current = new Directory.current();
  DirectoryLister lister = current.list(true);

  lister.onFile = (String file) {
    if (file.endsWith(".foobar")) {
      new File(file).delete();
    }
  };
}

/**
 * Recursively scan the current directory looking for .foo files to process.
 */
void handleFullBuild() {
  List<String> files = [];

  Directory current = new Directory.current();
  DirectoryLister lister = current.list(true);

  lister.onFile = (String file) {
    if (file.endsWith(".foo")) {
      files.add(file);
    }
  };
  lister.onDone = (bool completed) => handleChangedFiles(files);
}

/**
 * Process the given list of changed files.
 */
void handleChangedFiles(List<String> files) {
  for (String arg in files) {
    _processFile(arg);
  }
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
