library engine.test;

import "scanner_test.dart" as t_scanner;
import "parser_test.dart" as t_parser;
import "ast_test.dart" as t_ast;

main() {
  t_scanner.main();
  t_parser.main();
  t_ast.main();
}
