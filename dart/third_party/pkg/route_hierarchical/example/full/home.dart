library portfolio;

import 'package:web_ui/web_ui.dart';
import 'package:route_hierarchical/client.dart';

import 'index.dart';

class HomeComponent extends WebComponent {
  var links = <String, Map<String, String>>{};

  created() {
    ['100001', '111111'].forEach((companyId) {
      links[companyId] = {};
      ['info', 'activities', 'notes'].forEach((mode) =>
          links[companyId][mode] = router.url('companyInfo.companyId.' + mode,
              parameters: {
                'companyId': companyId
              }));
    });
  }
}