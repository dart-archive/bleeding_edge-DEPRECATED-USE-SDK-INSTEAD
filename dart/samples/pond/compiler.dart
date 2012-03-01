// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** Embeds the frog compiler as a worker isolate. */
#library('compiler');

#import('dart:isolate', prefix: 'isolate');
#import("../../frog/file_system_dom.dart");
#import("../../frog/lang.dart", prefix: 'frog');

main() {
  final fileSystem = new DomFileSystem();
  isolate.port.receive((input, replyTo) {
    final watch = new Stopwatch.start();
    // Each message is a compilation request.
    fileSystem.writeString("user.dart", input['code']);
    frog.parseOptions('../../frog', ['dummyArg1', 'dummyArg2', 'user.dart'],
      fileSystem);
    frog.options.useColors = false;
    frog.options.warningsAsErrors = input['warningsAsErrors'];
    frog.initializeWorld(fileSystem);

    // Collect compilation messages
    final warnings = [];
    frog.world.messageHandler = (prefix, msg, span) {
      final warning = { 'prefix': prefix, 'msg': msg};

      if (span != null) {
        warning['filename'] = span.file.filename;
        warning['line'] = span.line;
        warning['column'] = span.column;
        warning['endLine'] = span.endLine;
        warning['endColumn'] = span.endColumn;
        warning['locationText'] = span.locationText;
      }
      warnings.add(warning);
    };

    bool success = frog.world.compile();
    watch.stop();

    // Send results back to the requester isolate.
    String output = success ? frog.world.getGeneratedCode() : null;
    replyTo.send({
      'success': success,
      'code': output,
      'warnings': warnings,
      'time': watch.elapsedInMs()});
  });
}
