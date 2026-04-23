¡Llegó la hora de la verdad! Vamos a hacer un recorrido paso a paso (End-to-End) probando cada una de las piezas que construimos:

Fase 1: Validar las Pruebas y Cobertura (CI/CD)
En tu terminal principal, ejecuta tu suite oficial (la misma que corre en GitHub Actions):

bash

./gradlew clean build

Validarás que todo compile en azul/verde y el archivo build/reports/jacoco/test/html/index.html refleje el >90% de cobertura. (Recuerda, esto debe terminar exitoso sin levantar servidores, gracias al Mock contextual que añadimos antes).
Fase 2: Levantar el Ecosistema Completo
Inicia tu Infraestructura Base (Kafka, Loki, Grafana):

bash

docker-compose up -d

Arranca tu Aplicación Spring Boot (En la terminal #1):
bash

./gradlew bootRun

Arranca el Monitor de Git (En la terminal #2):
bash

python3 .scripts/monitor_git_merges.py

$$$$$$$$$ KAFKA $$$$$$$$$$$$$$$
Fase 3: Probar la API y Kafka (Broker Queue)
Vamos a engañar al sistema creando una transacción falsa por medio del endpoint que creamos, y veremos como Kafka la recibe en el Queue (Tópico).

Abre una tercera terminal u hoja bash para hacer una petición al API (o hazlo vía Postman):

bash

curl -i -X POST http://localhost:8080/api/transactions \
     -H "Content-Type: application/json" \
     -d '{"id":"b0b14c33-8fba-4d92-9571-0ae1f1e31af1", "accountId":"CUENTA-XY", "amount": 8900.50, "status":"ACTIVE"}'

curl -i -X POST http://localhost:8080/api/transactions -H "Content-Type: application/json" -d '{"id":"b0b14c33-8fba-4d92-9571-0ae1f1e31af1", "accountId":"CUENTA-XY", "amount": 8900.50, "status":"ACTIVE"}'


Debe responder con un HTTP 202 Accepted.


Verifícalo directo desde dentro del contenedor de Kafka: Para asomarte al queue e interceptar el mensaje via CLI de Kafka ejecuta:

bash

docker exec -it kafka-local rpk topic consume transactions-topic

¡Allí deberías ver el JSON serializado de tu transacción llegando milisegundos después de recibir el HTTP POST! Ctrl+C para salir de ahí.

Response : 
 docker exec -it kafka-local rpk topic consume transactions-topic
{
  "topic": "transactions-topic",
  "key": "b0b14c33-8fba-4d92-9571-0ae1f1e31af1",
  "value": "{\"id\":\"b0b14c33-8fba-4d92-9571-0ae1f1e31af1\",\"accountId\":\"CUENTA-XY\",\"amount\":8900.50,\"status\":\"ACTIVE\"}",
  "headers": [
    {
      "key": "__TypeId__",
      "value": "com.example.demo.TransactionDto"
    }
  ],
  "timestamp": 1776979166583,
  "partition": 0,
  "offset": 0
}


########## PUSH AND MERGES ##########

Fase 4: Probar los Merges (Push/Merge Monitor)
El script de Python está actualmente observando si ocurren merges formales que se hagan push hacia la rama base en origin/feature/xxxx. Simularemos uno:

Crea o ingresa a esa rama en tu local y haz un push inicial:

bash

git checkout -b feature/xxxx
git push -u origin feature/xxxx

Ahora, simulemos un merge intencional forzándolo localmente y empujándolo hacia el origin. (Ojo, el script reacciona ante un nuevo Push que contenga 2 ancestros):

bash


# Crearemos un commit vacío para poder forzar el ambiente de un merge
git commit --allow-empty -m "Dummy commit 1"
git checkout -b temp/testing-branch
git commit --allow-empty -m "Dummy commit 2"
# Regresa a la principal del monitor y fusiona "sin omitir" el historial para que genere el parentesco dual (el verdadero Merge)
git checkout feature/xxxx
git merge temp/testing-branch --no-ff -m "Testing real merge trigger for Python script"
# ¡Dispara a remote!
git push origin feature/xxxx

$$$$$$$$$$$$$$ logs $$$$$$$$$$$$$

Mira la Terminal #2 de tu script Python. ¡Deberías ver cómo inmediatamente imprime que interceptó un nuevo hash, reconoce que era un Merge y notifica => Log publicado en Grafana Loki de forma exitosa!
Fase 5: Explotar y Visualizar Grafana

Entra a tu navegador web a la dirección: http://localhost:3000
Dirígete a la pestaña visual de compás en el panel izquierdo (🧭 Explore).
Asegúrate de que el Data Source en el selector superior diga Loki.
En la barra de expresiones superior (Label filters), introduce la etiqueta mágica que programamos por defecto y dale enter: {job="git-monitor"}
Dale clic al botón azul grande "Run Query" de la esquina superior derecha.
¡En consola inferior verás los Logs oficiales con detalles indicando el Merge exitoso y los sellos de tiempo exactos!