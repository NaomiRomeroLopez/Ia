
package Agentes;

import Comportamientos.CGenerador;
import jade.core.Agent;

public class AgenteGenerador extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente Generador iniciado...");
        addBehaviour(new CGenerador());
    }
    
}