#!/bin/bash
# 상품 하드 삭제 검증. 깨끗한 H2 기동 직후 1회 실행 전제. 페이로드는 ASCII.
B=http://localhost:8099/api/v1

pass=0; fail=0
chk(){ if [ "$2" == "$3" ]; then echo "  PASS  $1"; pass=$((pass+1)); else echo "  FAIL  $1 | expected=[$2] actual=[$3]"; fail=$((fail+1)); fi; }
J(){ node -e 'let s="";process.stdin.on("data",c=>s+=c).on("end",()=>{const d=JSON.parse(s);console.log(eval(process.argv[1]))})' "$1"; }

echo "=== 0. 계정/업체 준비 ==="
curl -s -o /dev/null -X POST "$B/auth/sign-up" -H 'Content-Type: application/json' \
  -d '{"username":"admin@cdy.com","password":"123456","nickname":"admin","userCategory":"CODING","name":"Admin","phoneNumber":"01000000000"}'
curl -s -o /dev/null -X POST "$B/auth/sign-up" -H 'Content-Type: application/json' \
  -d '{"username":"crew@cdy.com","password":"123456","nickname":"crew","userCategory":"CODING","name":"Crew","phoneNumber":"01011111111"}'
curl -s -o /dev/null -X POST "$B/admin/bootstrap" -H 'Content-Type: application/json' \
  -d '{"username":"admin@cdy.com","secretKey":"cdy-admin-bootstrap-2026"}'
login(){ curl -s -X POST "$B/auth/login" -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"123456\"}" | tr -d '\r\n'; }
AT=$(login admin@cdy.com); CT=$(login crew@cdy.com)
AH="Authorization: Bearer $AT"; CH="Authorization: Bearer $CT"; JH='Content-Type: application/json'

curl -s -o /dev/null -X POST "$B/admin/partners" -H "$AH" -H "$JH" \
  -d '{"name":"Solo Shop","category":"CAFE","status":"ACTIVE","sortOrder":1}'
P=$(curl -s "$B/admin/partners" -H "$AH" | J "d.find(p=>p.name==='Solo Shop').id")
echo "  partner=$P"

mkprod(){ curl -s -o /dev/null -X POST "$B/admin/shop/products" -H "$AH" -H "$JH" -d "$1"; }
pid(){ curl -s "$B/admin/shop/products" -H "$AH" | J "d.find(i=>i.name==='$1').id"; }

echo
echo "=== 1. 미주문 상품 (상세 이미지 + 장바구니에 담긴 상태) 하드 삭제 ==="
# imageKeys 를 넣어 product_images 까지 cascade 로 지워지는지 본다.
# 이미지가 안 지워지면 FK 제약에 걸려 삭제가 실패한다.
mkprod "{\"name\":\"FreeDelete\",\"partnerId\":$P,\"originalPrice\":10000,\"crewPrice\":7000,\"stock\":5,\"imageKeys\":[\"k1\",\"k2\"]}"
FD=$(pid FreeDelete)
chk "이미지 2장이 등록됨" "2" "$(curl -s "$B/admin/shop/products" -H "$AH" | J "d.find(i=>i.id===$FD).imageKeys.length")"

# 크루가 장바구니에 담아둔 상태로 만든다
curl -s -o /dev/null -X POST "$B/shop/cart" -H "$CH" -H "$JH" -d "{\"productId\":$FD,\"quantity\":2}"
chk "장바구니에 담긴 상태" "1" "$(curl -s "$B/shop/cart/count" -H "$CH" | J "d.count")"

code=$(curl -s -o /tmp/del.txt -w '%{http_code}' -X DELETE "$B/admin/shop/products/$FD" -H "$AH")
chk "삭제 요청 200" "200" "$code"
chk "어드민 목록에서 완전 소멸" "false" "$(curl -s "$B/admin/shop/products" -H "$AH" | J "d.some(i=>i.name==='FreeDelete')")"
chk "크루 목록에서 소멸" "false" "$(curl -s "$B/shop/products" -H "$CH" | J "d.items.some(i=>i.name==='FreeDelete')")"
chk "상세 404" "404" "$(curl -s -o /dev/null -w '%{http_code}' "$B/shop/products/$FD" -H "$CH")"
chk "장바구니도 비워짐 (FK 정리)" "0" "$(curl -s "$B/shop/cart/count" -H "$CH" | J "d.count")"

echo
echo "=== 2. 주문 이력이 있는 상품은 400 ==="
mkprod "{\"name\":\"Ordered\",\"partnerId\":$P,\"originalPrice\":20000,\"crewPrice\":10000,\"stock\":5}"
OD=$(pid Ordered)
curl -s -o /dev/null -X POST "$B/shop/orders" -H "$CH" -H "$JH" \
  -d "{\"items\":[{\"productId\":$OD,\"quantity\":1}],\"receiverName\":\"Crew\",\"receiverPhone\":\"01011111111\",\"postcode\":\"12345\",\"address\":\"Seoul\",\"addressDetail\":\"101\",\"deliveryMemo\":\"none\"}"
code=$(curl -s -o /tmp/del2.txt -w '%{http_code}' -X DELETE "$B/admin/shop/products/$OD" -H "$AH")
chk "삭제 거부 400" "400" "$code"
chk "안내 문구" "주문 이력이 있는 상품은 삭제할 수 없어요. 숨김 처리를 사용해주세요." "$(cat /tmp/del2.txt | J "d.messages")"
chk "상품은 그대로 남아 있음" "true" "$(curl -s "$B/admin/shop/products" -H "$AH" | J "d.some(i=>i.name==='Ordered')")"
chk "상태도 ACTIVE 유지 (숨김되지 않음)" "ACTIVE" "$(curl -s "$B/admin/shop/products" -H "$AH" | J "d.find(i=>i.name==='Ordered').status")"

echo
echo "=== 3. 숨김 기능은 그대로 동작 (PUT status=HIDDEN) ==="
curl -s -o /dev/null -X PUT "$B/admin/shop/products/$OD" -H "$AH" -H "$JH" \
  -d "{\"name\":\"Ordered\",\"partnerId\":$P,\"originalPrice\":20000,\"crewPrice\":10000,\"stock\":5,\"status\":\"HIDDEN\"}"
chk "어드민 목록엔 HIDDEN 으로 보임" "HIDDEN" "$(curl -s "$B/admin/shop/products" -H "$AH" | J "d.find(i=>i.name==='Ordered').status")"
chk "크루 목록에서는 빠짐" "false" "$(curl -s "$B/shop/products" -H "$CH" | J "d.items.some(i=>i.name==='Ordered')")"
curl -s -o /dev/null -X PUT "$B/admin/shop/products/$OD" -H "$AH" -H "$JH" \
  -d "{\"name\":\"Ordered\",\"partnerId\":$P,\"originalPrice\":20000,\"crewPrice\":10000,\"stock\":5,\"status\":\"ACTIVE\"}"
chk "복구도 동작" "ACTIVE" "$(curl -s "$B/admin/shop/products" -H "$AH" | J "d.find(i=>i.name==='Ordered').status")"

echo
echo "=== 4. 상품 삭제 후 파트너 삭제 차단이 풀리는지 ==="
curl -s -o /dev/null -X POST "$B/admin/partners" -H "$AH" -H "$JH" \
  -d '{"name":"Temp Shop","category":"ETC","status":"ACTIVE","sortOrder":9}'
TP=$(curl -s "$B/admin/partners" -H "$AH" | J "d.find(p=>p.name==='Temp Shop').id")
mkprod "{\"name\":\"TempProd\",\"partnerId\":$TP,\"originalPrice\":1000,\"crewPrice\":900,\"stock\":1}"
TPD=$(pid TempProd)
chk "상품 있는 동안 파트너 삭제 차단 400" "400" "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$B/admin/partners/$TP" -H "$AH")"
chk "상품 하드 삭제 200" "200" "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$B/admin/shop/products/$TPD" -H "$AH")"
chk "이제 파트너 삭제 200" "200" "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$B/admin/partners/$TP" -H "$AH")"
chk "파트너 목록에서 소멸" "false" "$(curl -s "$B/admin/partners" -H "$AH" | J "d.some(p=>p.name==='Temp Shop')")"

echo
echo "=== 5. 주문 내역은 멀쩡한가 (회귀) ==="
chk "내 주문 1건" "1" "$(curl -s "$B/shop/orders" -H "$CH" | J "d.length")"
OID=$(curl -s "$B/shop/orders" -H "$CH" | J "d[0].id")
chk "주문 상세의 상품명 스냅샷" "Ordered" "$(curl -s "$B/shop/orders/$OID" -H "$CH" | J "d.items[0].productName")"

echo
echo "=== 6. 없는 상품 삭제 404 ==="
chk "존재하지 않는 id" "404" "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$B/admin/shop/products/999999" -H "$AH")"

echo
echo "================================"
echo "  PASS $pass  /  FAIL $fail"
echo "================================"
[ "$fail" -eq 0 ]
