library test_files.main;

import 'package:angular/core/module.dart';

@NgDirective(
    children: NgAnnotation.TRANSCLUDE_CHILDREN,
    selector:'[ng-if]',
    map: const {'.': '=>ngIfCondition'})
class NgIfDirective {
  bool ngIfCondition;
}

@NgComponent(
    selector: 'my-component',
    map: const {
      'attr': '@attr',
      'expr': '=>expr'
    },
    template: '<div>{{ctrl.inline.template.expression}}</div>',
    exportExpressionAttrs: const ['exported-attr'],
    exportExpressions: const ['exported + expression'])
class MyComponent {
  @NgOneWay('another-expression')
  String anotherExpression;

  @NgCallback('callback')
  set callback(Function) {}

  set twoWayStuff(String abc) {}
  @NgTwoWay('two-way-stuff')
  String get twoWayStuff => null;
}

class CssUrlsString {

}

class CssUrlsList {

}
