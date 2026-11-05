package Comportamientos;

import Interfaz.Reproductor;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

public class CGestor extends SimpleBehaviour {
    private Reproductor ventana;
    private Agent agente;
    private int paso = 0;
    private long tiempoLimite;

    public CGestor(Agent agente, Reproductor ventana) {
        super(agente);
        this.agente = agente;
        this.ventana = ventana;

        // El boton "Nueva consulta" le pide al Perfilador que abra otra vez el formulario.
        this.ventana.setAlNuevaConsulta(() -> {
            ACLMessage pedido = new ACLMessage(ACLMessage.REQUEST);
            pedido.addReceiver(new AID("AgentePerfilador", AID.ISLOCALNAME));
            pedido.setContent("nueva-consulta");
            this.agente.send(pedido);
        });
    }

    // Vuelve a esperar un perfil; 
    private void consultaTerminada() {
        paso = 0;
        ventana.setNuevaConsultaHabilitada(true);
    }

    @Override
    public void action() {
        ACLMessage mensaje;
        mensaje = myAgent.receive();
        if (mensaje == null) {
            if (paso == 1) {
                long tiempoActual = System.currentTimeMillis();
                long tiempoRestante = tiempoLimite - tiempoActual;
                if (tiempoRestante <= 0) {
                    ventana.mostrarError("El catalogo no respondio en 30 segundos.");
                    consultaTerminada();
                } else {
                    block(tiempoRestante);
                }
            } else {
                block();
            }
            return;
        }
        String emisor;
        emisor = mensaje.getSender().getLocalName();
        int tipo;
        tipo = mensaje.getPerformative();

        if (paso == 0) {
            if (emisor.equals("AgentePerfilador")) {
                if (tipo == ACLMessage.INFORM) {
                    String perfil;
                    perfil = mensaje.getContent();
                    if (perfil == null) {
                        ventana.mostrarError("No se recibio el perfil.");
                        return;
                    }
                    String[] preferencias;
                    preferencias = perfil.split(",", -1);
                    if (preferencias.length != 5) {
                        ventana.mostrarError("El perfil debe contener cinco preferencias.");
                        return;
                    }
                    for (int i = 0; i < preferencias.length; i++) {
                        if (preferencias[i].trim().isEmpty()) {
                            ventana.mostrarError("Una preferencia esta vacia.");
                            return;
                        }
                    }
                    ventana.mostrarPerfil(perfil);
                    ACLMessage solicitud;
                    solicitud = new ACLMessage(ACLMessage.REQUEST);
                    AID catalogo;
                    catalogo = new AID("AgenteCatalogo", AID.ISLOCALNAME);
                    solicitud.addReceiver(catalogo);
                    // Se envian LAS CINCO preferencias sin quitar ningun campo.
                    solicitud.setContent(perfil);
                    myAgent.send(solicitud);
                    ventana.mostrarEstado("Buscando playlist...");
                    ventana.setNuevaConsultaHabilitada(false);
                    System.out.println("Perfil enviado al catalogo: " + perfil);
                    tiempoLimite = System.currentTimeMillis() + 30000;
                    paso = 1;
                }
            }
            return;
        }
        if (paso == 1) {
            if (emisor.equals("AgenteCatalogo")) {
                if (tipo == ACLMessage.INFORM) {
                    String playlist;
                    playlist = mensaje.getContent();
                    if (playlist == null) {
                        ventana.mostrarError("La playlist esta vacia.");
                    } else {
                        if (playlist.trim().isEmpty()) {
                            ventana.mostrarError("La playlist esta vacia.");
                        } else {
                            ventana.mostrarCancion(playlist);
                            System.out.println("Playlist recibida del catalogo.");
                        }
                    }
                    consultaTerminada();
                }
                if (tipo == ACLMessage.REFUSE) {
                    String motivo = mensaje.getContent();
                    ventana.mostrarError(motivo);
                    consultaTerminada();
                }
                if (tipo == ACLMessage.FAILURE) {
                    String motivo = mensaje.getContent();
                    ventana.mostrarError(motivo);
                    consultaTerminada();
                }
            }
        }
    }

    @Override
    public boolean done() {
        return false;
    }
}