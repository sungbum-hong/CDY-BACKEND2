#!/bin/bash
# 크루 혜택몰(제휴 업체 몰) 백엔드 개편 검증. 페이로드는 전부 ASCII (CP949 깨짐 회피)
# 깨끗한 H2 로 기동한 직후에 1회 실행하는 것을 전제로 한다.
B=http://localhost:8099/api/v1

pass=0; fail=0
chk() { # chk "라벨" "기대" "실제"
  if [ "$2" == "$3" ]; then echo "  PASS  $1"; pass=$((pass+1));
  else echo "  FAIL  $1 | expected=[$2] actual=[$3]"; fail=$((fail+1)); fi
}

# stdin 의 JSON 을 d 로 놓고 인자로 준 JS 식을 평가한다 (이 환경의 python 은 스토어 스텁이라 못 씀)
J() { node -e 'let s="";process.stdin.on("data",c=>s+=c).on("end",()=>{const d=JSON.parse(s);console.log(eval(process.argv[1]))})' "$1"; }

echo "=== 0. 계정 준비 ==="
curl -s -o /dev/null -X POST "$B/auth/sign-up" -H 'Content-Type: application/json' \
  -d '{"username":"admin@cdy.com","password":"123456","nickname":"admin","userCategory":"CODING","name":"Admin","phoneNumber":"01000000000"}'
curl -s -o /dev/null -X POST "$B/auth/sign-up" -H 'Content-Type: application/json' \
  -d '{"username":"crew@cdy.com","password":"123456","nickname":"crew","userCategory":"CODING","name":"Crew","phoneNumber":"01011111111"}'
curl -s -o /dev/null -X POST "$B/admin/bootstrap" -H 'Content-Type: application/json' \
  -d '{"username":"admin@cdy.com","secretKey":"cdy-admin-bootstrap-2026"}'

login() { # 토큰은 응답 본문에 평문 JWT 로 온다
  curl -s -X POST "$B/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"123456\"}" | tr -d '\r\n'
}
AT=$(login admin@cdy.com); CT=$(login crew@cdy.com)
AH="Authorization: Bearer $AT"; CH="Authorization: Bearer $CT"; JH='Content-Type: application/json'
echo "  admin=${#AT}자 crew=${#CT}자"

echo
echo "=== 1. 제휴 업체 3곳 (CAFE / EDU / HIDDEN-TOOL) ==="
mkpartner() { curl -s -o /dev/null -X POST "$B/admin/partners" -H "$AH" -H "$JH" -d "$1"; }
mkpartner '{"name":"Cafe Moon","category":"CAFE","status":"ACTIVE","sortOrder":1}'
mkpartner '{"name":"Edu Lab","category":"EDU","status":"ACTIVE","sortOrder":2}'
# 숨김 업체는 일단 ACTIVE 로 만든다 — HIDDEN 업체에는 상품 등록이 막혀 있어서
# 그대로 두면 상품이 안 생기고, "숨김 업체 상품 제외" 검증이 공허하게 통과한다.
mkpartner '{"name":"Ghost Tools","category":"TOOL","status":"ACTIVE","sortOrder":3}'

PARTNERS=$(curl -s "$B/admin/partners" -H "$AH")
pid() { echo "$PARTNERS" | J "d.find(p=>p.name==='$1').id"; }
P_CAFE=$(pid 'Cafe Moon'); P_EDU=$(pid 'Edu Lab'); P_HID=$(pid 'Ghost Tools')
echo "  cafe=$P_CAFE edu=$P_EDU hidden=$P_HID"

echo
echo "=== 2. 상품 등록 ==="
mkprod() { curl -s -o /dev/null -X POST "$B/admin/shop/products" -H "$AH" -H "$JH" -d "$1"; }
mkprod "{\"name\":\"Cold Brew Set\",\"partnerId\":$P_CAFE,\"originalPrice\":10000,\"crewPrice\":7000,\"stock\":10}"   # 30%
mkprod "{\"name\":\"Drip Bag\",\"partnerId\":$P_CAFE,\"originalPrice\":5000,\"crewPrice\":4500,\"stock\":10}"          # 10%
mkprod "{\"name\":\"Coding Class\",\"partnerId\":$P_EDU,\"originalPrice\":100000,\"crewPrice\":40000,\"stock\":5}"     # 60%
mkprod "{\"name\":\"Ghost Pass\",\"partnerId\":$P_HID,\"originalPrice\":100000,\"crewPrice\":10000,\"stock\":5}"       # 90%
# 상품을 붙인 뒤 업체를 숨긴다 → 이제부터 Ghost Pass 는 크루 화면 어디에도 안 나와야 한다
curl -s -o /dev/null -X PUT "$B/admin/partners/$P_HID" -H "$AH" -H "$JH" \
  -d '{"name":"Ghost Tools","category":"TOOL","status":"HIDDEN","sortOrder":3}'
chk "픽스처: Ghost Pass 가 실제로 등록됨" "true" \
  "$(curl -s "$B/admin/shop/products" -H "$AH" | J "d.some(i=>i.name==='Ghost Pass')")"

echo
echo "=== 3. 상품 목록 / 필터 ==="
R=$(curl -s "$B/shop/products" -H "$CH")
chk "전체 목록 3건 (HIDDEN 업체 제외)" "3"  "$(echo "$R" | J "d.items.length")"
chk "maxDiscountRate=60 (숨김업체 90% 미포함)" "60" "$(echo "$R" | J "d.maxDiscountRate")"
chk "Ghost Pass 미노출" "false" "$(echo "$R" | J "d.items.some(i=>i.name==='Ghost Pass')")"
chk "항목에 partnerName" "Cafe Moon" "$(echo "$R" | J "d.items.find(i=>i.name==='Cold Brew Set').partnerName")"
chk "항목에 partnerCategory" "CAFE" "$(echo "$R" | J "d.items.find(i=>i.name==='Cold Brew Set').partnerCategory")"
chk "항목에 partnerId" "$P_CAFE" "$(echo "$R" | J "d.items.find(i=>i.name==='Cold Brew Set').partnerId")"
chk "discountRate 30%" "30" "$(echo "$R" | J "d.items.find(i=>i.name==='Cold Brew Set').discountRate")"

R=$(curl -s "$B/shop/products?category=CAFE" -H "$CH")
chk "category=CAFE → 2건" "2" "$(echo "$R" | J "d.items.length")"
chk "필터해도 maxDiscountRate는 전체 기준" "60" "$(echo "$R" | J "d.maxDiscountRate")"

chk "category=EDU → 1건" "1" "$(curl -s "$B/shop/products?category=EDU" -H "$CH" | J "d.items.length")"
chk "partnerId 필터 → 2건" "2" "$(curl -s "$B/shop/products?partnerId=$P_CAFE" -H "$CH" | J "d.items.length")"
chk "HIDDEN 업체 partnerId → 0건" "0" "$(curl -s "$B/shop/products?partnerId=$P_HID" -H "$CH" | J "d.items.length")"
chk "keyword=상품명" "1" "$(curl -s "$B/shop/products?keyword=Drip" -H "$CH" | J "d.items.length")"

R=$(curl -s "$B/shop/products?keyword=Edu%20Lab" -H "$CH")
chk "keyword=업체명 검색" "1" "$(echo "$R" | J "d.items.length")"
chk "업체명 검색 결과가 그 업체 상품" "Coding Class" "$(echo "$R" | J "d.items[0].name")"
chk "업체명 대소문자 무시" "2" "$(curl -s "$B/shop/products?keyword=cafe%20moon" -H "$CH" | J "d.items.length")"
chk "옛 카테고리(LIVING) → 400" "400" "$(curl -s -o /dev/null -w '%{http_code}' "$B/shop/products?category=LIVING" -H "$CH")"

echo
echo "=== 4. 제휴 업체 목록 ==="
R=$(curl -s "$B/shop/partners" -H "$CH")
chk "판매중 상품 있는 업체만 2곳" "2" "$(echo "$R" | J "d.length")"
chk "HIDDEN 업체 제외" "false" "$(echo "$R" | J "d.some(p=>p.name==='Ghost Tools')")"
chk "productCount 집계" "2" "$(echo "$R" | J "d.find(p=>p.name==='Cafe Moon').productCount")"
chk "업체 category 포함" "EDU" "$(echo "$R" | J "d.find(p=>p.name==='Edu Lab').category")"
chk "업체 정렬 sortOrder 순" "Cafe Moon" "$(echo "$R" | J "d[0].name")"

echo
echo "=== 5. 오늘의 특가 ==="
R=$(curl -s "$B/shop/deals/today" -H "$CH")
chk "할인율 1위" "Coding Class" "$(echo "$R" | J "d[0].name")"
chk "할인율 2위" "Cold Brew Set" "$(echo "$R" | J "d[1].name")"
chk "특가에도 HIDDEN 업체 제외" "false" "$(echo "$R" | J "d.some(i=>i.name==='Ghost Pass')")"
chk "판매중 3건 전부 (6개 미만)" "3" "$(echo "$R" | J "d.length")"

echo
echo "=== 6. 어드민 partnerId 검증 ==="
chk "partnerId 누락 → 400" "400" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$B/admin/shop/products" -H "$AH" -H "$JH" \
  -d '{"name":"No Partner","originalPrice":1000,"crewPrice":900,"stock":1}')"
chk "없는 partnerId → 400" "400" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$B/admin/shop/products" -H "$AH" -H "$JH" \
  -d '{"name":"Bad Partner","partnerId":999999,"originalPrice":1000,"crewPrice":900,"stock":1}')"
chk "HIDDEN 업체에 등록 → 400" "400" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$B/admin/shop/products" -H "$AH" -H "$JH" \
  -d "{\"name\":\"Hidden Partner\",\"partnerId\":$P_HID,\"originalPrice\":1000,\"crewPrice\":900,\"stock\":1}")"

R=$(curl -s -X POST "$B/admin/shop/products/bulk" -H "$AH" -H "$JH" \
  -d "[{\"name\":\"Bulk OK\",\"partnerId\":$P_CAFE,\"originalPrice\":2000,\"crewPrice\":1000,\"stock\":3},{\"name\":\"Bulk Bad\",\"partnerId\":999999,\"originalPrice\":2000,\"crewPrice\":1000,\"stock\":3}]")
chk "bulk 성공 1건" "1" "$(echo "$R" | J "d.created")"
chk "bulk 실패 1건" "1" "$(echo "$R" | J "d.errors.length")"

R=$(curl -s "$B/admin/shop/products" -H "$AH")
chk "어드민 목록에 partnerName" "Cafe Moon" "$(echo "$R" | J "d.find(i=>i.name==='Cold Brew Set').partnerName")"
chk "어드민 목록에 발주용 supplierName 유지" "true" "$(echo "$R" | J "'supplierName' in d[0]")"
chk "어드민 목록에 HIDDEN 업체 상품도 노출" "true" "$(echo "$R" | J "d.some(i=>i.name==='Ghost Pass')")"

echo
echo "=== 7. 업체 숨김 파급 / 삭제 차단 ==="
curl -s -o /dev/null -X PUT "$B/admin/partners/$P_EDU" -H "$AH" -H "$JH" \
  -d '{"name":"Edu Lab","category":"EDU","status":"HIDDEN","sortOrder":2}'
R=$(curl -s "$B/shop/products" -H "$CH")
chk "업체 숨김 → 목록에서 제외" "false" "$(echo "$R" | J "d.items.some(i=>i.name==='Coding Class')")"
chk "업체 숨김 → maxDiscountRate 재계산(50)" "50" "$(echo "$R" | J "d.maxDiscountRate")"
chk "업체 숨김 → 업체 목록도 1곳" "1" "$(curl -s "$B/shop/partners" -H "$CH" | J "d.length")"
curl -s -o /dev/null -X PUT "$B/admin/partners/$P_EDU" -H "$AH" -H "$JH" \
  -d '{"name":"Edu Lab","category":"EDU","status":"ACTIVE","sortOrder":2}'
chk "숨김 해제 → 복구 (bulk 로 추가된 1건 포함 4건)" "4" "$(curl -s "$B/shop/products" -H "$CH" | J "d.items.length")"
chk "상품 달린 업체 삭제 차단 (400)" "400" "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$B/admin/partners/$P_CAFE" -H "$AH")"

echo
echo "=== 8. 상세 ==="
PID=$(curl -s "$B/shop/products?keyword=Drip" -H "$CH" | J "d.items[0].id")
R=$(curl -s "$B/shop/products/$PID" -H "$CH")
chk "상세에 partnerName" "Cafe Moon" "$(echo "$R" | J "d.partnerName")"
chk "상세에 partnerCategory" "CAFE" "$(echo "$R" | J "d.partnerCategory")"
chk "상세에 발주용 supplierName 미노출" "false" "$(echo "$R" | J "'supplierName' in d")"
GID=$(curl -s "$B/admin/shop/products" -H "$AH" | J "d.find(i=>i.name==='Ghost Pass').id")
chk "HIDDEN 업체 상품 상세 → 404" "404" "$(curl -s -o /dev/null -w '%{http_code}' "$B/shop/products/$GID" -H "$CH")"

echo
echo "=== 9. 장바구니 / 주문 / 결제 회귀 ==="
CID=$(curl -s "$B/shop/products?keyword=Cold" -H "$CH" | J "d.items[0].id")
curl -s -o /dev/null -X POST "$B/shop/cart" -H "$CH" -H "$JH" -d "{\"productId\":$CID,\"quantity\":2}"
chk "장바구니 count" "1" "$(curl -s "$B/shop/cart/count" -H "$CH" | J "d.count")"

ORD=$(curl -s -X POST "$B/shop/orders" -H "$CH" -H "$JH" \
  -d "{\"items\":[{\"productId\":$CID,\"quantity\":2}],\"receiverName\":\"Crew\",\"receiverPhone\":\"01011111111\",\"postcode\":\"12345\",\"address\":\"Seoul\",\"addressDetail\":\"101\",\"deliveryMemo\":\"none\"}")
chk "주문 총액 서버 계산 (7000x2)" "14000" "$(echo "$ORD" | J "d.totalAmount")"
OID=$(echo "$ORD" | J "d.orderId")
chk "금액 위조 승인 → 400" "400" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$B/shop/orders/$OID/confirm-payment" -H "$CH" -H "$JH" \
  -d '{"paymentKey":"fake_key","amount":100}')"
chk "남의 주문 조회 차단 (404)" "404" "$(curl -s -o /dev/null -w '%{http_code}' "$B/shop/orders/$OID" -H "$AH")"
chk "내 주문 목록 1건" "1" "$(curl -s "$B/shop/orders" -H "$CH" | J "d.length")"
chk "주문 취소" "200" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$B/shop/orders/$OID/cancel" -H "$CH")"

echo
echo "=== 10. 인증/공개 경계 (회귀) ==="
chk "비로그인 /shop/products" "401" "$(curl -s -o /dev/null -w '%{http_code}' "$B/shop/products")"
chk "비로그인 /shop/partners" "401" "$(curl -s -o /dev/null -w '%{http_code}' "$B/shop/partners")"
chk "비로그인 /shop/deals/today" "401" "$(curl -s -o /dev/null -w '%{http_code}' "$B/shop/deals/today")"
chk "홈 배너 /partners 공개 유지" "200" "$(curl -s -o /dev/null -w '%{http_code}' "$B/partners")"
chk "혜택몰 /benefits 로그인시 200" "200" "$(curl -s -o /dev/null -w '%{http_code}' "$B/benefits" -H "$CH")"
chk "혜택몰 /benefits 비로그인 401" "401" "$(curl -s -o /dev/null -w '%{http_code}' "$B/benefits")"

echo
echo "================================"
echo "  PASS $pass  /  FAIL $fail"
echo "================================"
[ "$fail" -eq 0 ]
