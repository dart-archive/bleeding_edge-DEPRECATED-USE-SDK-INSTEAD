# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

{
  'variables': {
    'snapshot_cc_file': '<(SHARED_INTERMEDIATE_DIR)/snapshot_gen.cc',
  },
  'conditions': [
    ['OS=="android"',
      {
        'targets': [
          {
            # Dart shared library for Android.
            'target_name': 'android_embedder',
            'type': 'shared_library',
            'dependencies': [
              '../../runtime/dart-runtime.gyp:libdart_export',
              '../../runtime/dart-runtime.gyp:libdart_builtin',
              '../../runtime/dart-runtime.gyp:generate_snapshot_file',
            ],
            'include_dirs': [
              '../../runtime'
            ],
            'defines': [
              'DART_SHARED_LIB'
            ],
            'sources': [
              'support_android.cc',
              'builtin_nolib.cc',
              '../../runtime/bin/socket.cc',
              '../../runtime/bin/socket_android.cc',
              '../../runtime/bin/eventhandler.cc',
              '../../runtime/bin/eventhandler_android.cc',
              '../../runtime/bin/process.cc',
              '../../runtime/bin/process_android.cc',
              '../../runtime/bin/platform.cc',
              '../../runtime/bin/platform_android.cc',
              '<(snapshot_cc_file)',
            ],
            'link_settings': {
              'libraries': [ '-llog', '-lc' ],
              'ldflags': [
                '-z', 'muldefs'
              ],
              'ldflags!': [
                '-Wl,--exclude-libs=ALL',
              ],
            },
          },
        ],
      },
    ]
  ],
}

