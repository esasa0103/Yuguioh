// src/main/java/com/yugi/ui/MainWindow.java
package com.yugi.ui;

import com.yugi.api.YgoApiClient;
import com.yugi.game.BattleListener;
import com.yugi.game.Duel;
import com.yugi.model.Card;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainWindow extends JFrame implements BattleListener {
    private final YgoApiClient api = new YgoApiClient();

    private JButton btnStart;
    private JButton btnPlaySelected;
    private JLabel lblTurn;
    private JLabel lblScore;
    private JTextArea logArea;
    private JPanel playerPanel;
    private JPanel aiPanel;
    private Duel duel;

    private java.util.List<Card> playerCards;
    private java.util.List<Card> aiCards;

    // selection
    private int selectedIndex = -1;
    private JToggleButton[] cardButtons = new JToggleButton[3];

    public MainWindow() {
        setTitle("Yu-Gi-Oh! Duel Lite");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        btnStart = new JButton("Iniciar duelo (cargar cartas)");
        btnPlaySelected = new JButton("Elegir carta (Jugar selección)");
        lblTurn = new JLabel("Turno: -");
        lblScore = new JLabel("Score: 0 - 0");
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);

        playerPanel = new JPanel();
        playerPanel.setLayout(new GridLayout(1, 3, 10, 10));

        aiPanel = new JPanel();
        aiPanel.setLayout(new GridLayout(1, 3, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(btnStart);
        topPanel.add(btnPlaySelected);
        topPanel.add(lblTurn);
        topPanel.add(lblScore);

        JScrollPane logScroll = new JScrollPane(logArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel center = new JPanel(new BorderLayout());
        center.add(new JLabel("Cartas Jugador:"), BorderLayout.NORTH);
        center.add(playerPanel, BorderLayout.CENTER);

        JPanel aiContainer = new JPanel(new BorderLayout());
        aiContainer.add(new JLabel("Cartas IA (ocultas hasta jugar):"), BorderLayout.NORTH);
        aiContainer.add(aiPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, center, aiContainer);
        split.setResizeWeight(0.6);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);
        getContentPane().add(logScroll, BorderLayout.SOUTH);

        // acciones
        btnStart.addActionListener(e -> startDuel());
        btnPlaySelected.addActionListener(e -> onPlaySelected());

        // inicialmente deshabilitar play hasta cargar cartas
        btnPlaySelected.setEnabled(false);
    }

    private void startDuel() {
        btnStart.setEnabled(false);
        log("Cargando cartas para jugador e IA...");
        playerPanel.removeAll();
        aiPanel.removeAll();
        selectedIndex = -1;
        btnPlaySelected.setEnabled(false);
        lblTurn.setText("Turno: cargando...");

        CompletableFuture<List<Card>> pFuture = api.fetchNMonsterCardsAsync(3);
        CompletableFuture<List<Card>> aFuture = api.fetchNMonsterCardsAsync(3);

        pFuture.thenCombine(aFuture, (pList, aList) -> {
            this.playerCards = pList;
            this.aiCards = aList;
            return null;
        }).whenComplete((v, ex) -> {
            if (ex != null) {
                SwingUtilities.invokeLater(() -> {
                    log("Error al cargar cartas: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Error al cargar cartas: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    btnStart.setEnabled(true);
                    lblTurn.setText("Turno: -");
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    setupUIWithCards();
                });
            }
        });
    }

    private void setupUIWithCards() {
        playerPanel.removeAll();
        aiPanel.removeAll();
        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; i < 3; i++) {
            Card c = playerCards.get(i);
            JToggleButton tbtn = new JToggleButton("<html><center>" + c.getName() + "<br/>ATK:" + c.getAtk() + " DEF:" + c.getDef() + "</center></html>");
            final int idx = i;
            tbtn.addActionListener(ev -> {
                // seleccion
                selectedIndex = idx;
                for (int j = 0; j < 3; j++) if (cardButtons[j] != null && j != idx) cardButtons[j].setSelected(false);
            });
            cardButtons[i] = tbtn;
            bg.add(tbtn);
            JPanel cardPanel = new JPanel(new BorderLayout());
            JLabel imageLabel = new JLabel("Cargando imagen...", SwingConstants.CENTER);
            cardPanel.add(imageLabel, BorderLayout.CENTER);
            cardPanel.add(tbtn, BorderLayout.SOUTH);
            playerPanel.add(cardPanel);

            // cargar imagen async
            if (c.getImageUrl() != null && !c.getImageUrl().isEmpty()) {
                loadImageAsync(c.getImageUrl(), imageLabel);
            } else {
                imageLabel.setText("No hay imagen");
            }

            // para AI: colocar imagen de espalda
            JLabel aiBack = new JLabel("[ Carta oculta ]", SwingConstants.CENTER);
            aiPanel.add(aiBack);
        }

        // crear Duel
        duel = new Duel(playerCards, aiCards, this);
        lblScore.setText("Score: 0 - 0");
        lblTurn.setText("Turno inicial: " + (duel.isPlayerStarts() ? "Jugador" : "IA") );
        log("Cartas cargadas. Turno inicial: " + (duel.isPlayerStarts() ? "Jugador" : "IA"));
        btnPlaySelected.setEnabled(true);

        revalidate();
        repaint();
    }

    private void loadImageAsync(String url, JLabel targetLabel) {
        // Swing-friendly: cargar en otro hilo y actualizar en EDT
        new Thread(() -> {
            try {
                BufferedImage img = ImageIO.read(new URL(url));
                if (img != null) {
                    // escalar a tamaño razonable
                    Image scaled = img.getScaledInstance(200, 280, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(scaled);
                    SwingUtilities.invokeLater(() -> targetLabel.setIcon(icon));
                } else {
                    SwingUtilities.invokeLater(() -> targetLabel.setText("No se pudo cargar imagen"));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> targetLabel.setText("Error imagen"));
            }
        }).start();
    }

    private void onPlaySelected() {
        if (duel == null) {
            JOptionPane.showMessageDialog(this, "Aún no se inició el duelo.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (selectedIndex < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una carta antes de jugar.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // llamar lógica del duelo con la selección del jugador
        duel.playRoundWithPlayerSelection(selectedIndex);

        // deshabilitar botón si la carta ya fue usada
        if (playerCards.get(selectedIndex).isUsed()) {
            cardButtons[selectedIndex].setEnabled(false);
            cardButtons[selectedIndex].setSelected(false);
            selectedIndex = -1;
        }
        // actualizar UI de cartas IA: mostrar las cartas jugadas (si se usaron)
        updateAiPanel();
        lblScore.setText("Score: " + duel.getPlayerScore() + " - " + duel.getAiScore());
        if (duel.isFinished()) {
            btnPlaySelected.setEnabled(false);
            btnStart.setEnabled(true); // permitir reiniciar
        }
    }

    private void updateAiPanel() {
        aiPanel.removeAll();
        for (int i = 0; i < aiCards.size(); i++) {
            Card c = aiCards.get(i);
            if (c.isUsed()) {
                JLabel lbl = new JLabel("<html><center>" + c.getName() + "<br/>ATK:" + c.getAtk() + "<br/>DEF:" + c.getDef() + "</center></html>", SwingConstants.CENTER);
                // intentar cargar imagen
                if (c.getImageUrl() != null && !c.getImageUrl().isEmpty()) {
                    loadImageAsync(c.getImageUrl(), lbl);
                }
                aiPanel.add(lbl);
            } else {
                JLabel back = new JLabel("[ Carta oculta ]", SwingConstants.CENTER);
                aiPanel.add(back);
            }
        }
        aiPanel.revalidate();
        aiPanel.repaint();
    }

    private void log(String s) {
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /* === Implementación de BattleListener === */
    @Override
    public void onTurn(String playerCard, String aiCard, String winner) {
        SwingUtilities.invokeLater(() -> {
            log("Turno: Jugador jugó: " + playerCard + "  |  IA jugó: " + aiCard);
            if ("Draw".equalsIgnoreCase(winner)) log("Resultado: Empate (sin puntos).");
            else if ("Player".equalsIgnoreCase(winner)) log("Resultado: Gana Jugador.");
            else log("Resultado: Gana IA.");
        });
    }

    @Override
    public void onScoreChanged(int playerScore, int aiScore) {
        SwingUtilities.invokeLater(() -> {
            lblScore.setText("Score: " + playerScore + " - " + aiScore);
            log("Puntaje actualizado: Jugador " + playerScore + " - IA " + aiScore);
        });
    }

    @Override
    public void onDuelEnded(String winner) {
        SwingUtilities.invokeLater(() -> {
            String msg = winner.equalsIgnoreCase("Player") ? "¡Has ganado el duelo!" : "La IA ha ganado el duelo.";
            log("DUEL FINALIZADO: " + msg);
            JOptionPane.showMessageDialog(this, msg, "Duel Ended", JOptionPane.INFORMATION_MESSAGE);
            btnPlaySelected.setEnabled(false);
            btnStart.setEnabled(true);
            lblTurn.setText("Duel finalizado. Pulsa Iniciar para jugar otra vez.");
        });
    }

    @Override
    public void onError(String message) {
        SwingUtilities.invokeLater(() -> {
            log("ERROR: " + message);
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }
}
