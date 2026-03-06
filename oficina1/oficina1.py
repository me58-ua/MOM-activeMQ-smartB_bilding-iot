import stomp
import time
import random

HOST = "localhost"
PORT = 61613

TEMP_SENSOR  = "/topic/oficina1.sensors.temperature"
LIGHT_SENSOR = "/topic/oficina1.sensors.lighting"
TEMP_ACT     = "/topic/oficina1.actuators.temperature"
LIGHT_ACT    = "/topic/oficina1.actuators.lighting"

# Default 
current_temp_cmd = "DESACTIVAR"
current_light_cmd = "DESACTIVAR"

class ActuatorListener(stomp.ConnectionListener):
    def on_message(self, frame):
        global current_temp_cmd, current_light_cmd
        topic = frame.headers.get("destination", "")
        cmd = frame.body.strip()
        print(f"[Actuador recibido en {topic}]: {cmd}")

        if "temperature" in topic:
            current_temp_cmd = cmd
        elif "lighting" in topic:
            current_light_cmd = cmd

def main():
    conn = stomp.Connection([(HOST, PORT)])
    conn.set_listener("", ActuatorListener())
    conn.connect(login="admin", passcode="admin", wait=True)

    conn.subscribe(destination=TEMP_ACT,  id=1, ack="auto")
    conn.subscribe(destination=LIGHT_ACT, id=2, ack="auto")

    print("Oficina 1 (Python) conectada.\n")

    #valores inicales
    temp  = 22.0
    light = 550.0

    while True:
        # Temperature adjustment based on actuator command
        if current_temp_cmd == "ACTIVAR_FRIO":
            temp = round(temp - random.uniform(1.0, 2.0), 2)   
        elif current_temp_cmd == "ACTIVAR_CALOR":
            temp = round(temp + random.uniform(1.0, 2.0), 2)   
        else:
            temp = round(temp + random.uniform(-2.5, 2.5), 2)  

        # Light adjustment based on actuator command
        if current_light_cmd == "REDUCIR_ILUMINACION":
            light = round(light - random.uniform(50.0, 100.0), 2)   
        elif current_light_cmd == "AUMENTAR_ILUMINACION":
            light = round(light + random.uniform(50.0, 100.0), 2)   
        else:
            light = round(light + random.uniform(-250.0, 250.0), 2)  

        
        temp  = max(10.0, min(40.0, temp))
        light = max(100.0, min(1200.0, light))

        conn.send(
            destination=TEMP_SENSOR,
            body=str(temp),
            headers={"content-type": "text/plain",
                     "transformation": "jms-text"}
        )
        conn.send(
            destination=LIGHT_SENSOR,
            body=str(light),
            headers={"content-type": "text/plain",
                     "transformation": "jms-text"}
        )

        print(f"[Sensor enviado] Temp={temp}°C  Luz={light} lux")
        time.sleep(2)

if __name__ == "__main__":
    main()
