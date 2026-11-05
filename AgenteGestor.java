
package Agentes;

import Comportamientos.CGestor;
import Interfaz.Reproductor;
import jade.core.Agent;

public class AgenteGestor extends Agent {
    private Reproductor ventana;

    @Override
    protected void setup() {

        System.out.println("Agente Gestor iniciado...");

        ventana = new Reproductor();

        ventana.setVisible(true);

        addBehaviour(new CGestor(this, ventana));
    }

}
