import 'dart:async';
import 'dart:io';
import 'package:hop/hop.dart';
import 'package:hop/hop_tasks.dart';
import '../test/console_test_harness.dart' as test_console;

void main(List<String> args) {

  addTask('test', createUnitTestTask(test_console.testCore));

  //
  // Analyzer
  //
  addTask('analyze_libs', createAnalyzerTask(_getLibs));

  //
  // Dart2js
  //
  addTask('dart2js', createDartCompilerTask(['web/game_web.dart'],
      minify: true, liveTypeAnalysis: true));

  //
  // Dart2js - App
  //
  addTask('app_dart2js', createDartCompilerTask(['app_package/game_app.dart'],
      minify: true,
      liveTypeAnalysis: true
  ));

  addTask('update_js', createCopyJSTask('web',
      browserDart: true,
      browserInterop: true));

  addTask('app_update_js', createCopyJSTask('app_package',
      browserDart: true,
      browserInterop: true));

  //
  // gh_pages
  //
  addAsyncTask('pages', (ctx) =>
      branchForDir(ctx, 'master', 'web', 'gh-pages'));

  runHop(args);
}


Future<List<String>> _getLibs() {
  return new Directory('lib').list()
      .where((FileSystemEntity fse) => fse is File)
      .map((File file) => file.path)
      .toList();
}
