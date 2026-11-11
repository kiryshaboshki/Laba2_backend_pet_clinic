#!/usr/bin/env bash

BASE=http://localhost:8080

echo "=== 1. ПРОВЕРКА PING И ПУТЕЙ ==="

echo "Ожидается: 200 OK"
curl -s -i $BASE/ping

echo -e "\nОжидается: 404 Not Found (несуществующий подпуть ping)"
curl -s -i $BASE/ping-extra

echo -e "\nОжидается: 405 Method Not Allowed (POST на /ping)"
curl -s -i -X POST $BASE/ping


echo -e "\n=== 2. СОЗДАНИЕ ПИТОМЦЕВ И ВАЛИДАЦИЯ ==="

echo "Ожидается: 201 Created (кошка Barsik, непривита)"
curl -s -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Barsik","type":"CAT","ownerName":"Alice","vaccinated":false}'

echo -e "\nОжидается: 201 Created (собака Rex, привита)"
curl -s -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Rex","type":"DOG","ownerName":"Bob","vaccinated":true}'

echo -e "\nОжидается: 201 Created (собака Unvaxxed, непривита)"
curl -s -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Unvaxxed","type":"DOG","ownerName":"Bad","vaccinated":false}'

echo -e "\nОжидается: 400 Bad Request (пустое имя питомца)"
curl -s -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"","type":"CAT","ownerName":"Alice"}'

echo -e "\nОжидается: 400 Bad Request (пустое имя владельца)"
curl -s -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Murka","type":"CAT","ownerName":""}'

echo -e "\nОжидается: 400 Bad Request (недопустимый тип COW)"
curl -s -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Burenka","type":"COW","ownerName":"Farmer"}'


echo -e "\n=== 3. ПОЛУЧЕНИЕ И ОБНОВЛЕНИЕ ПИТОМЦЕВ ==="

echo "Ожидается: 200 OK (список всех питомцев)"
curl -s -i $BASE/pets

echo -e "\nОжидается: 404 Not Found (несуществующий префикс /pets-test)"
curl -s -i $BASE/pets-test

echo -e "\nОжидается: 200 OK (получить питомца 1)"
curl -s -i $BASE/pets/1

echo -e "\nОжидается: 404 Not Found (несуществующий питомец 999)"
curl -s -i $BASE/pets/999

echo -e "\nОжидается: 200 OK (обновление питомца 1)"
curl -s -i -X PUT $BASE/pets/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Barsik","type":"CAT","ownerName":"Alice Smith","vaccinated":true}'

echo -e "\nОжидается: 404 Not Found (обновление несуществующего питомца)"
curl -s -i -X PUT $BASE/pets/999 \
  -H "Content-Type: application/json" \
  -d '{"name":"Ghost","type":"CAT","ownerName":"Unknown","vaccinated":false}'

echo -e "\nОжидается: 204 No Content (удаление питомца 2 без записей)"
curl -s -i -X DELETE $BASE/pets/2

echo -e "\nОжидается: 404 Not Found (повторное удаление питомца 2)"
curl -s -i -X DELETE $BASE/pets/2

echo -e "\nОжидается: 405 Method Not Allowed (PATCH не поддерживается)"
curl -s -i -X PATCH $BASE/pets/1


echo -e "\n=== 4. ФИЛЬТРАЦИЯ ПИТОМЦЕВ ==="

echo "Ожидается: 200 OK (только кошки)"
curl -s -i "$BASE/pets?type=CAT"

echo -e "\nОжидается: 200 OK (только вакцинированные)"
curl -s -i "$BASE/pets?vaccinated=true"


echo -e "\n=== 5. СОЗДАНИЕ ЗАПИСЕЙ И БИЗНЕС-ПРАВИЛА ==="

echo "Ожидается: 409 Conflict (собаке без прививки petId 3 запись запрещена)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"3","vetName":"Dr.House","dateTime":"2026-12-01T10:00:00","durationMinutes":60}'

echo -e "\nОжидается: 404 Not Found (запись несуществующего питомца 999)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"999","vetName":"Dr.House","dateTime":"2026-12-01T10:00:00","durationMinutes":60}'

echo -e "\nОжидается: 400 Bad Request (пустое имя врача)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"","dateTime":"2026-12-01T10:00:00","durationMinutes":60}'

echo -e "\nОжидается: 400 Bad Request (дата в прошлом)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.House","dateTime":"2020-01-01T10:00:00","durationMinutes":60}'

echo -e "\nОжидается: 400 Bad Request (отрицательная длительность)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.House","dateTime":"2026-12-01T10:00:00","durationMinutes":-15}'

echo -e "\nОжидается: 201 Created (успешная запись кошки Barsik к Dr.House: 10:00-11:00)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.House","dateTime":"2026-12-01T10:00:00","durationMinutes":60}'


echo -e "\n=== 6. ПРОВЕРКА ПЕРЕСЕЧЕНИЯ ИНТЕРВАЛОВ ПРИЁМА ВРАЧА ==="

echo "Ожидается: 409 Conflict (пересечение внутри: 10:30-11:00 к Dr.House)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.House","dateTime":"2026-12-01T10:30:00","durationMinutes":30}'

echo -e "\nОжидается: 409 Conflict (пересечение с началом раньше: 09:30-10:30 к Dr.House)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.House","dateTime":"2026-12-01T09:30:00","durationMinutes":60}'

echo -e "\nОжидается: 201 Created (то же время 10:00-10:30, но к ДРУГОМУ врачу Dr.Watson)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.Watson","dateTime":"2026-12-01T10:00:00","durationMinutes":30}'

echo -e "\nОжидается: 201 Created (запись к Dr.House встык: 11:00-11:30 без пересечения)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.House","dateTime":"2026-12-01T11:00:00","durationMinutes":30}'


echo -e "\n=== 7. ЦЕЛОСТНОСТЬ ДАННЫХ: БЛОКИРОВКА УДАЛЕНИЯ ПРИ АКТИВНЫХ ЗАПИСЯХ ==="

echo "Ожидается: 409 Conflict (нельзя удалить питомца 1, пока есть активные записи SCHEDULED)"
curl -s -i -X DELETE $BASE/pets/1


echo -e "\n=== 8. ВАЛИДАЦИЯ ПРИ ОБНОВЛЕНИИ (PUT /appointments/{id}) ==="

echo "Ожидается: 200 OK (корректное обновление времени записи 2 к Dr.Watson на 12:00)"
curl -s -i -X PUT $BASE/appointments/2 \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.Watson","dateTime":"2026-12-01T12:00:00","durationMinutes":45}'

echo -e "\nОжидается: 400 Bad Request (обновление с датой в прошлом)"
curl -s -i -X PUT $BASE/appointments/2 \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.Watson","dateTime":"2020-01-01T12:00:00","durationMinutes":45}'

echo -e "\nОжидается: 400 Bad Request (обновление с пустым именем врача)"
curl -s -i -X PUT $BASE/appointments/2 \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"","dateTime":"2026-12-01T12:00:00","durationMinutes":45}'

echo -e "\nОжидается: 409 Conflict (перенос записи 2 на время, занятое Dr.House: 10:15)"
curl -s -i -X PUT $BASE/appointments/2 \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.House","dateTime":"2026-12-01T10:15:00","durationMinutes":30}'


echo -e "\n=== 9. ЖИЗНЕННЫЙ ЦИКЛ ЗАПИСЕЙ (COMPLETE И CANCEL) ==="

echo "Ожидается: 200 OK (завершение приёма 1)"
curl -s -i -X POST $BASE/appointments/1/complete

echo -e "\nОжидается: 409 Conflict (повторное завершение уже завершённого приёма)"
curl -s -i -X POST $BASE/appointments/1/complete

echo -e "\nОжидается: 409 Conflict (нельзя отменить завершённый приём)"
curl -s -i -X POST $BASE/appointments/1/cancel

echo -e "\nОжидается: 409 Conflict (нельзя обновить завершённый приём)"
curl -s -i -X PUT $BASE/appointments/1 \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.House","dateTime":"2026-12-05T12:00:00","durationMinutes":30}'

echo -e "\nОжидается: 200 OK (отмена приёма 2)"
curl -s -i -X POST $BASE/appointments/2/cancel

echo -e "\nОжидается: 409 Conflict (повторная отмена уже отменённого приёма)"
curl -s -i -X POST $BASE/appointments/2/cancel

echo -e "\nОжидается: 409 Conflict (нельзя завершить отменённый приём)"
curl -s -i -X POST $BASE/appointments/2/complete

echo -e "\nОжидается: 201 Created (запись на время отменённого приёма разрешена - слот свободен)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"1","vetName":"Dr.Watson","dateTime":"2026-12-01T12:00:00","durationMinutes":30}'


echo -e "\n=== 10. ФИЛЬТРАЦИЯ И УДАЛЕНИЕ ЗАПИСЕЙ ==="

echo "Ожидается: 200 OK (фильтрация по статусу SCHEDULED)"
curl -s -i "$BASE/appointments?status=SCHEDULED"

echo -e "\nОжидается: 400 Bad Request (недопустимый статус)"
curl -s -i "$BASE/appointments?status=UNKNOWN"

echo -e "\nОжидается: 200 OK (записи к Dr.Watson)"
curl -s -i "$BASE/appointments?vetName=Dr.Watson"

echo -e "\nОжидается: 200 OK (пустой список, врача Dr.Unknown нет)"
curl -s -i "$BASE/appointments?vetName=Dr.Unknown"

echo -e "\nОжидается: 404 Not Found (несуществующий префикс /appointments-wrong)"
curl -s -i $BASE/appointments-wrong

echo -e "\nОжидается: 204 No Content (удаление записи 4)"
curl -s -i -X DELETE $BASE/appointments/4

echo -e "\nОжидается: 404 Not Found (повторное удаление записи 4)"
curl -s -i -X DELETE $BASE/appointments/4


echo -e "\n=== 11. ПРОВЕРКА ЗАЩИТЫ ОТ ОБХОДА ВАКЦИНАЦИИ ЧЕРЕЗ PUT /pets/{id} ==="

echo "Ожидается: 201 Created (создаём привитую собаку Bobik, id=4)"
curl -s -i -X POST $BASE/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Bobik","type":"DOG","ownerName":"Oleg","vaccinated":true}'

echo -e "\nОжидается: 201 Created (записываем Bobik к Dr.House: 15:00-15:30)"
curl -s -i -X POST $BASE/appointments \
  -H "Content-Type: application/json" \
  -d '{"petId":"4","vetName":"Dr.House","dateTime":"2026-12-02T15:00:00","durationMinutes":30}'

echo -e "\nОжидается: 409 Conflict (попытка сделать собаку непривитой при активной записи)"
curl -s -i -X PUT $BASE/pets/4 \
  -H "Content-Type: application/json" \
  -d '{"name":"Bobik","type":"DOG","ownerName":"Oleg","vaccinated":false}'


echo -e "\n=== 12. ПРОВЕРКА КАСКАДНОЙ ОЧИСТКИ ПРИ УДАЛЕНИИ ПИТОМЦА ==="

echo "Ожидается: 200 OK (отменяем запись 3 для Barsik, теперь у него только завершенные/отмененные записи)"
curl -s -i -X POST $BASE/appointments/3/cancel

echo -e "\nОжидается: 204 No Content (удаление Barsik, id=1, со всеми завершенными/отмененными записями)"
curl -s -i -X DELETE $BASE/pets/1

echo -e "\nОжидается: 404 Not Found (питомец 1 удален)"
curl -s -i $BASE/pets/1

echo -e "\nОжидается: 404 Not Found (запись 1 каскадно удалена, нет битой ссылки)"
curl -s -i $BASE/appointments/1

echo -e "\nОжидается: 404 Not Found (запись 3 каскадно удалена, нет битой ссылки)"
curl -s -i $BASE/appointments/3

echo -e "\n=== ВСЕ ТЕСТЫ ЗАВЕРШЕНЫ ==="
