#!/bin/bash
set -e

echo "=================================================="
echo "🚀 Iniciando Entorno de Desarrollo y Pruebas"
echo "=================================================="

# 1. Levantar Docker Compose
echo "📦 1. Levantando infraestructura con Docker Compose..."
docker-compose up -d

echo "⏳ Esperando a que los servicios estén listos (15s)..."
sleep 15

# 2. Importar y activar Workflow en n8n
echo "🤖 2. Importando flujo de trabajo en n8n..."
# Importamos el flujo y reiniciamos el contenedor para aplicar los cambios si es necesario
docker exec n8n-local n8n import:workflow --input=/home/node/.n8n/transaction-workflow.json || true
docker exec n8n-local n8n publish:workflow --id=1 || true
echo "🔄 Reiniciando n8n para aplicar el workflow..."
docker restart n8n-local
echo "⏳ Esperando a que n8n reinicie con el workflow activo (15s)..."
sleep 15

# 3. Correr pruebas automatizadas
echo "🧪 3. Ejecutando pruebas unitarias y de integración..."
./gradlew test

# 4. Iniciar servicio Spring Boot
echo "🍃 4. Iniciando servicio Spring Boot en segundo plano..."
# Nos aseguramos de detener cualquier instancia previa en el puerto 8080 si existiera (opcional)
lsof -i tcp:8080 | awk 'NR!=1 {print $2}' | xargs -r kill -9 || true

./gradlew build -x test
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar > build/bootRun.log 2>&1 &
SPRING_PID=$!

echo "⏳ Esperando a que Spring Boot inicie (15s)..."
sleep 15

# 5. Cargar datos / Ejecutar prueba completa
echo "✉️  5. Enviando transacción de prueba a la API de Spring Boot..."
curl -s -X POST http://localhost:8080/api/transactions \
     -H "Content-Type: application/json" \
     -d '{
           "id": "123e4567-e89b-12d3-a456-426614174000",
           "accountId": "account-test-1",
           "amount": 250.50,
           "status": "USD"
         }'
echo -e "\n✅ Transacción enviada."

echo "⏳ Esperando a que el flujo asíncrono termine (Kafka -> Lambda -> n8n -> DB) (10s)..."
sleep 10

# 6. Validar datos en la base de datos (Prueba de n8n)
echo "🔍 6. Validando que n8n guardó la transacción en PostgreSQL..."
RESULT=$(docker exec postgres-local psql -U admin -d transactions_db -t -c "SELECT count(*) FROM transactions WHERE id = '123e4567-e89b-12d3-a456-426614174000';")

# Limpiar espacios en blanco
RESULT=$(echo $RESULT | tr -d ' ')

if [ "$RESULT" -eq "1" ]; then
    echo "✅ ¡ÉXITO! La transacción se procesó correctamente por el Webhook de n8n y se guardó en PostgreSQL."
else
    echo "❌ ¡ERROR! La transacción no fue encontrada en la base de datos."
    echo "Revisa los logs en build/bootRun.log o en http://localhost:5678 (n8n)"
fi

echo "=================================================="
echo "🎉 Entorno levantado y validado correctamente."
echo "   - Spring Boot PID: $SPRING_PID (Ejecutándose en puerto 8080)"
echo "   - n8n UI: http://localhost:5678"
echo "   - Grafana UI: http://localhost:3000"
echo "   - Logs Spring: build/bootRun.log"
echo "=================================================="
echo "Para detener la aplicación de Spring Boot, ejecuta: kill $SPRING_PID"
