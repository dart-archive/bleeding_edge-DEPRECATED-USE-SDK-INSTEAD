# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

{
  'conditions': [
    ['OS=="android"',
      {
        'targets': [
          {
            'target_name': 'android_app',
            'type': 'none',
            'dependencies': [
              'copy_extension',
              'copy_main',
              '../../../runtime/dart-runtime.gyp:android_embedder',
              'copy_embedder',
            ],
            'actions': [
              # TODO(gram) - this should have a debug and release version.
              {
                'action_name': 'build_app',
                'inputs': [
                  '<(PRODUCT_DIR)/lib.target/libandroid_embedder.so',
                  '../../../runtime/embedders/openglui/common/gl.dart',
                  '../src/openglui_raytrace.dart',
                ],
                'outputs': [
                  'bin/NativeActivity-debug.apk',
                ],
                'action': [ 'ant', 'debug' ]
              }
            ]
          },
          {
            'target_name': 'copy_embedder',
            'type': 'none',
            'copies': [ {
              # TODO(gram) - this should vary based on architecture.
              'destination': 'libs/x86',
              'files': [
                '<(PRODUCT_DIR)/lib.target/libandroid_embedder.so'
              ],
            }],
          },
          {
            'target_name': 'copy_extension',
            'type': 'none',
            'copies': [ {
              'destination': 'assets/dart',
              'files': [
                '../../../runtime/embedders/openglui/common/gl.dart'
              ],
            }],
          },
          {
            'target_name': 'copy_main',
            'type': 'none',
            'copies': [ {
              'destination': 'assets/dart',
              'files': [
                '../src/openglui_raytrace.dart',
              ],
            }],
          },
        ]
      }
    ]
  ]
}


