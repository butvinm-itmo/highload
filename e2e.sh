#!/bin/bash

# End-to-end test script for Tarology microservices
# Tests user-service (8081), tarot-service (8082), and divination-service (8083)

set -e

# Configuration
CONFIG_SERVER="http://localhost:8888"
USER_SERVICE="http://localhost:8081"
TAROT_SERVICE="http://localhost:8082"
DIVINATION_SERVICE="http://localhost:8083"
API_VERSION="v0.0.1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
TESTS_PASSED=0
TESTS_FAILED=0

# Helper functions
log_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    TESTS_PASSED=$((TESTS_PASSED + 1))
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1"
    TESTS_FAILED=$((TESTS_FAILED + 1))
}

assert_status() {
    local expected=$1
    local actual=$2
    local test_name=$3

    if [ "$actual" -eq "$expected" ]; then
        log_success "$test_name (status: $actual)"
        return 0
    else
        log_error "$test_name - Expected status $expected, got $actual"
        return 1
    fi
}

assert_not_empty() {
    local value=$1
    local field_name=$2
    local test_name=$3

    if [ -n "$value" ] && [ "$value" != "null" ]; then
        log_success "$test_name - $field_name is not empty"
        return 0
    else
        log_error "$test_name - $field_name is empty or null"
        return 1
    fi
}

assert_equals() {
    local expected=$1
    local actual=$2
    local test_name=$3

    if [ "$expected" = "$actual" ]; then
        log_success "$test_name"
        return 0
    else
        log_error "$test_name - Expected '$expected', got '$actual'"
        return 1
    fi
}

# Wait for services to be ready
wait_for_services() {
    log_info "Waiting for services to be ready..."

    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -s "$CONFIG_SERVER/actuator/health" | jq -e '.status == "UP"' > /dev/null 2>&1 && \
           curl -s "$USER_SERVICE/actuator/health" | jq -e '.status == "UP"' > /dev/null 2>&1 && \
           curl -s "$TAROT_SERVICE/actuator/health" | jq -e '.status == "UP"' > /dev/null 2>&1 && \
           curl -s "$DIVINATION_SERVICE/actuator/health" | jq -e '.status == "UP"' > /dev/null 2>&1; then
            log_success "All services are healthy (including Config Server)"
            return 0
        fi

        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done

    log_error "Services did not become ready in time"
    exit 1
}

echo "=================================================="
echo "  Tarology Microservices E2E Tests"
echo "=================================================="
echo ""

wait_for_services

echo ""
echo "=================================================="
echo "  1. USER SERVICE TESTS"
echo "=================================================="
echo ""

# Test 1.1: Get existing admin user
log_info "Test 1.1: Get users list (should contain admin)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$USER_SERVICE/api/$API_VERSION/users")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "GET /users"
# API returns array directly, not paginated object
ADMIN_EXISTS=$(echo "$BODY" | jq -r '.[] | select(.username == "admin") | .id')
assert_not_empty "$ADMIN_EXISTS" "admin user" "Admin user exists in database"

# Test 1.2: Create a new user
log_info "Test 1.2: Create new user"
TEST_USERNAME="e2e_test_user_$(date +%s)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$USER_SERVICE/api/$API_VERSION/users" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$TEST_USERNAME\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 201 "$HTTP_CODE" "POST /users"
USER_ID=$(echo "$BODY" | jq -r '.id')
assert_not_empty "$USER_ID" "user id" "User created with ID"

# Test 1.3: Get user by ID
log_info "Test 1.3: Get user by ID"
RESPONSE=$(curl -s -w "\n%{http_code}" "$USER_SERVICE/api/$API_VERSION/users/$USER_ID")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "GET /users/{id}"
RETURNED_USERNAME=$(echo "$BODY" | jq -r '.username')
assert_equals "$TEST_USERNAME" "$RETURNED_USERNAME" "Username matches"

# Test 1.4: Update user
log_info "Test 1.4: Update user"
UPDATED_USERNAME="${TEST_USERNAME}_updated"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$USER_SERVICE/api/$API_VERSION/users/$USER_ID" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$UPDATED_USERNAME\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "PUT /users/{id}"
RETURNED_USERNAME=$(echo "$BODY" | jq -r '.username')
assert_equals "$UPDATED_USERNAME" "$RETURNED_USERNAME" "Username updated"

# Test 1.5: Duplicate username conflict
log_info "Test 1.5: Create user with duplicate username (should fail)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$USER_SERVICE/api/$API_VERSION/users" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$UPDATED_USERNAME\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 409 "$HTTP_CODE" "POST /users with duplicate username returns 409"

# Test 1.6: Get non-existent user
log_info "Test 1.6: Get non-existent user"
FAKE_UUID="00000000-0000-0000-0000-000000000000"
RESPONSE=$(curl -s -w "\n%{http_code}" "$USER_SERVICE/api/$API_VERSION/users/$FAKE_UUID")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 404 "$HTTP_CODE" "GET /users/{non-existent-id} returns 404"

# Test 1.7: Internal endpoint for Feign clients
log_info "Test 1.7: Internal endpoint for user entity"
RESPONSE=$(curl -s -w "\n%{http_code}" "$USER_SERVICE/api/internal/users/$USER_ID/entity")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "GET /api/internal/users/{id}/entity"
INTERNAL_USER_ID=$(echo "$BODY" | jq -r '.id')
assert_equals "$USER_ID" "$INTERNAL_USER_ID" "Internal endpoint returns correct user"

echo ""
echo "=================================================="
echo "  2. TAROT SERVICE TESTS"
echo "=================================================="
echo ""

# Test 2.1: Get cards (paginated, max 50 per page)
log_info "Test 2.1: Get cards (paginated)"
RESPONSE=$(curl -s -w "\n%{http_code}" "$TAROT_SERVICE/api/$API_VERSION/cards?size=50")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "GET /cards"
# API returns array directly, max 50 per page
CARD_COUNT=$(echo "$BODY" | jq -r 'length')
assert_equals "50" "$CARD_COUNT" "First page returns 50 cards"

# Get second page to verify more cards exist
RESPONSE2=$(curl -s "$TAROT_SERVICE/api/$API_VERSION/cards?page=1&size=50")
CARD_COUNT_PAGE2=$(echo "$RESPONSE2" | jq -r 'length')
TOTAL_CARDS=$((CARD_COUNT + CARD_COUNT_PAGE2))
if [ "$TOTAL_CARDS" -eq 78 ]; then
    log_success "Total 78 tarot cards returned across pages"
else
    log_error "Expected 78 total cards, got $TOTAL_CARDS"
fi

# Test 2.2: Verify card structure
log_info "Test 2.2: Verify card structure"
FIRST_CARD=$(echo "$BODY" | jq '.[0]')
CARD_NAME=$(echo "$FIRST_CARD" | jq -r '.name')
CARD_ARCANA=$(echo "$FIRST_CARD" | jq -r '.arcanaType.name')
assert_not_empty "$CARD_NAME" "card name" "Card has name"
assert_not_empty "$CARD_ARCANA" "arcana type" "Card has arcana type"

# Test 2.3: Get all layout types
log_info "Test 2.3: Get all layout types"
RESPONSE=$(curl -s -w "\n%{http_code}" "$TAROT_SERVICE/api/$API_VERSION/layout-types")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "GET /layout-types"
# API returns array directly
LAYOUT_COUNT=$(echo "$BODY" | jq -r 'length')
if [ "$LAYOUT_COUNT" -ge 3 ]; then
    log_success "At least 3 layout types returned (got $LAYOUT_COUNT)"
else
    log_error "Expected at least 3 layout types, got $LAYOUT_COUNT"
fi

# Store layout type IDs for later use
ONE_CARD_LAYOUT_ID=$(echo "$BODY" | jq -r '.[] | select(.name == "ONE_CARD") | .id')
THREE_CARDS_LAYOUT_ID=$(echo "$BODY" | jq -r '.[] | select(.name == "THREE_CARDS") | .id')
assert_not_empty "$ONE_CARD_LAYOUT_ID" "ONE_CARD layout id" "ONE_CARD layout exists"

# Test 2.4: Internal endpoint - get random cards
log_info "Test 2.4: Internal endpoint - get random cards"
RESPONSE=$(curl -s -w "\n%{http_code}" "$TAROT_SERVICE/api/internal/cards/random?count=3")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "GET /api/internal/cards/random?count=3"
RANDOM_CARD_COUNT=$(echo "$BODY" | jq -r 'length')
assert_equals "3" "$RANDOM_CARD_COUNT" "3 random cards returned"

# Test 2.5: Internal endpoint - get layout type by ID
log_info "Test 2.5: Internal endpoint - get layout type by ID"
RESPONSE=$(curl -s -w "\n%{http_code}" "$TAROT_SERVICE/api/internal/layout-types/$ONE_CARD_LAYOUT_ID")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "GET /api/internal/layout-types/{id}"
LAYOUT_NAME=$(echo "$BODY" | jq -r '.name')
assert_equals "ONE_CARD" "$LAYOUT_NAME" "Layout type name matches"

echo ""
echo "=================================================="
echo "  3. DIVINATION SERVICE TESTS"
echo "=================================================="
echo ""

# Test 3.1: Create a spread (uses Feign clients to call user-service and tarot-service)
log_info "Test 3.1: Create spread (inter-service communication)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$DIVINATION_SERVICE/api/$API_VERSION/spreads" \
    -H "Content-Type: application/json" \
    -d "{
        \"question\": \"E2E test question - What does the future hold?\",
        \"layoutTypeId\": \"$ONE_CARD_LAYOUT_ID\",
        \"authorId\": \"$USER_ID\"
    }")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 201 "$HTTP_CODE" "POST /spreads"
SPREAD_ID=$(echo "$BODY" | jq -r '.id')
assert_not_empty "$SPREAD_ID" "spread id" "Spread created with ID"

# Test 3.2: Get spread by ID
log_info "Test 3.2: Get spread by ID"
RESPONSE=$(curl -s -w "\n%{http_code}" "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "GET /spreads/{id}"
SPREAD_QUESTION=$(echo "$BODY" | jq -r '.question')
assert_equals "E2E test question - What does the future hold?" "$SPREAD_QUESTION" "Spread question matches"

# Verify spread has cards
SPREAD_CARDS_COUNT=$(echo "$BODY" | jq -r '.cards | length')
assert_equals "1" "$SPREAD_CARDS_COUNT" "ONE_CARD spread has 1 card"

# Verify author info is fetched via Feign
SPREAD_AUTHOR_USERNAME=$(echo "$BODY" | jq -r '.author.username')
assert_equals "$UPDATED_USERNAME" "$SPREAD_AUTHOR_USERNAME" "Spread author fetched via Feign"

# Test 3.3: Create spread with THREE_CARDS layout
log_info "Test 3.3: Create spread with THREE_CARDS layout"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$DIVINATION_SERVICE/api/$API_VERSION/spreads" \
    -H "Content-Type: application/json" \
    -d "{
        \"question\": \"E2E test - Past, Present, Future?\",
        \"layoutTypeId\": \"$THREE_CARDS_LAYOUT_ID\",
        \"authorId\": \"$USER_ID\"
    }")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 201 "$HTTP_CODE" "POST /spreads (THREE_CARDS)"
SPREAD_ID_2=$(echo "$BODY" | jq -r '.id')

# Verify it has 3 cards
RESPONSE=$(curl -s "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID_2")
SPREAD_CARDS_COUNT=$(echo "$RESPONSE" | jq -r '.cards | length')
assert_equals "3" "$SPREAD_CARDS_COUNT" "THREE_CARDS spread has 3 cards"

# Test 3.4: Get spreads list with pagination
log_info "Test 3.4: Get spreads list with pagination"
RESPONSE=$(curl -s -w "\n%{http_code}" "$DIVINATION_SERVICE/api/$API_VERSION/spreads?page=0&size=10")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "GET /spreads?page=0&size=10"
# API returns array directly
SPREADS_COUNT=$(echo "$BODY" | jq -r 'length')
if [ "$SPREADS_COUNT" -ge 2 ]; then
    log_success "At least 2 spreads returned (got $SPREADS_COUNT)"
else
    log_error "Expected at least 2 spreads, got $SPREADS_COUNT"
fi

# Test 3.5: Get spreads with scroll pagination
log_info "Test 3.5: Get spreads with scroll pagination"
RESPONSE=$(curl -s -D - "$DIVINATION_SERVICE/api/$API_VERSION/spreads/scroll?size=1" 2>&1)
HTTP_CODE=$(echo "$RESPONSE" | grep -m1 "HTTP/" | awk '{print $2}')
BODY=$(echo "$RESPONSE" | sed -n '/^\[/,$p')
X_AFTER=$(echo "$RESPONSE" | grep -i "X-After:" | awk '{print $2}' | tr -d '\r')

assert_status 200 "$HTTP_CODE" "GET /spreads/scroll?size=1"
# API returns array directly, uses X-After header for cursor
SCROLL_CONTENT_COUNT=$(echo "$BODY" | jq -r 'length')
assert_equals "1" "$SCROLL_CONTENT_COUNT" "Scroll returns 1 item"
if [ -n "$X_AFTER" ]; then
    log_success "X-After header present (more items available)"
else
    log_error "X-After header missing (expected more items)"
fi

# Test 3.6: Create interpretation
log_info "Test 3.6: Create interpretation"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID/interpretations" \
    -H "Content-Type: application/json" \
    -d "{
        \"text\": \"E2E test interpretation - The cards suggest great fortune ahead!\",
        \"authorId\": \"$USER_ID\"
    }")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 201 "$HTTP_CODE" "POST /spreads/{id}/interpretations"
INTERPRETATION_ID=$(echo "$BODY" | jq -r '.id')
assert_not_empty "$INTERPRETATION_ID" "interpretation id" "Interpretation created with ID"

# Test 3.7: Get spread with interpretation
log_info "Test 3.7: Verify spread now has interpretation"
RESPONSE=$(curl -s "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID")
INTERPRETATIONS_COUNT=$(echo "$RESPONSE" | jq -r '.interpretations | length')
assert_equals "1" "$INTERPRETATIONS_COUNT" "Spread has 1 interpretation"

# Test 3.8: Duplicate interpretation conflict
log_info "Test 3.8: Create duplicate interpretation (should fail)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID/interpretations" \
    -H "Content-Type: application/json" \
    -d "{
        \"text\": \"Another interpretation attempt\",
        \"authorId\": \"$USER_ID\"
    }")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 409 "$HTTP_CODE" "POST duplicate interpretation returns 409"

# Test 3.9: Update interpretation
log_info "Test 3.9: Update interpretation"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID/interpretations/$INTERPRETATION_ID" \
    -H "Content-Type: application/json" \
    -d "{
        \"text\": \"Updated E2E interpretation - Even better fortune!\",
        \"authorId\": \"$USER_ID\"
    }")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

assert_status 200 "$HTTP_CODE" "PUT /spreads/{id}/interpretations/{id}"
UPDATED_TEXT=$(echo "$BODY" | jq -r '.text')
assert_equals "Updated E2E interpretation - Even better fortune!" "$UPDATED_TEXT" "Interpretation text updated"

# Test 3.10: Update interpretation by non-author (should fail)
log_info "Test 3.10: Update interpretation by non-author (should fail)"
# Get admin user ID
ADMIN_ID=$(curl -s "$USER_SERVICE/api/$API_VERSION/users" | jq -r '.[] | select(.username == "admin") | .id')
RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID/interpretations/$INTERPRETATION_ID" \
    -H "Content-Type: application/json" \
    -d "{
        \"text\": \"Malicious update attempt\",
        \"authorId\": \"$ADMIN_ID\"
    }")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 403 "$HTTP_CODE" "PUT interpretation by non-author returns 403"

# Test 3.11: Create spread with non-existent user (should fail)
log_info "Test 3.11: Create spread with non-existent user"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$DIVINATION_SERVICE/api/$API_VERSION/spreads" \
    -H "Content-Type: application/json" \
    -d "{
        \"question\": \"This should fail\",
        \"layoutTypeId\": \"$ONE_CARD_LAYOUT_ID\",
        \"authorId\": \"$FAKE_UUID\"
    }")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 404 "$HTTP_CODE" "POST /spreads with non-existent user returns 404"

# Test 3.12: Create spread with non-existent layout type (should fail)
log_info "Test 3.12: Create spread with non-existent layout type"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$DIVINATION_SERVICE/api/$API_VERSION/spreads" \
    -H "Content-Type: application/json" \
    -d "{
        \"question\": \"This should fail\",
        \"layoutTypeId\": \"$FAKE_UUID\",
        \"authorId\": \"$USER_ID\"
    }")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 404 "$HTTP_CODE" "POST /spreads with non-existent layout returns 404"

echo ""
echo "=================================================="
echo "  4. CLEANUP TESTS"
echo "=================================================="
echo ""

# Test 4.1: Delete interpretation
log_info "Test 4.1: Delete interpretation"
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID/interpretations/$INTERPRETATION_ID" \
    -H "Content-Type: application/json" \
    -d "{\"userId\": \"$USER_ID\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 204 "$HTTP_CODE" "DELETE /spreads/{id}/interpretations/{id}"

# Verify interpretation is deleted
RESPONSE=$(curl -s "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID")
INTERPRETATIONS_COUNT=$(echo "$RESPONSE" | jq -r '.interpretations | length')
assert_equals "0" "$INTERPRETATIONS_COUNT" "Spread has 0 interpretations after deletion"

# Test 4.2: Delete spread
log_info "Test 4.2: Delete spread"
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID" \
    -H "Content-Type: application/json" \
    -d "{\"userId\": \"$USER_ID\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 204 "$HTTP_CODE" "DELETE /spreads/{id}"

# Verify spread is deleted
RESPONSE=$(curl -s -w "\n%{http_code}" "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
assert_status 404 "$HTTP_CODE" "GET deleted spread returns 404"

# Test 4.3: Delete spread by non-author (should fail)
log_info "Test 4.3: Delete spread by non-author (should fail)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID_2" \
    -H "Content-Type: application/json" \
    -d "{\"userId\": \"$ADMIN_ID\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 403 "$HTTP_CODE" "DELETE spread by non-author returns 403"

# Test 4.4: Delete second spread (cleanup)
log_info "Test 4.4: Delete second spread (cleanup)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$DIVINATION_SERVICE/api/$API_VERSION/spreads/$SPREAD_ID_2" \
    -H "Content-Type: application/json" \
    -d "{\"userId\": \"$USER_ID\"}")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 204 "$HTTP_CODE" "DELETE /spreads/{id} (cleanup)"

# Test 4.5: Delete test user
log_info "Test 4.5: Delete test user"
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$USER_SERVICE/api/$API_VERSION/users/$USER_ID")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

assert_status 204 "$HTTP_CODE" "DELETE /users/{id}"

# Verify user is deleted
RESPONSE=$(curl -s -w "\n%{http_code}" "$USER_SERVICE/api/$API_VERSION/users/$USER_ID")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
assert_status 404 "$HTTP_CODE" "GET deleted user returns 404"

echo ""
echo "=================================================="
echo "  TEST SUMMARY"
echo "=================================================="
echo ""
echo -e "Tests passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests failed: ${RED}$TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
fi
