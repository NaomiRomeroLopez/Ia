//#INTERFAZ
package Interfaz;

import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Reproductor extends JFrame {

    // Carpeta donde esta fma_small (contiene 000/, 001/, ... con los .mp3).
    // Puede ser ruta absoluta, por ejemplo "C:/fma_small".
    public static final String RUTA_AUDIO = "Data/fma_small";

    // Cuantas canciones se muestran de todas las encontradas (se eligen al azar).
    public static final int MAX_CANCIONES = 10;

    // Los MP3 de fma_small son fragmentos de 30 s (la duracion del TSV es la de la cancion completa).
    public static final int DURACION_CLIP_SEG = 30;

    public static final Color COLOR_BG_BORDER = new Color(0xFAD2E1);
    public static final Color COLOR_CONTAINER = new Color(0xFFF0F5);
    public static final Color COLOR_CARD_BG = new Color(0xF8D7E3);
    public static final Color COLOR_BUTTON_BG = new Color(0xF3C0D3);
    public static final Color COLOR_TEXT_MAIN = new Color(0x70525C);
    public static final Color COLOR_TEXT_SUB = new Color(0x9E7885);
    public static final Color COLOR_ACCENT = new Color(0xE888A7);

    private static final Pattern PATRON_ID = Pattern.compile("\\| ID: (\\d+)");

    private final DefaultListModel<String> songModel = new DefaultListModel<>();
    private final JTextArea detalle = new JTextArea();
    private final JLabel estado = new JLabel("Esperando perfil...");

    private JList<String> songList;
    private JLabel songName;
    private JLabel artistName;
    private JLabel currentTime;
    private JLabel totalTime;
    private JProgressBar progressBar;
    private JButton btnPrev;
    private JButton btnPlay;
    private JButton btnNext;
    private Timer timer;
    private JButton btnNueva;
    private Runnable alNuevaConsulta;

    // Datos paralelos a songModel.
    private final List<String> ids = new ArrayList<>();
    private final List<Double> duraciones = new ArrayList<>();

    // Estado de reproduccion (solo se toca desde el hilo de Swing).
    private AdvancedPlayer player;
    private int indiceActual = -1;
    private boolean reproduciendo = false;
    private long acumuladoMs = 0;
    private long marcaMs = 0;
    private int sesion = 0;

    public Reproductor() {

        setTitle("Reproductor");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(750, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(COLOR_BG_BORDER);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("  Reproductor  🎀", JLabel.LEFT);
        titleLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        titleLabel.setForeground(COLOR_TEXT_MAIN);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        estado.setForeground(COLOR_TEXT_MAIN);

        btnNueva = createStyledButton("Nueva consulta 🎀");
        btnNueva.setFont(new Font("Monospaced", Font.BOLD, 12));
        btnNueva.addActionListener(e -> {
            if (alNuevaConsulta != null) {
                alNuevaConsulta.run();
            }
        });

        JPanel estadoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        estadoPanel.setOpaque(false);
        estadoPanel.add(estado);
        estadoPanel.add(btnNueva);
        headerPanel.add(estadoPanel, BorderLayout.EAST);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        centerPanel.setOpaque(false);

        JPanel artPanel = new JPanel(new BorderLayout());
        artPanel.setBackground(COLOR_CONTAINER);
        artPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BUTTON_BG, 2, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        detalle.setEditable(false);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        detalle.setBackground(COLOR_CONTAINER);
        detalle.setForeground(COLOR_TEXT_MAIN);
        detalle.setText("Completa el formulario de perfil musical.");
        artPanel.add(new JScrollPane(detalle), BorderLayout.CENTER);

        JPanel playlistPanel = new JPanel(new BorderLayout(5, 5));
        playlistPanel.setBackground(COLOR_CONTAINER);
        playlistPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BUTTON_BG, 2, true),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel playlistTitle = new JLabel("Playlist 🎵  (doble clic para reproducir)");
        playlistTitle.setFont(new Font("Monospaced", Font.BOLD, 12));
        playlistTitle.setForeground(COLOR_TEXT_MAIN);
        playlistTitle.setBorder(new EmptyBorder(0, 0, 5, 0));

        songList = new JList<>(songModel);
        songList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        songList.setBackground(COLOR_CARD_BG);
        songList.setForeground(COLOR_TEXT_MAIN);
        songList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        songList.setSelectionBackground(COLOR_ACCENT);
        songList.setSelectionForeground(Color.WHITE);
        songList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int i = songList.locationToIndex(e.getPoint());
                    if (i >= 0) {
                        reproducir(i, 0);
                    }
                }
            }
        });

        JScrollPane scrollList = new JScrollPane(songList);
        scrollList.setBorder(BorderFactory.createEmptyBorder());

        playlistPanel.add(playlistTitle, BorderLayout.NORTH);
        playlistPanel.add(scrollList, BorderLayout.CENTER);

        centerPanel.add(artPanel);
        centerPanel.add(playlistPanel);

        JPanel playerPanel = new JPanel(new BorderLayout(10, 10));
        playerPanel.setBackground(COLOR_CONTAINER);
        playerPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BUTTON_BG, 2, true),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JPanel songInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        songInfoPanel.setOpaque(false);

        JLabel albumThumb = new JLabel("♪");
        albumThumb.setPreferredSize(new Dimension(40, 40));
        albumThumb.setOpaque(true);
        albumThumb.setBackground(COLOR_BUTTON_BG);
        albumThumb.setForeground(COLOR_TEXT_MAIN);
        albumThumb.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel textInfoPanel = new JPanel(new GridLayout(2, 1));
        textInfoPanel.setOpaque(false);
        textInfoPanel.setPreferredSize(new Dimension(230, 40));

        songName = new JLabel("Resultado del catálogo");
        songName.setFont(new Font("Monospaced", Font.BOLD, 14));
        songName.setForeground(COLOR_TEXT_MAIN);

        artistName = new JLabel("Consulta por cinco preferencias");
        artistName.setFont(new Font("Monospaced", Font.PLAIN, 12));
        artistName.setForeground(COLOR_TEXT_SUB);

        textInfoPanel.add(songName);
        textInfoPanel.add(artistName);

        songInfoPanel.add(albumThumb);
        songInfoPanel.add(textInfoPanel);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        controlsPanel.setOpaque(false);

        btnPrev = createStyledButton("⏮");
        btnPlay = createStyledButton("▶");
        btnNext = createStyledButton("⏭");

        setControles(false);

        btnPrev.addActionListener(e -> anterior());
        btnPlay.addActionListener(e -> alternarPausa());
        btnNext.addActionListener(e -> siguiente());

        controlsPanel.add(btnPrev);
        controlsPanel.add(btnPlay);
        controlsPanel.add(btnNext);

        JPanel progressPanel = new JPanel(new BorderLayout(10, 0));
        progressPanel.setOpaque(false);

        currentTime = new JLabel("0:00");
        currentTime.setFont(new Font("Monospaced", Font.PLAIN, 10));
        currentTime.setForeground(COLOR_TEXT_SUB);

        totalTime = new JLabel("0:00");
        totalTime.setFont(new Font("Monospaced", Font.PLAIN, 10));
        totalTime.setForeground(COLOR_TEXT_SUB);

        progressBar = new JProgressBar(0, 1000);
        progressBar.setValue(0);
        progressBar.setForeground(COLOR_ACCENT);
        progressBar.setBackground(COLOR_CARD_BG);
        progressBar.setBorderPainted(false);

        progressPanel.add(currentTime, BorderLayout.WEST);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(totalTime, BorderLayout.EAST);

        JPanel topPlayerRow = new JPanel(new BorderLayout());
        topPlayerRow.setOpaque(false);
        topPlayerRow.add(songInfoPanel, BorderLayout.WEST);
        topPlayerRow.add(controlsPanel, BorderLayout.CENTER);

        playerPanel.add(topPlayerRow, BorderLayout.NORTH);
        playerPanel.add(progressPanel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(playerPanel, BorderLayout.SOUTH);

        add(mainPanel);

        timer = new Timer(250, e -> actualizarProgreso());
    }

    // ---------------- Metodos llamados desde el hilo de JADE ----------------

    public void mostrarPerfil(String perfil) {
        SwingUtilities.invokeLater(() -> detalle.setText("Perfil recibido:\n" + perfil));
    }

    // El agente Gestor registra aqui lo que debe pasar al pulsar "Nueva consulta".
    public void setAlNuevaConsulta(Runnable accion) {
        this.alNuevaConsulta = accion;
    }

    public void setNuevaConsultaHabilitada(boolean habilitada) {
        SwingUtilities.invokeLater(() -> btnNueva.setEnabled(habilitada));
    }

    public void mostrarEstado(String texto) {
        SwingUtilities.invokeLater(() -> estado.setText(texto));
    }

    public void mostrarCancion(String registro) {
        SwingUtilities.invokeLater(() -> {
            detenerAudio();
            reproduciendo = false;
            acumuladoMs = 0;
            indiceActual = -1;
            btnPlay.setText("▶");

            songModel.clear();
            ids.clear();
            duraciones.clear();

            List<String> todas = new ArrayList<>();
            for (String l : registro.split("\n")) {
                if (!l.trim().isEmpty()) {
                    todas.add(l);
                }
            }
            int totalEncontradas = todas.size();
            Collections.shuffle(todas);
            if (todas.size() > MAX_CANCIONES) {
                todas = new ArrayList<>(todas.subList(0, MAX_CANCIONES));
            }

            for (int i = 0; i < todas.size(); i++) {
                // Se renumera porque el orden cambio al barajar.
                String linea = (i + 1) + ". " + todas.get(i).replaceFirst("^\\d+\\.\\s*", "");
                songModel.addElement(linea);

                Matcher mId = PATRON_ID.matcher(linea);
                ids.add(mId.find() ? mId.group(1) : "");

                // La barra de progreso usa la duracion real del fragmento.
                duraciones.add((double) DURACION_CLIP_SEG);
            }

            detalle.append("\n\nCanciones encontradas: " + totalEncontradas
                    + "\nMostrando: " + songModel.size());
            estado.setText(songModel.size() + " de " + totalEncontradas + " canciones");

            if (!songModel.isEmpty()) {
                songList.setSelectedIndex(0);
                indiceActual = 0;
                actualizarInfoCancion();
                setControles(true);
            }
        });
    }

    public void mostrarError(String motivo) {
        SwingUtilities.invokeLater(() -> {
            estado.setText("Consulta sin resultado");
            detalle.append("\n\n" + motivo);
            JOptionPane.showMessageDialog(this, motivo, "Resultado", JOptionPane.WARNING_MESSAGE);
        });
    }

    // ---------------- Reproduccion ----------------

    private File archivoAudio(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String id6 = String.format("%06d", Long.parseLong(id));
        File directo = new File(RUTA_AUDIO + File.separator + id6.substring(0, 3), id6 + ".mp3");
        if (directo.exists()) {
            return directo;
        }
        // Carpeta anidada al descomprimir: fma_small/fma_small/000/...
        File anidado = new File(RUTA_AUDIO + File.separator + "fma_small" + File.separator
                + id6.substring(0, 3), id6 + ".mp3");
        return anidado.exists() ? anidado : directo;
    }

    private void reproducir(int indice, long desdeMs) {
        if (indice < 0 || indice >= ids.size()) {
            return;
        }

        detenerAudio();
        indiceActual = indice;
        songList.setSelectedIndex(indice);
        songList.ensureIndexIsVisible(indice);
        actualizarInfoCancion();

        File archivo = archivoAudio(ids.get(indice));
        if (archivo == null || !archivo.exists()) {
            reproduciendo = false;
            acumuladoMs = 0;
            btnPlay.setText("▶");
            estado.setText("No se encontró el audio");
            JOptionPane.showMessageDialog(this,
                    "No se encontró el archivo de audio:\n"
                            + (archivo == null ? "(ID vacío)" : archivo.getPath())
                            + "\n\nRevisa RUTA_AUDIO en Reproductor.java.",
                    "Audio", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            final int miSesion = ++sesion;
            final AdvancedPlayer p = new AdvancedPlayer(
                    new BufferedInputStream(new FileInputStream(archivo)));

            p.setPlayBackListener(new PlaybackListener() {
                @Override
                public void playbackFinished(PlaybackEvent evt) {
                    SwingUtilities.invokeLater(() -> {
                        // Si la sesion cambio, fue una pausa o un cambio de cancion.
                        if (miSesion == sesion) {
                            cancionTerminada();
                        }
                    });
                }
            });

            player = p;
            acumuladoMs = desdeMs;
            marcaMs = System.currentTimeMillis();
            reproduciendo = true;
            btnPlay.setText("⏸");
            estado.setText("Reproduciendo");

            // Un frame MP3 dura aprox. 26 ms (44.1 kHz).
            final int frameInicio = (int) (desdeMs / 26);

            Thread hilo = new Thread(() -> {
                try {
                    p.play(frameInicio, Integer.MAX_VALUE);
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        if (miSesion == sesion) {
                            reproduciendo = false;
                            btnPlay.setText("▶");
                            estado.setText("Error de audio: " + ex.getMessage());
                        }
                    });
                }
            }, "hilo-audio");
            hilo.setDaemon(true);
            hilo.start();

            timer.start();

        } catch (Exception ex) {
            reproduciendo = false;
            btnPlay.setText("▶");
            estado.setText("Error de audio: " + ex.getMessage());
        }
    }

    private void alternarPausa() {
        if (ids.isEmpty()) {
            return;
        }

        if (reproduciendo) {
            acumuladoMs = transcurridoMs();
            detenerAudio();
            reproduciendo = false;
            btnPlay.setText("▶");
            estado.setText("Pausado");
            actualizarProgreso();
        } else if (indiceActual >= 0 && acumuladoMs > 0) {
            reproducir(indiceActual, acumuladoMs);
        } else {
            int i = songList.getSelectedIndex();
            reproducir(i >= 0 ? i : 0, 0);
        }
    }

    private void anterior() {
        if (ids.isEmpty()) {
            return;
        }
        reproducir(Math.max(0, indiceActual - 1), 0);
    }

    private void siguiente() {
        if (ids.isEmpty()) {
            return;
        }
        if (indiceActual + 1 < ids.size()) {
            reproducir(indiceActual + 1, 0);
        }
    }

    private void cancionTerminada() {
        if (indiceActual + 1 < ids.size()) {
            reproducir(indiceActual + 1, 0);
        } else {
            detenerAudio();
            reproduciendo = false;
            acumuladoMs = 0;
            btnPlay.setText("▶");
            estado.setText("Playlist terminada");
            progressBar.setValue(0);
            currentTime.setText("0:00");
            timer.stop();
        }
    }

    private void detenerAudio() {
        sesion++;
        timer.stop();
        if (player != null) {
            // close() es seguro aunque la cancion ya haya terminado; stop() lanza
            // NullPointerException si el dispositivo de audio ya se cerro.
            try {
                player.close();
            } catch (Exception ex) {
                // ya estaba cerrado
            }
            player = null;
        }
    }

    private long transcurridoMs() {
        if (reproduciendo) {
            return acumuladoMs + (System.currentTimeMillis() - marcaMs);
        }
        return acumuladoMs;
    }

    private void actualizarProgreso() {
        if (indiceActual < 0 || indiceActual >= duraciones.size()) {
            return;
        }
        long durMs = (long) (duraciones.get(indiceActual) * 1000);
        long actual = transcurridoMs();
        if (durMs > 0 && actual > durMs) {
            actual = durMs;
        }
        currentTime.setText(formatear(actual));
        totalTime.setText(formatear(durMs));
        progressBar.setValue(durMs > 0 ? (int) (actual * 1000 / durMs) : 0);
    }

    private void actualizarInfoCancion() {
        if (indiceActual < 0 || indiceActual >= songModel.size()) {
            return;
        }
        String linea = songModel.get(indiceActual);
        int corte = linea.indexOf(" | ID:");
        String base = corte >= 0 ? linea.substring(0, corte) : linea;
        base = base.replaceFirst("^\\d+\\.\\s*", "");

        String titulo = base;
        String artista = "";
        int k = base.lastIndexOf(" - ");
        if (k >= 0) {
            titulo = base.substring(0, k);
            artista = base.substring(k + 3);
        }
        songName.setText(titulo);
        songName.setToolTipText(titulo);
        artistName.setText(artista);

        progressBar.setValue(0);
        currentTime.setText("0:00");
        totalTime.setText(formatear((long) (duraciones.get(indiceActual) * 1000)));
    }

    private String formatear(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    private void setControles(boolean activos) {
        btnPrev.setEnabled(activos);
        btnPlay.setEnabled(activos);
        btnNext.setEnabled(activos);
    }

    @Override
    public void dispose() {
        detenerAudio();
        super.dispose();
    }

    // ---------------- Utilidades de estilo ----------------

    public static JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Monospaced", Font.BOLD, 14));
        btn.setBackground(COLOR_BUTTON_BG);
        btn.setForeground(COLOR_TEXT_MAIN);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_ACCENT, 1, true),
                new EmptyBorder(5, 15, 5, 15)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Reproductor player = new Reproductor();
            player.setVisible(true);
        });
    }
}