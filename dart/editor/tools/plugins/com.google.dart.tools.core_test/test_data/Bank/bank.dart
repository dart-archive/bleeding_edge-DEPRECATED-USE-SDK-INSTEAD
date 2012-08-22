#import('dart:html');
#import('../Money/money.dart');
#import('package:customer/customer.dart');

Customer customer;
SimpleMoney balance;

void main() {
  customer = new Customer();
  balance = new SimpleMoney(0, Currency.USD);
  displayBalance();
  query("#button").text = "[Deposit money]";
  query("#button").on.click.add(makeDeposit);
}

void makeDeposit(Event event) {
  balance = balance + new SimpleMoney(100, balance.getCurrency());
  displayBalance();
}

void displayBalance() {
  query("#balance").text = "${balance.getAmount()} ${balance.getCurrency()}";
}
