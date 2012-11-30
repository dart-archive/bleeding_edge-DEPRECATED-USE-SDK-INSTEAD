# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

{
  'conditions': [
    ['OS=="android"',
      {
        'targets': [
          {
            'target_name': 'android_sample',
            'type': 'none',
            'dependencies': [
              '../android_embedder/android_embedder.gyp:android_embedder',
            ],
            'actions': [
              # TODO(gram) - this should have a debug and release version.
              {
                'action_name': 'build_app',
                'inputs': [
                  'jni/*.cc',
                  'jni/*.h',
                  'assets/dart/*.dart',
                ],
                'outputs': [
                  'bin/NativeActivity-debug.apk',
                ],
                'action': [ 'ant', 'debug' ]
              }
            ]
          }
        ]
      }
    ]
  ]
}


