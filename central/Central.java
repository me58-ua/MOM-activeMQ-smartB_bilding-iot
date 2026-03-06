package central;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;


public class Central {
    private static final String url = "tcp://localhost:61616";
    
    private static final double TEMP_MAX    = 25.0;  // activate cooling above this
    private static final double TEMP_MIN    = 18.0;  // activate heating below this
    private static final double LIGHT_MAX   = 1000.0; // dim lights above this
    private static final double LIGHT_MIN   = 200.0;
    
    // topics Sensors (oficina → central)
    private static final String O1_TEMP_SENSOR  = "oficina1.sensors.temperature";
    private static final String O1_LIGHT_SENSOR = "oficina1.sensors.lighting";
    private static final String O2_TEMP_SENSOR  = "oficina2.sensors.temperature";
    private static final String O2_LIGHT_SENSOR = "oficina2.sensors.lighting";

    // topics Actuators (central → oficina)
    private static final String O1_TEMP_ACT     = "oficina1.actuators.temperature";
    private static final String O1_LIGHT_ACT    = "oficina1.actuators.lighting";
    private static final String O2_TEMP_ACT     = "oficina2.actuators.temperature";
    private static final String O2_LIGHT_ACT    = "oficina2.actuators.lighting";

    private static Map<String, Double> sensorValues = new ConcurrentHashMap<>();
    private static Map<String, String> lastCommand = new ConcurrentHashMap<>();
    
    private static final String RED   = "\u001B[31m";
    private static final String RESET = "\u001B[0m";
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
            Thread.sleep(1500);
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
                String texto = null;

                if (mensaje instanceof TextMessage) {
                    texto = ((TextMessage) mensaje).getText();
                } else if (mensaje instanceof BytesMessage) {
                    BytesMessage bm = (BytesMessage) mensaje;
                    byte[] bytes = new byte[(int) bm.getBodyLength()];
                    bm.readBytes(bytes);
                    texto = new String(bytes);
                }

                if (texto != null) {
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
            if (valor > max)       comando = "ACTIVAR_FRIO";
            else if (valor < min)  comando = "ACTIVAR_CALOR";
            else                   comando = "DESACTIVAR";
        } else if (tipo.equals("ILUMINACION")) {
            if (valor > max)       comando = "REDUCIR_ILUMINACION";
            else if (valor < min)  comando = "AUMENTAR_ILUMINACION";
            else                   comando = "DESACTIVAR";
        }

        // solo si el comando ha cambiado
        String lastCmd = lastCommand.get(etiqueta);
        if (comando.equals(lastCmd)) return; // silent, no print

        lastCommand.put(etiqueta, comando);

        // Print only si cambia
        if (tipo.equals("TEMPERATURA")) {
            System.out.println("[" + etiqueta + "] Temperatura=" + valor + "°C -> " + comando);
        } else {
            System.out.println("[" + etiqueta + "] Iluminación=" + valor + " lux -> " + comando);
        }

        TextMessage msg = sesion.createTextMessage(comando);
        productor.send(msg);
        System.out.println(RED + ">>> Comando enviado al actuador [" + etiqueta + "]: " + comando + RESET);
    }

}