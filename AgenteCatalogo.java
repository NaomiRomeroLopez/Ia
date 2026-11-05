package Agentes;

import jade.core.Agent;
import Comportamientos.CCatalogo;

public class AgenteCatalogo extends Agent {
    @Override
    protected void setup() {
        addBehaviour(new CCatalogo(this));
    }
}
