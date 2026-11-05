//#COMPORTAMIENTO PERFILADOR
package Comportamientos;

import Interfaz.FormularioPerfil;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.SwingUtilities;

// Abre el formulario al iniciar y cada vez que el Gestor pide una nueva consulta.
public class CPerfilador extends CyclicBehaviour {

    private final Agent agente;
    private boolean primeraVez = true;
    private FormularioPerfil formulario;

    public CPerfilador(Agent a) {
        super(a);
        this.agente = a;
    }

    @Override
    public void action() {
        if (primeraVez) {
            primeraVez = false;
            abrirFormulario();
        }

        ACLMessage pedido = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        if (pedido == null) {
            block();
            return;
        }

        abrirFormulario();
    }

    private void abrirFormulario() {
        SwingUtilities.invokeLater(() -> {
            // Si ya hay un formulario abierto, solo se trae al frente.
            if (formulario != null && formulario.isDisplayable()) {
                formulario.toFront();
                return;
            }
            formulario = new FormularioPerfil(this::enviarPerfil);
            formulario.setVisible(true);
        });
    }

    private void enviarPerfil(String perfilEmpaquetado) {
        System.out.println("\nPerfil generado: " + perfilEmpaquetado);

        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
        mensaje.addReceiver(new AID("AgenteGestor", AID.ISLOCALNAME));
        mensaje.setContent(perfilEmpaquetado);
        agente.send(mensaje);

        System.out.println("Mensaje enviado al Agente Gestor exitosamente.");
    }
}