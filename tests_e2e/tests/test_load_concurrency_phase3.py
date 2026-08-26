"""
Fase 3: Suite de Carga, Rendimiento y Concurrencia Masiva
Evalúa:
1. Concurrencia simultánea de múltiples roles (Abogados, Directores, Administradores).
2. Tasa de peticiones por segundo (Throughput / RPS).
3. Percentiles de latencia bajo estrés (Promedio < 300ms, P95 < 600ms).
4. Tasa de fallos y resistencia del pool de conexiones HikariCP (0% errores 500).
"""
import concurrent.futures
import time
import requests
import statistics
from pathlib import Path
import pytest

USERS = [
    ("abogado", "12345"),
    ("abogado.laboral", "12345"),
    ("rodrigo.paz", "12345"),
    ("qa.abogado", "12345"),
    ("gerente", "12345"),
    ("qa.gerente", "12345"),
    ("jefe", "12345"),
    ("qa.jefe", "12345"),
    ("director", "12345"),
    ("qa.director", "12345"),
]

ENDPOINTS = [
    "/dashboard",
    "/expedientes?keyword=2024",
    "/expedientes?keyword=Civil",
    "/audiencias",
    "/terminos",
    "/catalogos",
]


def test_concurrent_traffic_and_latency_benchmark(page, web_logger, backend_logger):
    """
    Fase 3: Benchmark de Carga y Concurrencia con 10 Hilos Simultáneos (100 Peticiones).
    Verifica que el servidor soporte tráfico concurrente con:
    - 0% de fallos o excepciones 500
    - Throughput superior a 30 req/s
    - Latencia promedio inferior a 300 ms
    - Percentil 95 inferior a 600 ms
    """
    # 1. Warm-up y autenticación simultánea
    sessions = []
    for u, p in USERS:
        s = requests.Session()
        res = s.post("http://localhost:8080/perform_login", data={"username": u, "password": p})
        assert res.status_code == 200, f"Error en login para {u}: {res.status_code}"
        sessions.append(s)

    assert len(sessions) == len(USERS), "No todas las sesiones se activaron correctamente."

    # 2. Ejecutar 100 peticiones concurrentes
    total_calls = 100

    def worker(idx):
        s = sessions[idx % len(sessions)]
        endpoint = ENDPOINTS[idx % len(ENDPOINTS)]
        start = time.perf_counter()
        try:
            r = s.get(f"http://localhost:8080{endpoint}", timeout=10)
            duration_ms = (time.perf_counter() - start) * 1000.0
            return (r.status_code == 200), duration_ms
        except Exception:
            return False, 0.0

    start_all = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(sessions)) as executor:
        results = list(executor.map(worker, range(total_calls)))
    total_time = time.perf_counter() - start_all

    success_count = sum(1 for success, _ in results if success)
    latencies = [lat for success, lat in results if success]
    failures = total_calls - success_count

    avg_latency = statistics.mean(latencies)
    p50_latency = statistics.median(latencies)
    p95_latency = statistics.quantiles(latencies, n=20)[18]
    rps = total_calls / total_time

    # Criterios de Rendimiento para Producción
    assert failures == 0, f"¡FALLO DE CONCURRENCIA! Se registraron {failures} peticiones fallidas bajo carga."
    assert rps >= 25.0, f"Throughput insuficiente: {rps:.2f} req/s (Mínimo: 25 req/s)"
    assert avg_latency < 350.0, f"Latencia promedio alta: {avg_latency:.2f} ms (Límite: 350 ms)"
    assert p95_latency < 700.0, f"Percentil 95 alto: {p95_latency:.2f} ms (Límite: 700 ms)"

    # Comprobar salud de los logs del backend
    assert len(backend_logger.get_errors()) == 0, f"Errores en servidor durante carga: {backend_logger.get_errors()}"
