spawn = require('child_process').spawn

DART_VM = process.env.DART_VM or '../dart-sdk/bin/dart'

module.exports = (grunt) ->
  grunt.initConfig
    watch:
      tests:
        files: ['test/**/*.dart', 'lib/**/*.dart']
        tasks: ['dart']
    dart:
      tests:
        entry: 'test/main.dart'

  grunt.registerMultiTask 'dart', 'run dart program', ->
    spawn(DART_VM, [@data.entry], {stdio: 'inherit'}).on 'close', @async()

  grunt.loadNpmTasks 'grunt-contrib-watch'
