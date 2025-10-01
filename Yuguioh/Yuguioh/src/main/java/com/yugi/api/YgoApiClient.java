// src/main/java/com/yugi/api/YgoApiClient.java
package com.yugi.api;

import com.yugi.model.Card;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class YgoApiClient {
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    // Endpoint que devuelve SOLO cartas Monster
    private static final String MONSTER_ENDPOINT =
            "https://db.ygoprodeck.com/api/v7/cardinfo.php?type=Normal%20Monster";

    private final Random rnd = new Random();

    /**
     * Obtiene una carta Monster aleatoria desde el pool de Monsters.
     * Retorna CompletableFuture<Card>
     */
    public CompletableFuture<Card> fetchRandomMonsterCardAsync() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(MONSTER_ENDPOINT))
                .GET()
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException("Error en la API: status " + resp.statusCode());
                    }

                    JSONObject json = new JSONObject(resp.body());
                    JSONArray data = json.getJSONArray("data");

                    // elegir una carta Monster al azar
                    JSONObject cardJson = data.getJSONObject(rnd.nextInt(data.length()));

                    String name = cardJson.optString("name", "Unknown");
                    int atk = cardJson.has("atk") && !cardJson.isNull("atk") ? cardJson.getInt("atk") : 0;
                    int def = cardJson.has("def") && !cardJson.isNull("def") ? cardJson.getInt("def") : 0;

                    String imageUrl = "";
                    JSONArray imgs = cardJson.optJSONArray("card_images");
                    if (imgs != null && imgs.length() > 0) {
                        imageUrl = imgs.getJSONObject(0).optString("image_url", "");
                    }

                    return new Card(name, atk, def, imageUrl);
                });
    }

    /**
     * Obtiene n cartas Monster en paralelo.
     */
    public CompletableFuture<List<Card>> fetchNMonsterCardsAsync(int n) {
        List<CompletableFuture<Card>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(fetchRandomMonsterCardAsync());
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<Card> list = new ArrayList<>();
                    for (CompletableFuture<Card> f : futures) {
                        list.add(f.join()); // seguro porque allOf completó
                    }
                    return list;
                });
    }
}
