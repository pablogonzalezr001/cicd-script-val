#!/usr/bin/env python3
import time
import subprocess
import logging
import json
import urllib.request
import urllib.error
import sys
import os

# Configuracion
# ===============
BRANCH_PREFIX = "feature/" 
CHECK_INTERVAL_SEC = 10
LOG_FILE = "merges.local.log"
GRAFANA_LOKI_URL = os.getenv("GRAFANA_LOKI_URL", "http://localhost:3100/loki/api/v1/push")

# Configurar logger local
logging.basicConfig(
    filename=LOG_FILE,
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
# Mostrar info tambien por consola
console = logging.StreamHandler()
console.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)

def fetch_and_get_branches():
    """
    Refresca silenciosamente el historio remoto de las ramas "feature/" 
    y retorna un diccionario en formato { 'origin/feature/ui': 'hash5678', ... }
    """
    try:
        # Fetchear todas las ramas del bloque (wildcard) y acoplarlas a refs locales
        ref_spec = f"+refs/heads/{BRANCH_PREFIX}*:refs/remotes/origin/{BRANCH_PREFIX}*"
        subprocess.run(["git", "fetch", "origin", ref_spec], capture_output=True, check=True)
        
        # Listar todas las refs que guardamos
        result = subprocess.run(
            ["git", "for-each-ref", "--format=%(refname:short) %(objectname)", f"refs/remotes/origin/{BRANCH_PREFIX}"], 
            capture_output=True, check=True, text=True
        )
        
        branches = {}
        for line in result.stdout.strip().split("\n"):
            if not line:
                continue
            parts = line.split(" ")
            if len(parts) == 2:
                branch_name, branch_hash = parts[0], parts[1]
                branches[branch_name] = branch_hash
        return branches
            
    except subprocess.CalledProcessError as e:
        # Sucederá si el origin no existe
        return {}

def is_merge_commit(commit_hash):
    """
    Descubre si un commit es un 'merge' analizando si tiene más de un padre.
    """
    try:
        result = subprocess.run(["git", "show", "--no-patch", "--format=%P", commit_hash], capture_output=True, check=True, text=True)
        parents = result.stdout.strip().split()
        return len(parents) > 1
    except subprocess.CalledProcessError as e:
        logging.error(f"Error comprobando el commit {commit_hash}: {e}")
        return False

def push_to_grafana_loki(commit_hash, branch_name):
    # Loki requiere un timestamp en nanosegundos como string
    nano_timestamp = str(int(time.time() * 1e9))
    
    # Limpiamos parte de la semantica origin/ para comodidad visual en Grafana
    clean_branch = branch_name.replace("origin/", "") if branch_name.startswith("origin/") else branch_name
    
    message = f"Nuevo merge detectado en {clean_branch}! Hash: {commit_hash[:8]}"
    
    payload = {
        "streams": [
            {
                "stream": {
                    "job": "git-monitor",
                    "branch": clean_branch
                },
                "values": [
                    [ nano_timestamp, message ]
                ]
            }
        ]
    }
    
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(
        GRAFANA_LOKI_URL, 
        data=data, 
        headers={'Content-Type': 'application/json'}
    )
    
    try:
        urllib.request.urlopen(req, timeout=5)
        logging.info(f"=> Log de {clean_branch} publicado en Grafana Loki de forma exitosa.")
    except urllib.error.URLError as e:
        logging.error(f"=> Falla al conectar con Grafana Loki en {GRAFANA_LOKI_URL}: {e}")

def main():
    logging.info(f"Iniciando monitor Global de Git para multiples ramas '{BRANCH_PREFIX}*' cada {CHECK_INTERVAL_SEC} s...")
    logging.info(f"Logs dirigidos a Loki: {GRAFANA_LOKI_URL}")
    
    # Snapshot inicial
    tracked_branches = fetch_and_get_branches()
    if tracked_branches:
        logging.info(f"Monitoreando activamente las siguientes {len(tracked_branches)} ramas:")
        for branch, hash_val in tracked_branches.items():
            logging.info(f" -> {branch} (Hash: {hash_val[:8]})")
    else:
        logging.warning(f"No se detectaron amas con el patrón '{BRANCH_PREFIX}' en el origin remoto... A la espera.")
    
    while True:
        time.sleep(CHECK_INTERVAL_SEC)
        
        current_branches = fetch_and_get_branches()
        
        for branch_name, current_hash in current_branches.items():
            # ¿Es una rama completamente nueva que acaban de empujar, o avanzó su hash?
            if branch_name not in tracked_branches or tracked_branches[branch_name] != current_hash:
                
                if branch_name not in tracked_branches:
                    logging.info(f"Nueva rama detectada: {branch_name}")
                else:
                    logging.info(f"Actividad detectada en {branch_name} (Hash: {current_hash[:8]})")
                
                # Revisar si se inyectó con un Merge
                if is_merge_commit(current_hash):
                    logging.info(f"¡El commit de {branch_name} es oficialmente un MERGE! Escribiendo eventos...")
                    push_to_grafana_loki(current_hash, branch_name)
                else:
                    logging.info(f"Commits regulares progresando en {branch_name} (No es merge)")
                
                # Guardamos su nuevo estado
                tracked_branches[branch_name] = current_hash

if __name__ == "__main__":
    main()
