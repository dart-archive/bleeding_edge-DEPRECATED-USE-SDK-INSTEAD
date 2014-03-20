library pop_pop_win.textures;

import 'dart:html';
import 'package:bot_web/bot_texture.dart';

Map<String, TextureInput> getTextures(ImageElement transparentElement,
    ImageElement opaqueElement, ImageElement transparentStaticElement) {

  var frames = <String, TextureInput>{};

  _getTransparentItems().forEach((String key, Map<String, dynamic> value) {
    frames[key] = new TextureInput.fromHash(key, value, transparentElement);
  });

  _getOpaqueItems().forEach((String key, Map<String, dynamic> value) {
    frames[key] = new TextureInput.fromHash(key, value, opaqueElement);
  });

  _getTransparentStaticItems().forEach((String key, Map<String, dynamic> value) {
    frames[key] = new TextureInput.fromHash(key, value, transparentStaticElement);
  });

  return frames;
}

Map _getTransparentStaticItems() => {

"button_new_game.png":
{
  "frame": {"x":398,"y":150,"w":294,"h":94},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":294,"h":94},
  "sourceSize": {"w":294,"h":94}
},
"button_new_game_clicked.png":
{
  "frame": {"x":504,"y":0,"w":292,"h":94},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":292,"h":94},
  "sourceSize": {"w":292,"h":94}
},
"logo_win.png":
{
  "frame": {"x":0,"y":88,"w":318,"h":96},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":318,"h":96},
  "sourceSize": {"w":318,"h":96}
}};


Map _getOpaqueItems() => {
"background_side_left.png":
{
  "frame": {"x":0,"y":96,"w":352,"h":672},
  "rotated": true,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":352,"h":672},
  "sourceSize": {"w":352,"h":672}
},
"background_top_left.png":
{
  "frame": {"x":0,"y":0,"w":1024,"h":96},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":1024,"h":96},
  "sourceSize": {"w":1024,"h":96}
},
"balloon.png":
{
  "frame": {"x":1184,"y":352,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"balloon_pieces_a.png":
{
  "frame": {"x":1184,"y":272,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"balloon_pieces_b.png":
{
  "frame": {"x":1184,"y":192,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"balloon_pieces_c.png":
{
  "frame": {"x":1104,"y":352,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"balloon_pieces_d.png":
{
  "frame": {"x":1024,"y":304,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"balloon_tagged_!.png":
{
  "frame": {"x":832,"y":368,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"balloon_tagged_bomb.png":
{
  "frame": {"x":944,"y":304,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"balloon_tagged_frozen.png":
{
  "frame": {"x":1104,"y":272,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"crater_b.png":
{
  "frame": {"x":1136,"y":112,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"game_board_center.png":
{
  "frame": {"x":1104,"y":192,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"game_board_corner_bottom_left.png":
{
  "frame": {"x":784,"y":96,"w":112,"h":112},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":112,"h":112},
  "sourceSize": {"w":112,"h":112}
},
"game_board_corner_bottom_right.png":
{
  "frame": {"x":672,"y":96,"w":112,"h":112},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":112,"h":112},
  "sourceSize": {"w":112,"h":112}
},
"game_board_corner_top_left.png":
{
  "frame": {"x":1136,"y":0,"w":112,"h":112},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":112,"h":112},
  "sourceSize": {"w":112,"h":112}
},
"game_board_corner_top_right.png":
{
  "frame": {"x":1024,"y":0,"w":112,"h":112},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":112,"h":112},
  "sourceSize": {"w":112,"h":112}
},
"game_board_side_bottom.png":
{
  "frame": {"x":976,"y":112,"w":80,"h":112},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":112},
  "sourceSize": {"w":80,"h":112}
},
"game_board_side_left.png":
{
  "frame": {"x":784,"y":208,"w":112,"h":80},
  "rotated": true,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":112,"h":80},
  "sourceSize": {"w":112,"h":80}
},
"game_board_side_right.png":
{
  "frame": {"x":672,"y":208,"w":112,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":112,"h":80},
  "sourceSize": {"w":112,"h":80}
},
"game_board_side_top.png":
{
  "frame": {"x":896,"y":96,"w":80,"h":112},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":112},
  "sourceSize": {"w":80,"h":112}
},
"number_eight.png":
{
  "frame": {"x":1056,"y":112,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"number_five.png":
{
  "frame": {"x":1024,"y":224,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"number_four.png":
{
  "frame": {"x":672,"y":368,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"number_one.png":
{
  "frame": {"x":752,"y":320,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"number_seven.png":
{
  "frame": {"x":864,"y":288,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"number_six.png":
{
  "frame": {"x":944,"y":224,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"number_three.png":
{
  "frame": {"x":864,"y":208,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
},
"number_two.png":
{
  "frame": {"x":672,"y":288,"w":80,"h":80},
  "rotated": false,
  "trimmed": false,
  "spriteSourceSize": {"x":0,"y":0,"w":80,"h":80},
  "sourceSize": {"w":80,"h":80}
}};


Map _getTransparentItems() => {

"balloon_explode_0000.png":
{
  "frame": {"x":1762,"y":1058,"w":80,"h":86},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":89,"y":87,"w":80,"h":86},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0001.png":
{
  "frame": {"x":446,"y":1332,"w":208,"h":208},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":35,"y":13,"w":208,"h":208},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0002.png":
{
  "frame": {"x":868,"y":612,"w":230,"h":230},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":13,"y":13,"w":230,"h":230},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0003.png":
{
  "frame": {"x":1128,"y":840,"w":226,"h":222},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":18,"y":12,"w":226,"h":222},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0004.png":
{
  "frame": {"x":1072,"y":1074,"w":228,"h":218},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":16,"y":16,"w":228,"h":218},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0005.png":
{
  "frame": {"x":1100,"y":610,"w":228,"h":218},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":16,"y":15,"w":228,"h":218},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0006.png":
{
  "frame": {"x":908,"y":844,"w":228,"h":218},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":15,"y":15,"w":228,"h":218},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0007.png":
{
  "frame": {"x":1362,"y":234,"w":224,"h":218},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":15,"y":14,"w":224,"h":218},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0008.png":
{
  "frame": {"x":1320,"y":608,"w":224,"h":218},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":15,"y":14,"w":224,"h":218},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0009.png":
{
  "frame": {"x":656,"y":1314,"w":226,"h":224},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":13,"y":14,"w":226,"h":224},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0010.png":
{
  "frame": {"x":842,"y":1078,"w":228,"h":230},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":12,"y":13,"w":228,"h":230},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0011.png":
{
  "frame": {"x":1298,"y":2,"w":228,"h":230},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":12,"y":13,"w":228,"h":230},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0012.png":
{
  "frame": {"x":1132,"y":236,"w":228,"h":230},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":12,"y":13,"w":228,"h":230},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0013.png":
{
  "frame": {"x":676,"y":846,"w":230,"h":230},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":11,"y":13,"w":230,"h":230},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0014.png":
{
  "frame": {"x":1066,"y":2,"w":230,"h":232},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":11,"y":11,"w":230,"h":232},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0015.png":
{
  "frame": {"x":900,"y":236,"w":230,"h":232},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":11,"y":12,"w":230,"h":232},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0016.png":
{
  "frame": {"x":834,"y":2,"w":230,"h":232},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":12,"y":12,"w":230,"h":232},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0017.png":
{
  "frame": {"x":666,"y":244,"w":232,"h":232},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":12,"y":12,"w":232,"h":232},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0018.png":
{
  "frame": {"x":634,"y":612,"w":232,"h":232},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":11,"y":12,"w":232,"h":232},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0019.png":
{
  "frame": {"x":442,"y":846,"w":232,"h":232},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":12,"y":12,"w":232,"h":232},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0020.png":
{
  "frame": {"x":408,"y":1098,"w":232,"h":232},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":12,"y":11,"w":232,"h":232},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0021.png":
{
  "frame": {"x":400,"y":612,"w":232,"h":232},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":12,"y":12,"w":232,"h":232},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0022.png":
{
  "frame": {"x":208,"y":864,"w":232,"h":232},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":12,"y":11,"w":232,"h":232},
  "sourceSize": {"w":256,"h":256}
},
"balloon_explode_0023.png":
{
  "frame": {"x":882,"y":1310,"w":230,"h":228},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":12,"y":12,"w":230,"h":228},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0000.png":
{
  "frame": {"x":1236,"y":1304,"w":142,"h":122},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":60,"y":62,"w":142,"h":122},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0001.png":
{
  "frame": {"x":642,"y":1080,"w":232,"h":198},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":15,"y":23,"w":232,"h":198},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0002.png":
{
  "frame": {"x":206,"y":1144,"w":234,"h":200},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":14,"y":22,"w":234,"h":200},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0003.png":
{
  "frame": {"x":2,"y":1144,"w":238,"h":202},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":11,"y":22,"w":238,"h":202},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0004.png":
{
  "frame": {"x":628,"y":2,"w":240,"h":204},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":10,"y":21,"w":240,"h":204},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0005.png":
{
  "frame": {"x":458,"y":248,"w":242,"h":206},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":9,"y":20,"w":242,"h":206},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0006.png":
{
  "frame": {"x":422,"y":2,"w":244,"h":204},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":8,"y":22,"w":244,"h":204},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0007.png":
{
  "frame": {"x":250,"y":284,"w":248,"h":206},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":5,"y":21,"w":248,"h":206},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0008.png":
{
  "frame": {"x":2,"y":892,"w":250,"h":204},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":4,"y":23,"w":250,"h":204},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0009.png":
{
  "frame": {"x":196,"y":612,"w":250,"h":202},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":4,"y":25,"w":250,"h":202},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0010.png":
{
  "frame": {"x":1292,"y":1212,"w":88,"h":110},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":82,"y":78,"w":88,"h":110},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0011.png":
{
  "frame": {"x":1544,"y":948,"w":92,"h":116},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":80,"y":75,"w":92,"h":116},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0012.png":
{
  "frame": {"x":1478,"y":1440,"w":98,"h":122},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":77,"y":72,"w":98,"h":122},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0013.png":
{
  "frame": {"x":1730,"y":1424,"w":104,"h":128},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":73,"y":69,"w":104,"h":128},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0014.png":
{
  "frame": {"x":1726,"y":948,"w":108,"h":134},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":71,"y":66,"w":108,"h":134},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0015.png":
{
  "frame": {"x":924,"y":470,"w":114,"h":140},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":67,"y":63,"w":114,"h":140},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0016.png":
{
  "frame": {"x":506,"y":492,"w":118,"h":144},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":65,"y":62,"w":118,"h":144},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0017.png":
{
  "frame": {"x":1112,"y":1304,"w":122,"h":150},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":63,"y":58,"w":122,"h":150},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0018.png":
{
  "frame": {"x":2,"y":1384,"w":128,"h":154},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":59,"y":56,"w":128,"h":154},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0019.png":
{
  "frame": {"x":764,"y":478,"w":132,"h":158},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":57,"y":54,"w":132,"h":158},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0020.png":
{
  "frame": {"x":308,"y":1380,"w":136,"h":160},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":54,"y":53,"w":136,"h":160},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0021.png":
{
  "frame": {"x":1040,"y":470,"w":138,"h":164},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":53,"y":51,"w":138,"h":164},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0022.png":
{
  "frame": {"x":1832,"y":2,"w":142,"h":168},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":51,"y":49,"w":142,"h":168},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0023.png":
{
  "frame": {"x":1608,"y":610,"w":146,"h":170},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":48,"y":48,"w":146,"h":170},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0024.png":
{
  "frame": {"x":1438,"y":460,"w":146,"h":172},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":48,"y":47,"w":146,"h":172},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0025.png":
{
  "frame": {"x":1794,"y":458,"w":148,"h":174},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":47,"y":46,"w":148,"h":174},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0026.png":
{
  "frame": {"x":132,"y":1384,"w":150,"h":174},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":45,"y":46,"w":150,"h":174},
  "sourceSize": {"w":256,"h":256}
},
"balloon_pop_0027.png":
{
  "frame": {"x":1794,"y":608,"w":148,"h":172},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":46,"y":46,"w":148,"h":172},
  "sourceSize": {"w":256,"h":256}
},
"dart_fly_0000.png":
{
  "frame": {"x":1796,"y":330,"w":126,"h":194},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":869,"y":486,"w":126,"h":194},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0001.png":
{
  "frame": {"x":2,"y":2,"w":210,"h":320},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":794,"y":385,"w":210,"h":320},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0002.png":
{
  "frame": {"x":2,"y":324,"w":246,"h":286},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":736,"y":317,"w":246,"h":286},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0003.png":
{
  "frame": {"x":1582,"y":228,"w":212,"h":220},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":689,"y":271,"w":212,"h":220},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0004.png":
{
  "frame": {"x":1832,"y":146,"w":182,"h":166},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":653,"y":244,"w":182,"h":166},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0005.png":
{
  "frame": {"x":1352,"y":834,"w":162,"h":120},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":621,"y":231,"w":162,"h":120},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0006.png":
{
  "frame": {"x":1292,"y":1068,"w":142,"h":98},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":597,"y":211,"w":142,"h":98},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0007.png":
{
  "frame": {"x":1250,"y":1448,"w":126,"h":92},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":576,"y":189,"w":126,"h":92},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0008.png":
{
  "frame": {"x":1638,"y":948,"w":112,"h":86},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":558,"y":179,"w":112,"h":86},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0009.png":
{
  "frame": {"x":1518,"y":1206,"w":98,"h":94},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":545,"y":177,"w":98,"h":94},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0010.png":
{
  "frame": {"x":1456,"y":956,"w":86,"h":110},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":534,"y":185,"w":86,"h":110},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0011.png":
{
  "frame": {"x":1860,"y":1420,"w":76,"h":124},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":524,"y":199,"w":76,"h":124},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0012.png":
{
  "frame": {"x":1540,"y":608,"w":66,"h":134},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":517,"y":221,"w":66,"h":134},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0013.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0014.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0015.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0016.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0017.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0018.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0019.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0020.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0021.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0022.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0023.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0024.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0025.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0026.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0027.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0028.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0029.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0030.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0031.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0032.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0033.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0034.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0035.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0036.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0037.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0038.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0039.png":
{
  "frame": {"x":1850,"y":1080,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0040.png":
{
  "frame": {"x":1930,"y":758,"w":64,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0041.png":
{
  "frame": {"x":1862,"y":1014,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0042.png":
{
  "frame": {"x":1862,"y":948,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0043.png":
{
  "frame": {"x":1792,"y":882,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0044.png":
{
  "frame": {"x":1696,"y":1062,"w":64,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0045.png":
{
  "frame": {"x":1654,"y":882,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0046.png":
{
  "frame": {"x":1630,"y":1066,"w":64,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0047.png":
{
  "frame": {"x":1564,"y":1066,"w":64,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0048.png":
{
  "frame": {"x":1578,"y":1302,"w":64,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0049.png":
{
  "frame": {"x":1512,"y":1302,"w":64,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0050.png":
{
  "frame": {"x":1446,"y":1302,"w":64,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0051.png":
{
  "frame": {"x":1516,"y":882,"w":64,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0052.png":
{
  "frame": {"x":1540,"y":744,"w":64,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":505,"y":248,"w":64,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0053.png":
{
  "frame": {"x":1844,"y":1146,"w":62,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":506,"y":248,"w":62,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_0054.png":
{
  "frame": {"x":214,"y":287,"w":1,"h":1},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":0,"y":0,"w":1,"h":1},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0000.png":
{
  "frame": {"x":1644,"y":1292,"w":102,"h":130},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":390,"y":512,"w":102,"h":130},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0001.png":
{
  "frame": {"x":1704,"y":2,"w":126,"h":216},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":378,"y":424,"w":126,"h":216},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0002.png":
{
  "frame": {"x":214,"y":2,"w":206,"h":280},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":338,"y":359,"w":206,"h":280},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0003.png":
{
  "frame": {"x":2,"y":612,"w":192,"h":278},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":340,"y":312,"w":192,"h":278},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0004.png":
{
  "frame": {"x":1528,"y":2,"w":174,"h":224},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":347,"y":283,"w":174,"h":224},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0005.png":
{
  "frame": {"x":1612,"y":450,"w":158,"h":180},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":356,"y":265,"w":158,"h":180},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0006.png":
{
  "frame": {"x":1206,"y":468,"w":144,"h":138},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":365,"y":257,"w":144,"h":138},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0007.png":
{
  "frame": {"x":1606,"y":758,"w":130,"h":122},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":376,"y":235,"w":130,"h":122},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0008.png":
{
  "frame": {"x":652,"y":492,"w":118,"h":110},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":387,"y":221,"w":118,"h":110},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0009.png":
{
  "frame": {"x":1352,"y":956,"w":110,"h":102},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":394,"y":212,"w":110,"h":102},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0010.png":
{
  "frame": {"x":1378,"y":1440,"w":100,"h":98},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":406,"y":215,"w":100,"h":98},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0011.png":
{
  "frame": {"x":1404,"y":1206,"w":94,"h":112},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":416,"y":222,"w":94,"h":112},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0012.png":
{
  "frame": {"x":1618,"y":1204,"w":86,"h":122},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":428,"y":237,"w":86,"h":122},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0013.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0014.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0015.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0016.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0017.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0018.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0019.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0020.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0021.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0022.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0023.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0024.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0025.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0026.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0027.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0028.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0029.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0030.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0031.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0032.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0033.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0034.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0035.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0036.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0037.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0038.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0039.png":
{
  "frame": {"x":1478,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0040.png":
{
  "frame": {"x":1360,"y":1302,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0041.png":
{
  "frame": {"x":1392,"y":1068,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0042.png":
{
  "frame": {"x":1112,"y":1456,"w":84,"h":136},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0043.png":
{
  "frame": {"x":1352,"y":468,"w":84,"h":136},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":84,"h":136},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0044.png":
{
  "frame": {"x":1830,"y":1292,"w":80,"h":126},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":442,"y":261,"w":80,"h":126},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0045.png":
{
  "frame": {"x":1748,"y":1292,"w":80,"h":126},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":442,"y":261,"w":80,"h":126},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0046.png":
{
  "frame": {"x":1860,"y":1210,"w":80,"h":126},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":442,"y":261,"w":80,"h":126},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0047.png":
{
  "frame": {"x":1602,"y":1440,"w":80,"h":126},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":442,"y":261,"w":80,"h":126},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0048.png":
{
  "frame": {"x":1762,"y":1140,"w":80,"h":126},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":442,"y":261,"w":80,"h":126},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0049.png":
{
  "frame": {"x":1912,"y":1292,"w":76,"h":126},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":76,"h":126},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0050.png":
{
  "frame": {"x":378,"y":534,"w":76,"h":126},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":76,"h":126},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0051.png":
{
  "frame": {"x":250,"y":534,"w":76,"h":126},
  "rotated": true,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":261,"w":76,"h":126},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0052.png":
{
  "frame": {"x":1738,"y":758,"w":76,"h":122},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":443,"y":262,"w":76,"h":122},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0053.png":
{
  "frame": {"x":1816,"y":758,"w":74,"h":120},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":444,"y":264,"w":74,"h":120},
  "sourceSize": {"w":1024,"h":768}
},
"dart_fly_shadow_0054.png":
{
  "frame": {"x":214,"y":284,"w":1,"h":1},
  "rotated": false,
  "trimmed": true,
  "spriteSourceSize": {"x":0,"y":0,"w":1,"h":1},
  "sourceSize": {"w":1024,"h":768}
}};
