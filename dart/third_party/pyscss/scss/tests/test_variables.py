import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet(options=dict(compress=True))

    def test_variables(self):
        src = """
            @vars {
                $blue: #ffdd00 !default;
                $test: rgb(120, 35, 64);
                $test2: rgba(120, 35, 64, .4);
                $property: float;
                $value: left;
                $len: 0px or 5px;
            }
            $margin: 16px;
            $side: top;
            $image: 'test.png';

            .content-navigation {
                #{$property}: #{$value};
                border-color: $blue;
                background-color: $test + 5%;


                background-image: url('/test/' + $image);
                display: -moz-inline-box;
                color: $blue - 9%;
                margin: $len (-$margin * 2 ) -12px;
            }

            .border {
                padding-#{$side}: $margin / 2;
                margin: $margin / 2;
                padding-left: -$margin + 2px;
                border-#{$side}: {
                    color:  $blue;
                }
                color: $test2;
                font: -1.5em + 50px;
            }
            """
        test = ".content-navigation{float:left;display:-moz-inline-box;margin:5px -32px -12px;border-color:#fd0;background-color:#7b1f3e;background-image:url(/test/test.png);color:#f3d40b}.border{margin:8px;padding-top:8px;padding-left:-14px;border-top-color:#fd0;color:rgba(120,35,64,0.40);font:30.5px}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)
