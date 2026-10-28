#!/usr/bin/env bash

BASE=http://localhost:8080


echo "Ожидается: 200 OK"
curl -i $BASE/ping

echo "Ожидается: 201 Created"
curl -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Barsik","type":"CAT","ownerName":"Alice","vaccinated":false}'

echo "Ожидается: 201 Created"
curl -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Rex","type":"DOG","ownerName":"Bob","vaccinated":true}'


echo "Ожидается: 200 OK"
curl -i $BASE/pets

echo "Ожидается: 200 OK"
curl -i $BASE/pets/1


echo "Ожидается: 200 OK"
curl -i -X PUT $BASE/pets/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Barsik","type":"CAT","ownerName":"Alice Smith","vaccinated":true}'


echo "Ожидается: 204 No Content"
curl -i -X DELETE $BASE/pets/2


echo "Ожидается: 404 Not Found"
curl -i $BASE/pets/999

echo "Ожидается: 400 Bad Request"
curl -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"","type":"CAT","ownerName":"X"}'

echo "Ожидается: 400 Bad Request"
curl -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"X","type":"COW","ownerName":"Y"}'
