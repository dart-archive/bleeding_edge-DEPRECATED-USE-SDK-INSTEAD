library ppw_test;

import 'dart:math';
import 'package:bot/bot.dart';
import 'package:poppopwin/poppopwin.dart';
import 'package:unittest/unittest.dart';

part 'test_field.dart';
part 'test_game.dart';

runppwTests() {
  group('ppw', (){
    TestField.run();
    TestGame.run();
  });
}
