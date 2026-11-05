package Agentes;

import jade.core.Agent;
import Comportamientos.CPerfilador;

public class AgentePerfilador extends Agent {
    @Override
    protected void setup() {addBehaviour(new CPerfilador(this));}
}
