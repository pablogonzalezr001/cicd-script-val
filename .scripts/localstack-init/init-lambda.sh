#!/bin/bash
echo "Inicializando LocalStack para AWS Lambda..."

# Crear un directorio temporal para el código de la lambda
mkdir -p /tmp/lambda-code
cd /tmp/lambda-code

# Crear un archivo de python dummy que retorna 200 y el evento
cat <<EOF > index.py
import json
import urllib.request
import urllib.error

def handler(event, context):
    print("Received event: " + json.dumps(event))
    
    # URL del Webhook de n8n (el hostname n8n asume que están en la misma red de docker)
    # Como LocalStack se ejecuta en el host de Docker, podemos usar el host interno
    # Pero vamos a usar la IP/nombre de red si es posible. Para mayor seguridad, n8n es accesible en host.docker.internal desde localstack en Mac
    # Wait, in linux it's 172.x.x.x, in colima/docker for mac host.docker.internal works.
    # We will use host.docker.internal
    url = "http://host.docker.internal:5678/webhook/transaction"
    
    req = urllib.request.Request(url, data=json.dumps(event).encode('utf-8'), headers={'Content-Type': 'application/json'})
    
    try:
        response = urllib.request.urlopen(req)
        response_body = response.read().decode('utf-8')
        print("n8n response: " + response_body)
        return {
            "statusCode": 200,
            "body": json.dumps({"message": "Transaction forwarded to n8n successfully", "n8n_response": response_body})
        }
    except urllib.error.URLError as e:
        print("Error connecting to n8n: " + str(e))
        return {
            "statusCode": 500,
            "body": json.dumps({"message": "Failed to forward to n8n", "error": str(e)})
        }
EOF

# Empaquetar el código
zip function.zip index.py

# Crear la función lambda en LocalStack
awslocal lambda create-function \
    --function-name transaction-processor \
    --runtime python3.9 \
    --handler index.handler \
    --role arn:aws:iam::000000000000:role/lambda-role \
    --zip-file fileb://function.zip

echo "AWS Lambda dummy creada exitosamente."
