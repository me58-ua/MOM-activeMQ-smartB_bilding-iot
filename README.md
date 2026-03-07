# MOM-activeMQ-smartB_bilding-iot

## Overview
This project implements a smart building control system using **Apache ActiveMQ** as the MOM broker.
The goal is to practice asynchronous, decoupled communication between distributed systems using
the publish/subscribe messaging pattern.

The system monitors and controls **temperature and lighting** across 2 offices, simulating
real Arduino sensors and actuators via software.

## Architecture
<img width="709" height="234" alt="image" src="https://github.com/user-attachments/assets/2e14323f-89e1-4306-9c66-d96f0225fb21" />

- **Central (Java)**: Reads sensor data from both offices every second. If values exceed
  predefined thresholds, it sends commands to the corresponding actuators.
- **Oficina 1 (Python)**: Simulates temperature and lighting sensors. Publishes readings
  every 2 seconds and reacts to actuator commands by adjusting values gradually.
- **Oficina 2 (Node.js)**: Same logic as Oficina 1 but implemented in Node.js.

---

## Security

Authentication is enforced at both the **broker level** and **terminal level**:

- `activemq.xml` is configured with `simpleAuthenticationPlugin` — anonymous access is disabled and
  every connection must provide valid credentials.
- Each terminal (`Central`, `Oficina1`, `Oficina2`) prompts for a password on startup and will not
  connect until the correct password is entered.
- Credentials are loaded from a `config.properties` file (not hardcoded).

### Setup
Copy the example config and fill in your passwords:
```bash
cp config.example.properties config.properties
```
---

## Topics

| Topic | Direction | Description |
|---|---|---|
| `oficina1.sensors.temperature` | Oficina1 → Central | Temperature readings |
| `oficina1.sensors.lighting` | Oficina1 → Central | Lighting readings |
| `oficina1.actuators.temperature` | Central → Oficina1 | Temp actuator commands |
| `oficina1.actuators.lighting` | Central → Oficina1 | Light actuator commands |
| `oficina2.sensors.temperature` | Oficina2 → Central | Temperature readings |
| `oficina2.sensors.lighting` | Oficina2 → Central | Lighting readings |
| `oficina2.actuators.temperature` | Central → Oficina2 | Temp actuator commands |
| `oficina2.actuators.lighting` | Central → Oficina2 | Light actuator commands |

---

## Actuator Commands

| Command | Meaning |
|---|---|
| `ACTIVAR_FRIO` | Temperature too high → activate cooling |
| `ACTIVAR_CALOR` | Temperature too low → activate heating |
| `REDUCIR_ILUMINACION` | Light too high → dim lights |
| `AUMENTAR_ILUMINACION` | Light too low → increase lights |
| `DESACTIVAR` | Values back to normal → deactivate actuator |

---

## Thresholds (Central)

| Parameter | Min | Max |
|---|---|---|
| Temperature | 18°C | 25°C |
| Lighting | 200 lux | 1000 lux |

---
