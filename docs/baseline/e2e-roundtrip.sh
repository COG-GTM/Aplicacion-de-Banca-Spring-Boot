#!/usr/bin/env bash
# End-to-end smoke test for BankApp: customer create -> two accounts -> transfer -> verify.
# Baseline captured on JDK 11 / Spring Boot 2.7.18 (ticket UNT2-1). Expected statuses/bodies
# are the regression checks for the Java 21 / Spring Boot 4 migration.
#
# Usage: start the app (java -jar target/bank-app-1.0.0.jar), then ./docs/baseline/e2e-roundtrip.sh
set -u
BASE="${BASE:-http://localhost:8989/bank-api}"
CUST=${CUST:-1001}
ACC1=${ACC1:-5001}
ACC2=${ACC2:-5002}
fail=0

check() { # name expected_status actual_status body
  if [ "$2" = "$3" ]; then echo "PASS $1 -> HTTP $3 $4"; else echo "FAIL $1 -> expected HTTP $2, got $3 $4"; fail=1; fi
}
req() { # method path [json]
  if [ $# -ge 3 ]; then
    curl -s -o /tmp/e2e.body -w '%{http_code}' -X "$1" "$BASE$2" -H 'Content-Type: application/json' -d "$3"
  else
    curl -s -o /tmp/e2e.body -w '%{http_code}' -X "$1" "$BASE$2"
  fi
}

echo "== Swagger UI / OpenAPI / H2 console"
check "GET /swagger-ui/index.html"  200 "$(req GET /swagger-ui/index.html)" ""
check "GET /v3/api-docs"            200 "$(req GET /v3/api-docs)" ""
check "GET /h2-console/"            200 "$(req GET /h2-console/)" ""
check "GET /actuator/health"        200 "$(req GET /actuator/health)" "$(cat /tmp/e2e.body)"

echo "== Customer create"
s=$(req POST /customers/add "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"middleName\":\"K\",\"customerNumber\":$CUST,\"status\":\"Active\",\"customerAddress\":{\"address1\":\"1 Main St\",\"address2\":\"\",\"city\":\"London\",\"state\":\"LDN\",\"zip\":\"E1\",\"country\":\"UK\"},\"contactDetails\":{\"emailId\":\"ada@example.com\",\"homePhone\":\"111\",\"workPhone\":\"222\"}}")
check "POST /customers/add" 201 "$s" "$(cat /tmp/e2e.body)"   # body: "New Customer created successfully."
s=$(req GET /customers/$CUST); check "GET /customers/$CUST" 200 "$s" "$(cat /tmp/e2e.body)"
s=$(req GET /customers/all);   check "GET /customers/all"   200 "$s" ""

echo "== Account create (x2)"
acct() { echo "{\"accountNumber\":$1,\"bankInformation\":{\"branchName\":\"Main\",\"branchCode\":10,\"routingNumber\":123456,\"branchAddress\":{\"address1\":\"2 Bank St\",\"address2\":\"\",\"city\":\"London\",\"state\":\"LDN\",\"zip\":\"E2\",\"country\":\"UK\"}},\"accountStatus\":\"Active\",\"accountType\":\"Savings\",\"accountBalance\":$2}"; }
s=$(req POST /accounts/add/$CUST "$(acct $ACC1 1000.0)"); check "POST /accounts/add/$CUST (acct $ACC1)" 201 "$s" "$(cat /tmp/e2e.body)"  # "New Account created successfully."
s=$(req POST /accounts/add/$CUST "$(acct $ACC2 100.0)");  check "POST /accounts/add/$CUST (acct $ACC2)" 201 "$s" "$(cat /tmp/e2e.body)"
s=$(req GET /accounts/$ACC1); check "GET /accounts/$ACC1" 302 "$s" "$(cat /tmp/e2e.body)"   # NOTE: app returns HTTP 302 FOUND with JSON body

echo "== Transfer 250.0 from $ACC1 to $ACC2"
s=$(req PUT /accounts/transfer/$CUST "{\"fromAccountNumber\":$ACC1,\"toAccountNumber\":$ACC2,\"transferAmount\":250.0}")
check "PUT /accounts/transfer/$CUST" 200 "$s" "$(cat /tmp/e2e.body)"   # "Success: Amount transferred."
s=$(req GET /accounts/$ACC1); b1=$(cat /tmp/e2e.body); check "GET /accounts/$ACC1 after" 302 "$s" "$b1"
s=$(req GET /accounts/$ACC2); b2=$(cat /tmp/e2e.body); check "GET /accounts/$ACC2 after" 302 "$s" "$b2"
echo "$b1" | grep -q '"accountBalance":750.0' && echo "PASS balance $ACC1 == 750.0" || { echo "FAIL balance $ACC1 != 750.0"; fail=1; }
echo "$b2" | grep -q '"accountBalance":350.0' && echo "PASS balance $ACC2 == 350.0" || { echo "FAIL balance $ACC2 != 350.0"; fail=1; }
s=$(req GET /accounts/transactions/$ACC1); check "GET /accounts/transactions/$ACC1" 200 "$s" "$(cat /tmp/e2e.body)"

echo "== Insufficient funds (negative case)"
s=$(req PUT /accounts/transfer/$CUST "{\"fromAccountNumber\":$ACC2,\"toAccountNumber\":$ACC1,\"transferAmount\":99999.0}")
check "PUT /accounts/transfer (insufficient)" 400 "$s" "$(cat /tmp/e2e.body)"   # "Insufficient Funds."

[ $fail -eq 0 ] && echo "ALL PASS" || { echo "SOME CHECKS FAILED"; exit 1; }
