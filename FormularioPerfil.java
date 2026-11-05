package Interfaz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

public class FormularioPerfil extends JFrame {

    private static final String[] GENEROS = {
            "Experimental", "Electronic", "Rock", "Instrumental", "Pop",
            "Folk", "Hip-Hop", "International", "Cualquiera"
    };
    private static final String[] GENEROS_VAL = {
            "Experimental", "Electronic", "Rock", "Instrumental", "Pop",
            "Folk", "Hip-Hop", "International", "cualquiera"
    };

    private static final String[] IDIOMAS = {
            "Inglés (en)", "Español (es)", "Francés (fr)", "Portugués (pt)",
            "Alemán (de)", "Ruso (ru)", "Italiano (it)", "Turco (tr)", "Cualquiera"
    };
    private static final String[] IDIOMAS_VAL = {
            "en", "es", "fr", "pt", "de", "ru", "it", "tr", "cualquiera"
    };

    private static final String[] POPULARIDAD = {"Popular", "Underground", "Cualquiera"};
    private static final String[] POPULARIDAD_VAL = {"popular", "underground", "cualquiera"};

    private static final String[] DURACION = {"Cortas", "Promedio", "Largas", "Cualquiera"};
    private static final String[] DURACION_VAL = {"cortas", "promedio", "largas", "cualquiera"};

    private static final String[] EMOCION = {"Feliz", "Tranquilo", "Triste", "Intenso", "Cualquiera"};
    private static final String[] EMOCION_VAL = {"feliz", "tranquilo", "triste", "intenso", "cualquiera"};

    private final JComboBox<String> cbGenero = crearCombo(GENEROS);
    private final JComboBox<String> cbIdioma = crearCombo(IDIOMAS);
    private final JComboBox<String> cbPopularidad = crearCombo(POPULARIDAD);
    private final JComboBox<String> cbDuracion = crearCombo(DURACION);
    private final JComboBox<String> cbEmocion = crearCombo(EMOCION);

    public FormularioPerfil(Consumer<String> alAceptar) {

        setTitle("Perfil musical");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(460, 460);
        setLocationRelativeTo(null);
        setResizable(false);
        setAlwaysOnTop(true);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(Reproductor.COLOR_BG_BORDER);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titulo = new JLabel("  Perfil musical  🎀", JLabel.LEFT);
        titulo.setFont(new Font("Monospaced", Font.BOLD, 18));
        titulo.setForeground(Reproductor.COLOR_TEXT_MAIN);

        JPanel formPanel = new JPanel(new GridLayout(5, 1, 0, 8));
        formPanel.setBackground(Reproductor.COLOR_CONTAINER);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Reproductor.COLOR_BUTTON_BG, 2, true),
                new EmptyBorder(12, 15, 12, 15)
        ));

        formPanel.add(fila("1. ¿Qué género prefieres hoy?", cbGenero));
        formPanel.add(fila("2. ¿En qué idioma?", cbIdioma));
        formPanel.add(fila("3. ¿Qué popularidad buscas?", cbPopularidad));
        formPanel.add(fila("4. ¿Qué duración por canción?", cbDuracion));
        formPanel.add(fila("5. ¿Cuál es tu estado emocional?", cbEmocion));

        JButton btnCrear = Reproductor.createStyledButton("Crear playlist 🎵");
        btnCrear.addActionListener(e -> {
            String perfil = GENEROS_VAL[cbGenero.getSelectedIndex()] + ","
                    + IDIOMAS_VAL[cbIdioma.getSelectedIndex()] + ","
                    + POPULARIDAD_VAL[cbPopularidad.getSelectedIndex()] + ","
                    + DURACION_VAL[cbDuracion.getSelectedIndex()] + ","
                    + EMOCION_VAL[cbEmocion.getSelectedIndex()];
            alAceptar.accept(perfil);
            dispose();
        });

        JPanel botonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        botonPanel.setOpaque(false);
        botonPanel.add(btnCrear);

        mainPanel.add(titulo, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(botonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel fila(String pregunta, JComboBox<String> combo) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);

        JLabel l = new JLabel(pregunta);
        l.setFont(new Font("Monospaced", Font.BOLD, 12));
        l.setForeground(Reproductor.COLOR_TEXT_MAIN);

        p.add(l, BorderLayout.NORTH);
        p.add(combo, BorderLayout.CENTER);
        return p;
    }

    private JComboBox<String> crearCombo(String[] opciones) {
        JComboBox<String> cb = new JComboBox<>(opciones);
        cb.setFont(new Font("Monospaced", Font.PLAIN, 12));
        cb.setBackground(Reproductor.COLOR_CARD_BG);
        cb.setForeground(Reproductor.COLOR_TEXT_MAIN);
        cb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return cb;
    }
}