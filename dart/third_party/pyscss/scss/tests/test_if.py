import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet(options=dict(compress=True))

    def test_if(self):
        src = """
            $type: monster;
            $test: 9px;

            $rec: true;
            $rec2: $rec or true;
            $rec3: $rec or true;
            $rec: $rec2 or $rec3;

            @if $test + 2 > 10 {
                @if $rec {
                    .test { border: 2px; }
                }
            }

            @mixin test($fix: true) {
                @if $fix {
                    display: block;
                } @else {
                    display: none;
                }
            }
            span {
                @include test(false)
            }
            p {
                @if $type == girl {
                    color: pink;
                }
                @else if $type == monster {
                    color: red;
                    b { border: 2px; }
                }
                @else {
                    color: blue;
                }
            }
        """
        test = ".test{border:2px}span{display:none}p{color:#f00}p b{border:2px}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)
