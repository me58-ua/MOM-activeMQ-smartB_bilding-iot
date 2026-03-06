package central;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;


public class Central {
    private static final String url = "tcp://localhost:61616";
    
    private static final double TEMP_MAX    = 30.0;  // activate cooling above this
    private static final double TEMP_MIN    = 18.0;  // activate heating below this
    private static final double LIGHT_MAX   = 800.0; // dim lights above this
    private static final double LIGHT_MIN   = 300.0;
    
    // topics Sensors (oficina → central)
    private static final String O1_TEMP_SENSOR  = "office1.sensors.temperature";
    private static final String O1_LIGHT_SENSOR = "office1.sensors.lighting";
    private static final String O2_TEMP_SENSOR  = "office2.sensors.temperature";
    private static final String O2_LIGHT_SENSOR = "office2.sensors.lighting";

    // topics Actuators (central → oficina)
    private static final String O1_TEMP_ACT     = "office1.actuators.temperature";
    private static final String O1_LIGHT_ACT    = "office1.actuators.lighting";
    private static final String O2_TEMP_ACT     = "office2.actuators.temperature";
    private static final String O2_LIGHT_ACT    = "office2.actuators.lighting";

    private static Map<String, Double> sensorValues = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception{
        ConnectionFactory factory = new ActiveMQConnectionFactory(url);
        Connection conn = factory.createConnection();

        conn.start();
        System.out.println("Central connectada to ActiveMQ");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { conn.close(); } catch (JMSException e) { e.printStackTrace(); }
        }));

        Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

        MessageProducer actTempO1  = session.createProducer(session.createTopic(O1_TEMP_ACT));
        MessageProducer actLuzO1 = session.createProducer(session.createTopic(O1_LIGHT_ACT));
        MessageProducer actTempO2  = session.createProducer(session.createTopic(O2_TEMP_ACT));
        MessageProducer actLuzO2 = session.createProducer(session.createTopic(O2_LIGHT_ACT));

        subscribeAsync(session, O1_TEMP_SENSOR,  "Oficina1-Temp");
        subscribeAsync(session, O1_LIGHT_SENSOR, "Oficina1-Luz");
        subscribeAsync(session, O2_TEMP_SENSOR,  "Oficina2-Temp");
        subscribeAsync(session, O2_LIGHT_SENSOR, "Oficina2-Luz");

        System.out.println("Subscibido a todos los topics.\n");

        while (true) {
            Thread.sleep(2000);
            System.out.println("Revisando estado de las oficinas...");

            comprobarYActuar(session, "Oficina1-Temperatura",  sensorValues.get("Oficina1-Temp"),
                    TEMP_MIN, TEMP_MAX, actTempO1, "TEMPERATURA");

            comprobarYActuar(session, "Oficina1-Iluminacion",  sensorValues.get("Oficina1-Luz"),
                    LIGHT_MIN, LIGHT_MAX, actLuzO1, "ILUMINACION");

            comprobarYActuar(session, "Oficina2-Temperatura",  sensorValues.get("Oficina2-Temp"),
                    TEMP_MIN, TEMP_MAX, actTempO2, "TEMPERATURA");

            comprobarYActuar(session, "Oficina2-Iluminacion",  sensorValues.get("Oficina2-Luz"),
                    LIGHT_MIN, LIGHT_MAX, actLuzO2, "ILUMINACION");
        }
    }

    // ── Suscripción asíncrona con MessageListener ────────────
    private static void subscribeAsync(Session sesion, String nombreTopic, String clave) throws JMSException {
        Destination topic = sesion.createTopic(nombreTopic);
        MessageConsumer consumidor = sesion.createConsumer(topic);
        consumidor.setMessageListener(mensaje -> {
            try {
                if (mensaje instanceof TextMessage) {
                    String texto = ((TextMessage) mensaje).getText();
                    double valor = Double.parseDouble(texto.trim());
                    sensorValues.put(clave, valor);
                    System.out.println("[" + clave + "] Valor recibido: " + valor);
                }
            } catch (Exception e) {
                System.out.println("Error al leer mensaje de " + clave + ": " + e.getMessage());
            }
        });
    }

    // ── Lógica de umbrales y envío de comandos ───────────────
    private static void comprobarYActuar(Session sesion, String etiqueta, Double valor,
            double min, double max, MessageProducer productor, String tipo) throws JMSException {

        if (valor == null) {
            System.out.println("[" + etiqueta + "] Sin datos todavía.");
            return;
        }

        String comando = null;

        if (tipo.equals("TEMPERATURA")) {
            if (valor > max) {
                comando = "ACTIVAR_FRIO";
                System.out.println("[" + etiqueta + "] Temperatura=" + valor + "°C > " + max + "°C → " + comando);
            } else if (valor < min) {
                comando = "ACTIVAR_CALOR";
                System.out.println("[" + etiqueta + "] Temperatura=" + valor + "°C < " + min + "°C → " + comando);
            } else {
                comando = "DESACTIVAR";
                System.out.println("[" + etiqueta + "] Temperatura=" + valor + "°C normal → " + comando);
            }
        } else if (tipo.equals("ILUMINACION")) {
            if (valor > max) {
                comando = "REDUCIR_ILUMINACION";
                System.out.println("[" + etiqueta + "] Iluminación=" + valor + " lux > " + max + " → " + comando);
            } else if (valor < min) {
                comando = "AUMENTAR_ILUMINACION";
                System.out.println("[" + etiqueta + "] Iluminación=" + valor + " lux < " + min + " → " + comando);
            } else {
                comando = "DESACTIVAR";
                System.out.println("[" + etiqueta + "] Iluminación=" + valor + " lux normal → " + comando);
            }
        }

        // Enviar comando al topic del actuador
        TextMessage msg = sesion.createTextMessage(comando);
        productor.send(msg);
        System.out.println("Comando enviado al actuador [" + etiqueta + "]: " + comando);
    }
}