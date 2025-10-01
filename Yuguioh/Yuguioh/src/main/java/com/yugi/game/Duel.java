// src/main/java/com/yugi/game/Duel.java
package com.yugi.game;

import com.yugi.model.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Duel {
    private final List<Card> playerCards;
    private final List<Card> aiCards;
    private final BattleListener listener;
    private final Random rnd = new Random();

    private int playerScore = 0;
    private int aiScore = 0;
    private boolean playerStarts; // turno inicial aleatorio

    public Duel(List<Card> playerCards, List<Card> aiCards, BattleListener listener) {
        this.playerCards = new ArrayList<>(playerCards);
        this.aiCards = new ArrayList<>(aiCards);
        this.listener = listener;
        this.playerStarts = rnd.nextBoolean();
    }

    public boolean isPlayerStarts() { return playerStarts; }

    public int getPlayerScore() { return playerScore; }
    public int getAiScore() { return aiScore; }

    public boolean isFinished() {
        return playerScore >= 2 || aiScore >= 2;
    }

    /**
     * Ejecuta una "ronda" con la carta elegida por el jugador (index).
     * La lógica decide roles según turno: si playerStarts==true -> player es atacante,
     * sino -> AI es atacante. Se alterna playerStarts después de cada ronda.
     */
    public void playRoundWithPlayerSelection(int playerIndex) {
        if (isFinished()) {
            if (listener != null) listener.onError("El duelo ya finalizó.");
            return;
        }
        if (playerIndex < 0 || playerIndex >= playerCards.size()) {
            if (listener != null) listener.onError("Índice de carta inválido.");
            return;
        }
        Card playerCard = playerCards.get(playerIndex);
        if (playerCard == null || playerCard.isUsed()) {
            if (listener != null) listener.onError("Carta ya usada o inválida.");
            return;
        }

        // elegir carta IA al azar entre disponibles
        List<Integer> aiAvailable = new ArrayList<>();
        for (int i = 0; i < aiCards.size(); i++) {
            if (!aiCards.get(i).isUsed()) aiAvailable.add(i);
        }
        if (aiAvailable.isEmpty()) {
            if (listener != null) listener.onError("La IA no tiene cartas disponibles.");
            return;
        }
        int aiIndex = aiAvailable.get(rnd.nextInt(aiAvailable.size()));
        Card aiCard = aiCards.get(aiIndex);

        // marcar usadas
        playerCard.setUsed(true);
        aiCard.setUsed(true);

        // Determinar roles
        boolean playerIsAttacker = playerStarts;      // si playerStarts=true -> player ataca
        boolean aiIsAttacker = !playerStarts;        // simétrico
        // El "que está en ataque" del defensor puede ser aleatorio (se puede mejorar para pedir al usuario)
        boolean playerInAttackPosition = playerIsAttacker ? true : rnd.nextBoolean();
        boolean aiInAttackPosition = aiIsAttacker ? true : rnd.nextBoolean();

        // Resolver duelo según reglas:
        // - Ambos en ataque -> comparar ATK
        // - Uno en ataque y otro en defensa -> comparar ATK(atacante) vs DEF(defensor)
        String winner; // "Player", "AI", "Draw"
        if (playerInAttackPosition && aiInAttackPosition) {
            if (playerCard.getAtk() > aiCard.getAtk()) { playerScore++; winner = "Player"; }
            else if (playerCard.getAtk() < aiCard.getAtk()) { aiScore++; winner = "AI"; }
            else { winner = "Draw"; }
        } else if (playerInAttackPosition && !aiInAttackPosition) {
            if (playerCard.getAtk() > aiCard.getDef()) { playerScore++; winner = "Player"; }
            else if (playerCard.getAtk() < aiCard.getDef()) { aiScore++; winner = "AI"; }
            else { winner = "Draw"; }
        } else if (!playerInAttackPosition && aiInAttackPosition) {
            if (aiCard.getAtk() > playerCard.getDef()) { aiScore++; winner = "AI"; }
            else if (aiCard.getAtk() < playerCard.getDef()) { playerScore++; winner = "Player"; }
            else { winner = "Draw"; }
        } else {
            // ambos en defensa -> empate práctico
            winner = "Draw";
        }

        // Notificar
        if (listener != null) {
            String pDesc = playerCard.getName() + " (ATK:" + playerCard.getAtk() + " DEF:" + playerCard.getDef() + (playerInAttackPosition ? " - ATK" : " - DEF") + ")";
            String aDesc = aiCard.getName() + " (ATK:" + aiCard.getAtk() + " DEF:" + aiCard.getDef() + (aiInAttackPosition ? " - ATK" : " - DEF") + ")";
            listener.onTurn(pDesc, aDesc, winner);
            listener.onScoreChanged(playerScore, aiScore);
            if (isFinished()) {
                String finalWinner = playerScore > aiScore ? "Player" : "AI";
                listener.onDuelEnded(finalWinner);
            }
        }

        // alternar turno inicial para la siguiente ronda
        playerStarts = !playerStarts;
    }
}
