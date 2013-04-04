# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# We should be able to make this work on Mac. We need:
# -framework OpenGL -framework GLUT -lm -L /usr/X11/lib

{
  'targets': [
    {
      'target_name': 'mobile_emulator_app',
      'type': 'none',
      'conditions': [
        ['OS=="linux" or OS=="mac"',
          {
            'dependencies': [
              '../../../runtime/dart-runtime.gyp:emulator_embedder',
              'mobile_emulator',
              'copy_dart_files',
              '../web/web.gyp:assets',
              'copy_assets',
            ]
          }
        ]
      ]
    },
    {
      'target_name': 'copy_dart_files',
      'type': 'none',
      'copies': [ {
        'destination': '<(PRODUCT_DIR)',
        'files': [
           '../../../runtime/embedders/openglui/common/gl.dart',
           '../src/openglui_raytrace.dart',
           '../src/openglui_canvas_tests.dart',
           '../src/flashingbox.dart',
           '../src/blasteroids.dart'
        ],
      }],
    },
    {
      'target_name': 'copy_assets',
      'type': 'none',
      'copies': [ {
        'destination': '<(PRODUCT_DIR)',
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
    {
      'target_name': 'mobile_emulator',
      'type': 'executable',
      'dependencies': [
        '../../../runtime/dart-runtime.gyp:emulator_embedder',
      ],
      'include_dirs': [
         '../../../runtime',
         '/usr/X11/include',
       ],
       'sources': [
          'mobile_emulator_sample.cc',
       ],
       'conditions': [
         ['OS=="linux"',
           {
             'link_settings': {
               'libraries': [
                 '-Wl,--start-group',
                 '-lskia_core',
                 '-lskia_effects',
                 '-lskia_gr',
                 '-lskia_images',
                 '-lskia_opts',
                 '-lskia_opts_ssse3',
                 '-lskia_ports',
                 '-lskia_sfnt',
                 '-lskia_skgr',
                 '-lskia_utils',
                 '-Wl,--end-group',
                 '-lfontconfig',
                 '-lfreetype',
                 '-lGL',
                 '-lglut',
                 '-lGLU',
                 '-lpng',
                 '-lm',
                 '-lc' ],
              'ldflags': [
                '-Wall',
                # TODO(gram): handle release mode.
                '-Lthird_party/skia/trunk/out/Debug',
              ],
             },
           }
         ],
         ['OS=="mac"',
           {
             'ldflags': [
               '-framework OpenGL',
               '-framework GLUT',
               '-L /usr/X11/lib'
             ],
           }
        ]
      ]
    }
  ]
}

