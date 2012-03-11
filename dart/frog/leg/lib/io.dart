// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// This is a copy of the VM's dart:io library. This API is not usable
// when running inside a web browser. Nevertheless, Leg provides a
// mock version of the dart:io library so that it can statically
// analyze programs that use dart:io.

// TODO(ahe): Separate API from implementation details.

#library("io");
#import("dart:coreimpl");
#import("dart:isolate");
// TODO(ahe): Should Leg support this library?
// #import("dart:nativewrappers");
#import("dart:uri");
#source('../../../runtime/bin/buffer_list.dart');
#source('../../../runtime/bin/chunked_stream.dart');
#source('../../../runtime/bin/directory.dart');
// Uses native keyword.
// #source('../../../runtime/bin/directory_impl.dart');
// Uses native keyword.
// #source('../../../runtime/bin/eventhandler.dart');
#source('../../../runtime/bin/file.dart');
// Uses native keyword.
// #source('../../../runtime/bin/file_impl.dart');
#source('../../../runtime/bin/http.dart');
#source('../../../runtime/bin/http_impl.dart');
#source('../../../runtime/bin/http_parser.dart');
#source('../../../runtime/bin/http_utils.dart');
#source('../../../runtime/bin/input_stream.dart');
#source('../../../runtime/bin/list_stream.dart');
#source('../../../runtime/bin/list_stream_impl.dart');
#source('../../../runtime/bin/output_stream.dart');
#source('../../../runtime/bin/stream_util.dart');
#source('../../../runtime/bin/string_stream.dart');
#source('../../../runtime/bin/platform.dart');
// Uses native keyword.
// #source('../../../runtime/bin/platform_impl.dart');
#source('../../../runtime/bin/process.dart');
// Uses native keyword.
// #source('../../../runtime/bin/process_impl.dart');
#source('../../../runtime/bin/socket.dart');
// Uses native keyword.
// #source('../../../runtime/bin/socket_impl.dart');
#source('../../../runtime/bin/socket_stream.dart');
#source('../../../runtime/bin/socket_stream_impl.dart');
// Uses native keyword.
// #source('../../../runtime/bin/stdio.dart');
#source('../../../runtime/bin/timer.dart');
#source('../../../runtime/bin/timer_impl.dart');

class _File implements File {
  factory File(arg) {
    throw new UnsupportedOperationException('new File($arg)');
  }
}
