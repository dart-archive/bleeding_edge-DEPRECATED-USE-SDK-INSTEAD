import unittest

from scss.parser import Stylesheet


class TestSCSS( unittest.TestCase ):

    def setUp(self):
        self.parser = Stylesheet(options=dict(compress=True))

    def test_for(self):
        src = """
            @mixin test($src:2px){
                $width: $src + 5px;
                width: $width;
            }
            .test {
                color: blue;
                @for $i from 1 through 4 {
                    .span-#{$i}{
                        @include test($i); }
                }
            }

            @for $i from 1 through 2 {
                .span-#{$i}{
                    color: red; }
            }
        """
        test = ".test{color:#00f}.test .span-1{width:6px}.test .span-2{width:7px}.test .span-3{width:8px}.test .span-4{width:9px}.span-1{color:#f00}.span-2{color:#f00}"
        out = self.parser.loads(src)
        self.assertEqual(test, out)
