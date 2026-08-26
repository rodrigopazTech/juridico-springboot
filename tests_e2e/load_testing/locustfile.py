"""
Escenario de Carga y Concurrencia para Sistema Jurídico SIAJ (Fase 3)
Asigna cuentas 1-a-1 desde una cola thread-safe para que ningún usuario exceda su cuota de concurrencia.
"""
import queue
import random
from locust import HttpUser, task, between, events

# 15 Usuarios únicos del sistema
USER_ACCOUNTS = [
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
    ("subdirector", "12345"),
    ("qa.subdirector", "12345"),
    ("director.principal", "12345"),
    ("admin", "admin123"),
    ("qa.admin", "12345"),
]

user_queue = queue.Queue()
for acc in USER_ACCOUNTS:
    user_queue.put(acc)

SEARCH_KEYWORDS = ["022", "2024", "Civil", "Amparo", "Laboral", "023"]


class ConcurrentSystemUser(HttpUser):
    wait_time = between(0.05, 0.25)

    def on_start(self):
        try:
            self.username, self.password = user_queue.get_nowait()
        except queue.Empty:
            self.username, self.password = ("admin", "admin123")

        self.client.post("/perform_login", data={"username": self.username, "password": self.password})

    def on_stop(self):
        if hasattr(self, "username"):
            user_queue.put((self.username, self.password))

    @task(6)
    def view_dashboard(self):
        self.client.get("/dashboard", name="GET /dashboard")

    @task(6)
    def search_expedientes(self):
        kw = random.choice(SEARCH_KEYWORDS)
        self.client.get(f"/expedientes?keyword={kw}", name="GET /expedientes?keyword")

    @task(4)
    def view_audiencias(self):
        self.client.get("/audiencias", name="GET /audiencias")

    @task(4)
    def view_terminos(self):
        self.client.get("/terminos", name="GET /terminos")

    @task(2)
    def view_catalogos(self):
        self.client.get("/catalogos", name="GET /catalogos")
