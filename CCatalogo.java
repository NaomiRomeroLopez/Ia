package Comportamientos;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

public class CCatalogo extends CyclicBehaviour {

    // Guarda todas las columnas de cada canción.
    private List<String[]> cancionesFMA = new ArrayList<>();

    private String errorCarga = null;

    public CCatalogo(Agent agente) {
        super(agente);
        cargarBaseDatos();
    }

    @Override
    public void action() {

        ACLMessage mensaje;
        mensaje = myAgent.receive();

        if (mensaje == null) {
            block();
            return;
        }

        ACLMessage respuesta;
        respuesta = mensaje.createReply();

        String perfil;
        perfil = mensaje.getContent();

        String[] datosPerfil;
        datosPerfil = perfil.split(",", -1);

        if (datosPerfil.length != 5) {
            respuesta.setPerformative(ACLMessage.REFUSE);
            respuesta.setContent(
                "El perfil debe contener las cinco preferencias."
            );

            myAgent.send(respuesta);
            return;
        }

        // Obtener las cinco preferencias.
        String generoBuscado = datosPerfil[0].trim();
        String idiomaBuscado = datosPerfil[1].trim();
        String popularidadBuscada = datosPerfil[2].trim();
        String duracionBuscada = datosPerfil[3].trim();
        String emocionBuscada = datosPerfil[4].trim();

        // Revisar las opciones recibidas.
        boolean perfilValido = true;

        if (generoBuscado.isEmpty()) {
            perfilValido = false;
        }

        if (idiomaBuscado.isEmpty()) {
            perfilValido = false;
        }

        if (!popularidadBuscada.equalsIgnoreCase("popular")
                && !popularidadBuscada.equalsIgnoreCase("underground")
                && !popularidadBuscada.equalsIgnoreCase("cualquiera")) {
            perfilValido = false;
        }

        if (!duracionBuscada.equalsIgnoreCase("cortas")
                && !duracionBuscada.equalsIgnoreCase("promedio")
                && !duracionBuscada.equalsIgnoreCase("largas")
                && !duracionBuscada.equalsIgnoreCase("cualquiera")) {
            perfilValido = false;
        }

        if (!emocionBuscada.equalsIgnoreCase("feliz")
                && !emocionBuscada.equalsIgnoreCase("tranquilo")
                && !emocionBuscada.equalsIgnoreCase("triste")
                && !emocionBuscada.equalsIgnoreCase("intenso")
                && !emocionBuscada.equalsIgnoreCase("cualquiera")) {
            perfilValido = false;
        }

        if (!perfilValido) {
            respuesta.setPerformative(ACLMessage.REFUSE);
            respuesta.setContent("El perfil contiene opciones inválidas.");

            myAgent.send(respuesta);
            return;
        }

        System.out.println("Perfil recibido: " + perfil);

        StringBuilder playlist;
        playlist = new StringBuilder();

        int encontradas = 0;

        for (int i = 0; i < cancionesFMA.size(); i++) {

            String[] cancion;
            cancion = cancionesFMA.get(i);

            // Columnas del canciones.tsv.
            String id = cancion[0];
            String titulo = cancion[1];
            String artista = cancion[2];
            String genero = cancion[3];
            String idioma = cancion[4];
            String escuchasTexto = cancion[5];
            String duracionTexto = cancion[6];
            String emocion = cancion[7];

            // 1. Género.
            boolean coincideGenero;

            coincideGenero = genero.equalsIgnoreCase(generoBuscado);

            if (generoBuscado.equalsIgnoreCase("cualquiera")) {
                coincideGenero = true;
            }

            // 2. Idioma.
            boolean coincideIdioma;

            coincideIdioma = idioma.equalsIgnoreCase(idiomaBuscado);

            if (idiomaBuscado.equalsIgnoreCase("cualquiera")) {
                coincideIdioma = true;
            }

            // 3. Popularidad, calculada con las escuchas.
            boolean coincidePopularidad = false;

            if (popularidadBuscada.equalsIgnoreCase("cualquiera")) {

                coincidePopularidad = true;

            } else {

                try {
                    long escuchas;
                    escuchas = Long.parseLong(escuchasTexto);

                    if (escuchas >= 0) {

                        if (popularidadBuscada.equalsIgnoreCase("popular")) {
                            coincidePopularidad = escuchas >= 1000;
                        }

                        if (popularidadBuscada.equalsIgnoreCase("underground")) {
                            coincidePopularidad = escuchas < 1000;
                        }
                    }

                } catch (NumberFormatException e) {
                    // Si falta el dato, no cumple este filtro.
                    coincidePopularidad = false;
                }
            }

            // 4. Duración, expresada en segundos.
            boolean coincideDuracion = false;

            if (duracionBuscada.equalsIgnoreCase("cualquiera")) {

                coincideDuracion = true;

            } else {

                try {
                    double segundos;
                    segundos = Double.parseDouble(duracionTexto);

                    if (Double.isFinite(segundos) && segundos > 0) {

                        if (duracionBuscada.equalsIgnoreCase("cortas")) {
                            coincideDuracion = segundos < 180;
                        }

                        if (duracionBuscada.equalsIgnoreCase("promedio")) {
                            coincideDuracion = segundos >= 180
                                    && segundos <= 300;
                        }

                        if (duracionBuscada.equalsIgnoreCase("largas")) {
                            coincideDuracion = segundos > 300;
                        }
                    }

                } catch (NumberFormatException e) {
                    coincideDuracion = false;
                }
            }

            // 5. Emoción.
            boolean coincideEmocion;

            coincideEmocion = emocion.equalsIgnoreCase(emocionBuscada);

            if (emocionBuscada.equalsIgnoreCase("cualquiera")) {
                coincideEmocion = true;
            }

            // Debe cumplir los cinco criterios.
            if (coincideGenero
                    && coincideIdioma
                    && coincidePopularidad
                    && coincideDuracion
                    && coincideEmocion) {

                encontradas++;

                // Una canción por línea para mostrarla en la interfaz.
                playlist.append(encontradas);
                playlist.append(". ");
                playlist.append(titulo);
                playlist.append(" - ");
                playlist.append(artista);

                playlist.append(" | ID: ");
                playlist.append(id);

                playlist.append(" | Género: ");
                playlist.append(genero);

                playlist.append(" | Idioma: ");
                playlist.append(idioma);

                playlist.append(" | Escuchas: ");
                playlist.append(escuchasTexto);

                playlist.append(" | Duración: ");
                playlist.append(duracionTexto);
                playlist.append(" s");

                playlist.append(" | Emoción: ");
                playlist.append(emocion);

                playlist.append("\n");
            }
        }

        if (encontradas > 0) {

            respuesta.setPerformative(ACLMessage.INFORM);
            respuesta.setContent(playlist.toString());

            System.out.println("Canciones encontradas: " + encontradas);

        } else {

            respuesta.setPerformative(ACLMessage.REFUSE);
            respuesta.setContent(
                "No se encontraron canciones que cumplan las cinco preferencias."
            );
        }

        myAgent.send(respuesta);
    }

    private void cargarBaseDatos() {

        cancionesFMA.clear();
        errorCarga = null;

        System.out.println("Leyendo data/canciones.tsv...");

        try (BufferedReader lector = new BufferedReader(new InputStreamReader(new FileInputStream("src/data/canciones.tsv"), StandardCharsets.UTF_8))) {
            // Este TSV tiene UNA fila de encabezados.
            String encabezado;
            encabezado = lector.readLine();

            if (encabezado == null) {
                throw new IllegalArgumentException("El archivo está vacío.");
            }

            encabezado = encabezado.replace("\uFEFF", "");

            String[] nombres;
            nombres = encabezado.split("\t", -1);

            String[] esperados = {
                "id",
                "titulo",
                "artista",
                "genero",
                "idioma",
                "escuchas",
                "duracion",
                "emocion"
            };

            String linea;

            while ((linea = lector.readLine()) != null) {

                if (linea.trim().isEmpty()) {
                    continue;
                }

                String[] columnas;

                // -1 conserva los campos vacíos, incluida la emoción.
                columnas = linea.split("\t", -1);

                for (int i = 0; i < columnas.length; i++) {
                    columnas[i] = columnas[i].trim();
                }

                cancionesFMA.add(columnas);
            }

            System.out.println(
                "Canciones cargadas: " + cancionesFMA.size()
            );

        } catch (Exception e) {

            cancionesFMA.clear();

            errorCarga = "No se pudo leer data/canciones.tsv: " + e.getMessage();

            System.out.println(errorCarga);
        }
    }
}