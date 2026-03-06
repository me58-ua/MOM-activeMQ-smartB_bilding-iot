const stompit = require("stompit");

const connectOptions = {
    host: "localhost",
    port: 61613,
    connectHeaders: {
        host: "/",
        login: "admin",
        passcode: "admin",
        "heart-beat": "0,0"
    }
};

const TEMP_SENSOR  = "/topic/oficina2.sensors.temperature";
const LIGHT_SENSOR = "/topic/oficina2.sensors.lighting";
const TEMP_ACT     = "/topic/oficina2.actuators.temperature";
const LIGHT_ACT    = "/topic/oficina2.actuators.lighting";

let currentTempCmd  = "DESACTIVAR";
let currentLightCmd = "DESACTIVAR";

let temp  = 22.0;
let light = 550.0;

stompit.connect(connectOptions, (err, client) => {
    if (err) { console.error("Connection error:", err); return; }

    console.log("Oficina 2 (Node.js) conectada.\n");

    // Subscribe to actuator topics
    client.subscribe({ destination: TEMP_ACT, ack: "auto" }, (err, msg) => {
        msg.readString("utf-8", (err, body) => {
            const cmd = body.trim();
            console.log(`\x1b[31m[Actuador recibido en ${TEMP_ACT}]: ${cmd}\x1b[0m`);
            currentTempCmd = cmd;
        });
    });

    client.subscribe({ destination: LIGHT_ACT, ack: "auto" }, (err, msg) => {
        msg.readString("utf-8", (err, body) => {
            const cmd = body.trim();
            console.log(`\x1b[31m[Actuador recibido en ${LIGHT_ACT}]: ${cmd}\x1b[0m`);
            currentLightCmd = cmd;
        });
    });

    // envia datos cada 2 segundos
    setInterval(() => {
        // Temperature adjustment based on actuator command
        if (currentTempCmd === "ACTIVAR_FRIO") {
            temp += -(Math.random() * 1.0 + 1.0);
        } else if (currentTempCmd === "ACTIVAR_CALOR") {
            temp += (Math.random() * 1.0 + 1.0);
        } else {
            temp += (Math.random() * 5.0 - 2.5);
        }

        // Light adjustment based on actuator command
        if (currentLightCmd === "REDUCIR_ILUMINACION") {
            light -= (Math.random() * 50.0 + 50.0);
        } else if (currentLightCmd === "AUMENTAR_ILUMINACION") {
            light += (Math.random() * 50.0 + 50.0);
        } else {
            light += (Math.random() * 500.0 - 250.0);
        }

        // Clamp values
        temp  = Math.min(40.0, Math.max(10.0, temp));
        light = Math.min(1200.0, Math.max(100.0, light));

        temp  = Math.round(temp  * 100) / 100;
        light = Math.round(light * 100) / 100;

        // enviar via stomp
        const tempFrame = client.send({
            destination: TEMP_SENSOR,
            "content-type": "text/plain",
            transformation: "jms-text"
        });
        tempFrame.write(String(temp));
        tempFrame.end();

        const lightFrame = client.send({
            destination: LIGHT_SENSOR,
            "content-type": "text/plain",
            transformation: "jms-text"
        });
        lightFrame.write(String(light));
        lightFrame.end();

        console.log(`[Sensor enviado] Temp=${temp}°C  Luz=${light} lux`);
    }, 2000);
});
