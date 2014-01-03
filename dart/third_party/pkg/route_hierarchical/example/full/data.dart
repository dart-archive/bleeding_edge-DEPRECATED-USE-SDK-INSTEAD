/**
 * Mock implementation of the data service library.
 */
library dataservice;

import 'dart:async';

// List of fake companies
List _companies = [
  {
    'id': 100001,
    'name': 'Company 100001',
    'revenue': 3000000.00
  },
  {
    'id': 111111,
    'name': 'Company 1111111',
    'revenue': 1001100.00
  },
  {
    'id': 100003,
    'name': 'Mom & Pop Shop',
    'revenue': 201000.00
  },
  {
    'id': 333333,
    'name': 'Sunny & Bob',
    'revenue': 301000.00
  },
  {
    'id': 444444,
    'name': 'Sun In the Sky Inc.',
    'revenue': 5001000.00
  },
  {
    'id': 555555,
    'name': 'Sunny Java',
    'revenue': 401000.00
  }
];

/// simulate an RPC call to fetch companies
Future<List<Map>> fetchCompanies(String query) =>
    new Future.delayed(new Duration(milliseconds: 500), () =>
        _companies.where((company) =>
            company['name'].toLowerCase().indexOf(query.toLowerCase()) > -1));

/// simulate an RPC call to fetch a company
Future<Map> fetchCompany(int id) =>
     new Future.delayed(new Duration(seconds: 1), () =>
          _companies.firstWhere((c) => c['id'] == id, orElse: () => null));
