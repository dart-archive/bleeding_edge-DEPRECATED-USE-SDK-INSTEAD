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
              'copy_assets',
            ],
            'actions': [
              # TODO(gram) - this should have a debug and release version.
              {
                'action_name': 'build_app',
                'inputs': [
                  '<(PRODUCT_DIR)/lib.target/libandroid_embedder.so',
                  '../../../runtime/embedders/openglui/common/gl.dart',
                  '../src/blasteroids.dart',
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
                '../src/blasteroids.dart',
              ],
            }],
          },
          {
            'target_name': 'copy_assets',
            'type': 'none',
            'copies': [ {
              'destination': 'assets',
              'files': [
                '../web/asteroid1.png',
                '../web/asteroid2.png',
                '../web/asteroid3.png',
                '../web/asteroid4.png',
                '../web/bg3_1.png',
                '../web/enemyship1.png',
                '../web/player.png',
                '../web/shield.png',
              ],
            }],
          },
        ]
      }
    ]
  ]
}


