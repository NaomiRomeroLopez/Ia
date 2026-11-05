
package Comportamientos;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class CGenerador extends CyclicBehaviour {

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive();

        if (msg != null) {
            if (msg.getPerformative() == ACLMessage.REQUEST) {

                ACLMessage agree = msg.createReply();
                agree.setPerformative(ACLMessage.AGREE);
                agree.setContent("Solicitud aceptada. Procesando la playlist...");
                myAgent.send(agree);

                String cancionesRecibidas = msg.getContent();

                ACLMessage respuestaFinal = msg.createReply();

                if (cancionesRecibidas != null && !cancionesRecibidas.trim().isEmpty()) {
                    respuestaFinal.setPerformative(ACLMessage.INFORM);
                    respuestaFinal.setContent("Playlist Creada Exitosamente: [" + cancionesRecibidas + "]");
                } else {
                    respuestaFinal.setPerformative(ACLMessage.REFUSE);
                    respuestaFinal.setContent("Rechazado: No se proporcionaron canciones válidas para la playlist.");
                }

                myAgent.send(respuestaFinal);
            }
        } else {
            block();
        }
    }
}