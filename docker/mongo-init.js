

//* MongoDB replica set must be INITIATED before it will accept
// * transactions. Without this, @Transactional in OrderService/
// * PaymentService silently fails with:
// *   "Transaction numbers are only allowed on a replica set member"
// *
// * This script runs automatically on first MongoDB container startup
// * (because we mounted it under /docker-entrypoint-initdb.d/).
// * It initiates the replica set with a single member (this container
// * itself) — sufficient for local development transactions.
// *
// * HOW TO VERIFY IT WORKED (from your terminal):
// *   docker exec -it ecom-mongodb mongosh -u <username> -p <password>
// *   > rs.status()
// * You should see: "set": "rs0", "myState": 1 (1 = PRIMARY)

rs.initiate({
    _id: "rs0",
    members: [{ _id: 0, host: "mongodb:27017" }]
});