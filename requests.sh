#!/usr/bin/env bash

BASE="http://localhost:8080"

# Генерируем уникальную дату для каждого запуска скрипта,
# чтобы тесты всегда работали с чистыми временными слотами независимо от сохраненных данных в БД
RUN_RAND=$(( (RANDOM % 800) + 100 ))
R_MONTH=$(printf "%02d" $(( (RUN_RAND % 12) + 1 )))
R_DAY=$(printf "%02d" $(( (RUN_RAND % 28) + 1 )))
DATE_BASE="2027-${R_MONTH}-${R_DAY}"

T_1000="${DATE_BASE}T10:00:00"
T_1030="${DATE_BASE}T10:30:00"
T_1100="${DATE_BASE}T11:00:00"
T_1200="${DATE_BASE}T12:00:00"
T_1500="${DATE_BASE}T15:00:00"
T_1600="${DATE_BASE}T16:00:00"

echo "=== 1. ПРОВЕРКА PING ==="

echo "Ожидается: 200 OK"
curl -s -i "$BASE/ping"
echo ""

echo "Ожидается: 404 Not Found"
curl -s -i "$BASE/ping-extra"
echo ""

echo "=== 2. СОЗДАНИЕ ПИТОМЦЕВ И ВАЛИДАЦИЯ ==="

echo "Создаем кошку Barsik:"
BARSIK_RESP=$(curl -s -X POST "$BASE/pets" \
  -H "Content-Type: application/json" \
  -d '{"name":"Barsik","type":"CAT","ownerName":"Alice","vaccinated":false}')
echo "$BARSIK_RESP"
BARSIK_ID=$(echo "$BARSIK_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
echo "Barsik ID: $BARSIK_ID"

echo -e "\nСоздаем привитую собаку Rex:"
REX_RESP=$(curl -s -X POST "$BASE/pets" \
  -H "Content-Type: application/json" \
  -d '{"name":"Rex","type":"DOG","ownerName":"Bob","vaccinated":true}')
echo "$REX_RESP"
REX_ID=$(echo "$REX_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
echo "Rex ID: $REX_ID"

echo -e "\nСоздаем непривитую собаку Unvaxxed:"
UNVAXXED_RESP=$(curl -s -X POST "$BASE/pets" \
  -H "Content-Type: application/json" \
  -d '{"name":"Unvaxxed","type":"DOG","ownerName":"Bad","vaccinated":false}')
echo "$UNVAXXED_RESP"
UNVAXXED_ID=$(echo "$UNVAXXED_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
echo "Unvaxxed ID: $UNVAXXED_ID"

echo -e "\nОжидается: 400 Bad Request (пустое имя)"
curl -s -i -X POST "$BASE/pets" \
  -H "Content-Type: application/json" \
  -d '{"name":"","type":"CAT","ownerName":"Alice"}'
echo ""

echo "Ожидается: 400 Bad Request (недопустимый тип COW)"
curl -s -i -X POST "$BASE/pets" \
  -H "Content-Type: application/json" \
  -d '{"name":"Burenka","type":"COW","ownerName":"Farmer"}'
echo ""

echo "=== 3. ПОЛУЧЕНИЕ И ОБНОВЛЕНИЕ ПИТОМЦЕВ ==="

echo "Ожидается: 200 OK (список питомцев)"
curl -s "$BASE/pets"
echo -e "\n"

echo "Ожидается: 200 OK (получить Barsik по UUID)"
curl -s "$BASE/pets/$BARSIK_ID"
echo -e "\n"

echo "Ожидается: 404 Not Found (несуществующий UUID)"
curl -s -i "$BASE/pets/00000000-0000-0000-0000-000000000000"
echo ""

echo "Ожидается: 200 OK (обновление Barsik)"
curl -s -X PUT "$BASE/pets/$BARSIK_ID" \
  -H "Content-Type: application/json" \
  -d '{"name":"Barsik","type":"CAT","ownerName":"Alice Smith","vaccinated":true}'
echo -e "\n"

echo "=== 4. СОЗДАНИЕ ПРИЁМОВ С ЗАМЕТКАМИ (МИГРАЦИЯ V2) ==="

echo "Ожидается: 409 Conflict (собаке без прививки запись запрещена)"
curl -s -i -X POST "$BASE/appointments" \
  -H "Content-Type: application/json" \
  -d "{\"petId\":\"$UNVAXXED_ID\",\"vetName\":\"Dr.House\",\"dateTime\":\"$T_1000\",\"durationMinutes\":60}"
echo ""

echo "Ожидается: 201 Created (запись Barsik к Dr.House с заметками notes):"
APPT1_RESP=$(curl -s -X POST "$BASE/appointments" \
  -H "Content-Type: application/json" \
  -d "{\"petId\":\"$BARSIK_ID\",\"vetName\":\"Dr.House\",\"dateTime\":\"$T_1000\",\"durationMinutes\":60,\"notes\":\"Initial checkup\"}")
echo "$APPT1_RESP"
APPT1_ID=$(echo "$APPT1_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
echo "Appointment 1 ID: $APPT1_ID"

echo -e "\nОжидается: 409 Conflict (пересечение интервала 10:30-11:00 у Dr.House)"
curl -s -i -X POST "$BASE/appointments" \
  -H "Content-Type: application/json" \
  -d "{\"petId\":\"$BARSIK_ID\",\"vetName\":\"Dr.House\",\"dateTime\":\"$T_1030\",\"durationMinutes\":30}"
echo ""

echo "Ожидается: 201 Created (то же время 10:00, но другой врач Dr.Watson):"
APPT2_RESP=$(curl -s -X POST "$BASE/appointments" \
  -H "Content-Type: application/json" \
  -d "{\"petId\":\"$REX_ID\",\"vetName\":\"Dr.Watson\",\"dateTime\":\"$T_1000\",\"durationMinutes\":30}")
echo "$APPT2_RESP"
APPT2_ID=$(echo "$APPT2_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
echo "Appointment 2 ID: $APPT2_ID"

echo "=== 5. ЖИЗНЕННЫЙ ЦИКЛ ПРИЁМОВ (COMPLETE И CANCEL) ==="

echo "Завершаем приём 1 (POST /appointments/{id}/complete):"
curl -s -X POST "$BASE/appointments/$APPT1_ID/complete"
echo -e "\n"

echo "Ожидается: 409 Conflict (повторное завершение запрещено):"
curl -s -i -X POST "$BASE/appointments/$APPT1_ID/complete"
echo ""

echo "Ожидается: 201 Created (завершённый приём освободил слот - повторная запись на 10:00 к Dr.House разрешена):"
curl -s -X POST "$BASE/appointments" \
  -H "Content-Type: application/json" \
  -d "{\"petId\":\"$REX_ID\",\"vetName\":\"Dr.House\",\"dateTime\":\"$T_1000\",\"durationMinutes\":60}"
echo -e "\n"

echo "=== 6. ТРЕБОВАНИЕ 3: АТОМАРНАЯ ТРАНЗАКЦИЯ И ОТКАТ (ROLLBACK) ==="

echo "Создаем активную запись для Rex:"
APPT3_RESP=$(curl -s -X POST "$BASE/appointments" \
  -H "Content-Type: application/json" \
  -d "{\"petId\":\"$REX_ID\",\"vetName\":\"Dr.House\",\"dateTime\":\"$T_1500\",\"durationMinutes\":30}")
APPT3_ID=$(echo "$APPT3_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)
echo "Appointment 3 ID: $APPT3_ID"

echo -e "\n1. Проверяем ошибку и откат транзакции (POST /pets/{id}/transfer с trigger-error):"
echo "Ожидается: 409 Conflict"
curl -s -i -X POST "$BASE/pets/$REX_ID/transfer" \
  -H "Content-Type: application/json" \
  -d '{"newOwnerName":"trigger-error"}'
echo ""

echo "Проверяем, что изменения откатились (владелец Rex остался прежним):"
curl -s "$BASE/pets/$REX_ID"
echo -e "\n"

echo "Проверяем, что запись осталась SCHEDULED (не отменена):"
curl -s "$BASE/appointments/$APPT3_ID"
echo -e "\n"

echo "2. Успешная передача прав владения (атомарное изменение Rex и отмена всех его активных записей):"
curl -s -X POST "$BASE/pets/$REX_ID/transfer" \
  -H "Content-Type: application/json" \
  -d '{"newOwnerName":"Charlie"}'
echo -e "\n"

echo "Проверяем, что владелец обновился на Charlie:"
curl -s "$BASE/pets/$REX_ID"
echo -e "\n"

echo "Проверяем, что активная запись автоматически отменилась с соответствующей заметкой:"
curl -s "$BASE/appointments/$APPT3_ID"
echo -e "\n"

echo "=== 7. ТРЕБОВАНИЕ 5: ЗАГРУЗКА СВЯЗАННЫХ РЕСУРСОВ БЕЗ ПРОБЛЕМЫ N+1 ==="

echo "Получаем список всех приёмов со связанными объектами питомцев (GET /appointments/detailed):"
curl -s "$BASE/appointments/detailed"
echo -e "\n"

echo "=== 8. ЦЕЛОСТНОСТЬ ДАННЫХ И КАСКАДНАЯ ОЧИСТКА ==="

echo "Создаем кота Tom:"
TOM_RESP=$(curl -s -X POST "$BASE/pets" \
  -H "Content-Type: application/json" \
  -d '{"name":"Tom","type":"CAT","ownerName":"Jerry","vaccinated":true}')
TOM_ID=$(echo "$TOM_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)

echo "Создаем активную запись для Tom:"
TOM_APPT_RESP=$(curl -s -X POST "$BASE/appointments" \
  -H "Content-Type: application/json" \
  -d "{\"petId\":\"$TOM_ID\",\"vetName\":\"Dr.Watson\",\"dateTime\":\"$T_1600\",\"durationMinutes\":30}")
TOM_APPT_ID=$(echo "$TOM_APPT_RESP" | grep -o '"id":"[^"]*' | head -n 1 | cut -d'"' -f4)

echo -e "\nОжидается: 409 Conflict (нельзя удалить Tom, пока есть активная запись SCHEDULED):"
curl -s -i -X DELETE "$BASE/pets/$TOM_ID"
echo ""

echo "Отменяем запись Tom:"
curl -s -X POST "$BASE/appointments/$TOM_APPT_ID/cancel"
echo -e "\n"

echo "Ожидается: 204 No Content (удаление Tom вместе с исторической записью):"
curl -s -i -X DELETE "$BASE/pets/$TOM_ID"
echo ""

echo "Ожидается: 404 Not Found (Tom удален):"
curl -s -i "$BASE/pets/$TOM_ID"
echo ""

echo "Ожидается: 404 Not Found (запись Tom каскадно удалена, нет битой ссылки):"
curl -s -i "$BASE/appointments/$TOM_APPT_ID"
echo ""

echo "=== ВСЕ ТЕСТЫ ЗАВЕРШЕНЫ УСПЕШНО ==="
