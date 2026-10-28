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

echo "Ожидается: 201 Created (собака без прививки)"
curl -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Unvaxxed","type":"DOG","ownerName":"Bad","vaccinated":false}'

echo "Ожидается: 409 Conflict (собаке нужна прививка)"
curl -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"3","vetName":"Dr.House","dateTime":"2026-12-01T10:00:00","durationMinutes":30}'

echo "Ожидается: 201 Created (кошка без прививки)"
curl -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Murka","type":"CAT","ownerName":"Alice","vaccinated":false}'

echo "Ожидается: 201 Created (запись кошки)"
curl -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"4","vetName":"Dr.House","dateTime":"2026-12-01T10:00:00","durationMinutes":30}'

echo "Ожидается: 409 Conflict (врач занят)"
curl -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.House","dateTime":"2026-12-01T10:00:00","durationMinutes":30}'

echo "Ожидается: 200 OK (завершить прием)"
curl -i -X POST $BASE/appointments/1/complete

echo "Ожидается: 409 Conflict (нельзя отменить завершенный)"
curl -i -X POST $BASE/appointments/1/cancel

echo "Ожидается: 200 OK (только кошки)"
curl -i "$BASE/pets?type=CAT"

echo "Ожидается: 200 OK (только вакцинированные)"
curl -i "$BASE/pets?vaccinated=true"

echo "Ожидается: 200 OK (пустой массив, т.к. все приемы завершены)"
curl -i "$BASE/appointments?status=SCHEDULED"

echo "Ожидается: 200 OK (записи к конкретному врачу)"
curl -i "$BASE/appointments?vetName=Dr.House"