library engine.test;

import "scanner_test.dart" as t_scanner;
import "parser_test.dart" as t_parser;
import "ast_test.dart" as t_ast;
import "element_test.dart" as t_element;

main() {
  t_scanner.main();
  t_parser.main();
  t_ast.main();
  t_element.main();
}
