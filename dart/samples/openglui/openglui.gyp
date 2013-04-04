# Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

{
  'variables': {
    # Disable the OpenGLUI embedder by default on desktop OSes.  Note,
    # to build this on the desktop, you need GLUT installed.
    # TODO(vsm): This is also defined in runtime/dart-runtime.gyp.  Can we
    # reuse that definition?
    'enable_openglui%': 0,
  },
  'targets': [
    {
      'target_name': 'openglui_sample',
      'type': 'none',
      'conditions': [
        ['OS=="android"', {
            'dependencies': [
              'web/web.gyp:assets',
              'android-webgl-raytrace/android.gyp:android_app',
              'android-canvas-tests/android.gyp:android_app',
              'android-blasteroids/android.gyp:android_app',
            ],
          },
        ],
        ['enable_openglui==1', {
            'dependencies': [
              'web/web.gyp:assets',
              'emulator/emulator.gyp:mobile_emulator_app',
            ],
          },
        ],
      ],
    },
  ],
}
