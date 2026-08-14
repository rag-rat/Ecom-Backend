
# TESTING ORDER — Ecom Platform

Follow these steps IN ORDER. Each step builds on the previous.
Do not skip ahead — later steps assume earlier ones passed.


<br>


═══════════════════════════════════════════════════════════════
PHASE 1 — INFRASTRUCTURE
═══════════════════════════════════════════════════════════════


## Step 1 — Start Docker services

```bash
docker-compose up -d
```

Wait 60-90 seconds for all services to become healthy.


Verify all containers are running:
```bash
docker ps
```

Expected: 5 containers all showing "healthy" status:

[//]: # (saree-mongodb)

[//]: # (saree-redis)
ecom-kafka  <br>
ecom-zookeeper   <br>
ecom-app

If any show "starting" — wait another 30 seconds.
If any show "unhealthy" — check logs: docker logs <container-name>



[//]: # (## Step 2 — Verify MongoDB)

[//]: # ()
[//]: # (```bash)

[//]: # (docker exec -it ecom-mongodb mongosh -u admin -p password123 --eval "db.adminCommand&#40;'ping'&#41;")

[//]: # (```)

[//]: # ()
[//]: # (Expected: `{ ok: 1 }`)

[//]: # ()
[//]: # (Verify replica set is active:)

[//]: # (```bash)

[//]: # (docker exec -it ecom-mongodb mongosh -u admin -p password123 --eval "rs.status&#40;&#41;.myState")

[//]: # (```)

[//]: # (Expected: `1` &#40;PRIMARY&#41;)

[//]: # ()
[//]: # (If you see errors here, @Transactional in orders and payments WILL FAIL.)



[//]: # (## Step 3 — Verify Redis)

[//]: # ()
[//]: # (```bash)

[//]: # (docker exec -it ecom-redis redis-cli -a redispassword123 ping)

[//]: # (```)

[//]: # (Expected: `PONG`)




## Step 2 — Verify Kafka

```bash
docker exec -it ecom-kafka kafka-topics --bootstrap-server localhost:9093 --list
```
Expected: `order-created` and `payment-success` in the list
(Spring Boot creates them on first startup via KafkaConfig Part 19)



## Step 3 — Verify Spring Boot app

```bash
curl http://localhost:8080/actuator/health
```

Expected:
```json
{ "status": "UP", "components": { "mongo": { "status": "UP" }, "redis": { "status": "UP" } } }
```


## Step 4 — Open Swagger UI

Navigate to: http://localhost:9092/swagger-ui.html

Expected: Full API documentation loads, showing all tagged groups
(Admin - Chat, Admin - Dashboard, Admin - Inventory, Admin - Orders,
Admin - Reviews, Admin - User Management, Addresses, Authentication,
Cart, Categories, Notifications, Orders, Payments, Products, Reviews,
User Profile, Wishlist)

<br>
<br>


═══════════════════════════════════════════════════════════════
PHASE 2 — AUTHENTICATION
═══════════════════════════════════════════════════════════════

## Step 5 — Register a customer

POST http://localhost:9092/api/auth/register
```json
{
    "firstName": "",
    "lastName": "",
    "email": "",
    "password": "",
    "phoneNumber": ""
}
```
Expected: 201, tokens returned.
Save the `accessToken` as CUSTOMER_TOKEN.


## Step 6 — Insert admin user in MongoDB

Use MongoDB Atlas or mongosh:
```javascript
db.users.insertOne({
    "email": "",
    "password_hash": "",
    "first_name": "",
    "last_name": "",
    "phone_number": "",
    "role": "ADMIN",
    "is_active": true
})
```

[//]: # (Password: `Admin@123`)



## Step 7 — Login as admin

POST http://localhost:9092/api/auth/login
```json
{ "email": "", "password": "" }
```
Expected: 200, `role: "ADMIN"`.
Save as ADMIN_TOKEN.


## Step 8 — Verify JWT blacklist (logout test)

1. POST /api/auth/logout with CUSTOMER_TOKEN in Authorization header
2. GET /api/auth/verify with the SAME token
3. Expected: 401 — token is blacklisted in Redis ✓


## Step 9 — Refresh token flow

1. Login again to get fresh tokens
2. POST /api/auth/refresh with the refreshToken
3. Expected: new accessToken returned
4. POST /api/auth/refresh again with the SAME (now rotated) refreshToken
5. Expected: 401 — old refresh token deleted ✓

<br>
<br>

═══════════════════════════════════════════════════════════════
PHASE 3 — CORE CATALOGUE
═══════════════════════════════════════════════════════════════'

## Step 10 — Create categories (Admin)

POST /api/categories with ADMIN_TOKEN:
```json
{ "name": "", "displayOrder": 1 }
```
```json
{ "name": "", "displayOrder": 2 }
```

Save a `categoryId` from the first response.

GET /api/categories (no token) — both appear ✓



## Step 11 — Create products (Admin)

POST /api/products with ADMIN_TOKEN:
```json
{
    "name": "",
    "description": "",
    "price": 4999.00,
    "categoryId": "",
    "tags": [],
    "isFeatured": true,
    "initialStock": 50
}
```
Create at least 2-3 products. Save a `productId`.


## Step 12 — Verify inventory auto-created

GET /api/inventory/<productId> with ADMIN_TOKEN
Expected: `totalStock: 50, availableStock: 50` ✓


## Step 13 — Search and filter products

GET /api/products?keyword=silk → matches ✓
GET /api/products?minPrice=1000&maxPrice=6000 → correct range ✓
GET /api/products?sortBy=price&sortDir=asc → sorted ✓
GET /api/products/featured → only isFeatured:true items ✓


## Step 14 — Redis product cache

1. GET /api/products/<productId> — logs show "Cache MISS"
2. GET /api/products/<productId> again — logs show "Cache HIT" ✓
3. PUT /api/products/<productId> (change price) — logs show "Evicted product cache"
4. GET again — "Cache MISS", fresh price returned ✓



<br>
<br>


═══════════════════════════════════════════════════════════════
PHASE 4 — CUSTOMER JOURNEY
═══════════════════════════════════════════════════════════════

## Step 15 — User profile

GET /api/users/me with CUSTOMER_TOKEN → own data ✓
PUT /api/users/me with { "firstName": "Priya Updated" } → partial update ✓


## Step 16 — Addresses

POST /api/addresses — first address auto-sets as default ✓
POST /api/addresses again — second address is NOT default ✓
PUT /api/addresses/<secondId>/set-default → first demoted, second promoted ✓


## Step 17 — Wishlist

POST /api/wishlist/<productId> → product added ✓
POST /api/wishlist/<productId> again → no duplicate ✓ (idempotent)
DELETE /api/wishlist/<productId> → removed ✓


## Step 18 — Cart

POST /api/cart/items { "productId": "...", "quantity": 2 } → item added ✓
POST same product, quantity 3 → quantity becomes 5 (additive) ✓
Add quantity exceeding stock → 400 ✓
DELETE /api/cart/items/<productId> → removed, totals recalculated ✓


<br>
<br>


═══════════════════════════════════════════════════════════════
PHASE 5 — ORDER LIFECYCLE
═══════════════════════════════════════════════════════════════

## Step 19 — Place order

Add items to cart, then:
POST /api/orders { "addressId": "<your-address-id>" }
Expected:
- 201, order returned with status: PENDING ✓
- Cart is now empty ✓
- Check Kafka logs: "Published OrderCreatedEvent" ✓
- Check logs: "Consumed OrderCreatedEvent" ✓
- If email configured: confirmation email arrives ✓


## Step 20 — Verify inventory reservation

GET /api/inventory/<productId> (admin)
Expected: `reservedStock` increased by ordered quantity ✓
(totalStock unchanged — payment not yet confirmed)


## Step 21 — Admin progresses the order

PUT /api/admin/orders/<orderId>/status { "status": "CONFIRMED" } → 200 ✓
PUT /api/admin/orders/<orderId>/status { "status": "SHIPPED" } → 200, shippedAt set ✓
PUT /api/admin/orders/<orderId>/status { "status": "PENDING" } → 400 (backward move) ✓
PUT /api/admin/orders/<orderId>/status { "status": "DELIVERED" } → 200 ✓


## Step 22 — Cancel order test

Place a new order (repeat Step 19).
PUT /api/orders/<newOrderId>/cancel { "reason": "Changed my mind" } → 200 ✓
Check inventory: reservedStock back to original ✓
Try to cancel again: 400 (already CANCELLED) ✓


<br>
<br>


═══════════════════════════════════════════════════════════════
PHASE 6 — PAYMENT
═══════════════════════════════════════════════════════════════

## Step 23 — Razorpay flow (requires real test keys)

Place a fresh order (PENDING status).
POST /api/payments/create-order { "orderId": "<orderId>" }
Expected: razorpayOrderId returned ✓

POST /api/payments/verify with a fake signature:
Expected: 400 — signature verification failed ✓

Complete a real test payment using Razorpay's Checkout:
- Test card: 4111 1111 1111 1111, any future expiry, any CVV
  POST /api/payments/verify with the real returned values
  Expected:
- 200, order status → CONFIRMED ✓
- Kafka logs: "Published PaymentSuccessEvent" ✓
- Inventory: totalStock decreased (deduct called by consumer) ✓
- If email configured: receipt email arrives ✓


<br>
<br> 


═══════════════════════════════════════════════════════════════
PHASE 7 — REVIEWS
═══════════════════════════════════════════════════════════════

## Step 24 — Submit reviews

POST /api/reviews { "productId": "...", "rating": 5, "reviewText": "Excellent quality!" }
Expected: 201 ✓

GET /api/products/<productId> → averageRating updated ✓
GET /api/reviews/product/<productId>/summary → breakdown shows { 5: 1 } ✓

Post again same product/user → 400 (already reviewed) ✓
PUT /api/reviews/<reviewId> { "rating": 4 } → updated, average recalculates ✓


## Step 25 — Admin moderation

GET /api/admin/reviews → all reviews visible ✓
DELETE /api/admin/reviews/<reviewId> → soft-deleted, excluded from product summary ✓


<br>
<br>


═══════════════════════════════════════════════════════════════
PHASE 8 — ADMIN DASHBOARD
═══════════════════════════════════════════════════════════════

## Step 26 — Dashboard summary

GET /api/admin/dashboard/summary with ADMIN_TOKEN
Verify:
- totalRevenue matches sum of CONFIRMED+ orders ✓
- ordersByStatus counts match what you created ✓
- lowStockProducts shows products below threshold ✓
- topSellingProducts ranked by units sold ✓


<br>
<br>


═══════════════════════════════════════════════════════════════
PHASE 9 — WEBSOCKET CHAT
═══════════════════════════════════════════════════════════════

## Step 27 — WebSocket chat

Use Postman WebSocket or a browser console:

1. Connect to ws://localhost:9092/ws/websocket
2. Send STOMP CONNECT with Authorization header
3. Subscribe to /user/queue/messages
4. Send STOMP message to /app/chat.send with { "content": "Hello" }
5. GET /api/admin/chat/conversations → conversation appears ✓
6. POST /api/admin/chat/conversations/<id>/reply { "content": "Hi there!" }
7. Customer's WebSocket session receives the push ✓
8. GET /api/chat/history → both messages appear ✓


<br>
<br>


═══════════════════════════════════════════════════════════════
PHASE 10 — FINAL CHECKS
═══════════════════════════════════════════════════════════════

## Step 28 — Security sanity checks

CUSTOMER token on admin endpoints → 403 ✓
No token on protected endpoints → 401 ✓
No token on GET /api/products → 200 (public) ✓
No token on GET /api/categories → 200 (public) ✓
No token on GET /api/reviews/product/<id> → 200 (public) ✓
Editing another user's review → 400 (ownership check) ✓
Accessing another user's order → 400 (ownership check) ✓
Shipping to another user's address → 403 (ownership check) ✓


## Step 29 — docker-compose down test

docker-compose down
docker-compose up -d

[//]: # (GET /api/products — data persisted &#40;MongoDB volume&#41; ✓)

[//]: # (Check Redis: blacklisted JWT tokens gone &#40;expected — TTL expired or)

[//]: # (new container, but new logins work fine&#41; ✓)


<br> 
<br>


ALL STEPS PASSED = Backend is production-ready ✓


<br>