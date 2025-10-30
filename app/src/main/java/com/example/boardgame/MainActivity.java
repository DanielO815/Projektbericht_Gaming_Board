package com.example.boardgame;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout putCardViewLayoutHereInside;

    // DB-IDs als Key
    // Liste aller Termin-IDs
    final ArrayList<String> terminIDs = new ArrayList<>();

    // Map, die jede Termin-ID (als String) auf ihre VORSCHLAGSLISTE abbildet
    final HashMap<String, ArrayList<String>> listenSpielVorschlaege = new HashMap<>();

    // Map, die jede Termin-ID auf ihre VOTING-MAP abbildet
    final HashMap<String, HashMap<String, Integer>> mapGameVotes = new HashMap<>();

    // Map, die jede Termin-ID auf ihre ZULETZT GEWÄHLTE STIMME abbildet
    final HashMap<String, String> mapLastVotedGame = new HashMap<>();


    // Map, die Termin-ID -> (SpielName -> Vorschlag-ID) abbildet
    final HashMap<String, HashMap<String, Integer>> mapSpielNameToVorschlagId = new HashMap<>();


    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        putCardViewLayoutHereInside = findViewById(R.id.putCardViewLayoutHereInside);

        Button button = findViewById(R.id.button2);
        button.setOnClickListener(v -> {
            // Lade die Karten basierend auf den Daten
            Toast.makeText(MainActivity.this, "Lade Termine...", Toast.LENGTH_SHORT).show();


            // UI leeren
            terminIDs.clear();
            listenSpielVorschlaege.clear();
            mapGameVotes.clear();
            mapLastVotedGame.clear();
            putCardViewLayoutHereInside.removeAllViews();

            // terminIDs aus der DB laden
            getCompleteTableAsync("Spieleabend2").whenComplete((list, ex) -> {
                // Zurück auf dem UI-Thread
                handler.post(() -> {
                    if (ex != null) {
                        Log.e("LoadTermine", "Fehler beim Laden der Termine", ex);
                        Toast.makeText(MainActivity.this, "Fehler: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (list != null && !list.isEmpty()) {
                        // Datenstrukturen füllen
                        for (Spieleaband2Item item : list) {
                            String terminID = String.valueOf(item.getId());
                            terminIDs.add(terminID);

                            // Maps für die neuen IDs initialisieren
                            if (!listenSpielVorschlaege.containsKey(terminID)) {
                                listenSpielVorschlaege.put(terminID, new ArrayList<>());
                                mapGameVotes.put(terminID, new HashMap<>());
                                mapLastVotedGame.put(terminID, null);
                                mapSpielNameToVorschlagId.put(terminID, new HashMap<>());
                            }
                        }

                        // Karten erstellen
                        createCardForDate();
                        Toast.makeText(MainActivity.this, "Termine geladen!", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(MainActivity.this, "Keine Termine gefunden.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }


    // Methode, um die Stimmenanzahl in der DB zu aktualisieren
    private void updateDatabaseVote(String terminID, String spielName, int newVoteCount) {
        // ID für diesen Spielvorschlag finden
        HashMap<String, Integer> nameToIdMap = mapSpielNameToVorschlagId.get(terminID);
        if (nameToIdMap == null) {
            Log.e("UpdateVote", "Keine nameToIdMap für terminID " + terminID + " gefunden.");
            return;
        }

        Integer vorschlagId = nameToIdMap.get(spielName);
        if (vorschlagId == null) {
            Log.e("UpdateVote", "Keine Vorschlag-ID für Spiel '" + spielName + "' gefunden.");
            return;
        }

        // Erstelle JSON für updateRow-Methode
        JSONObject valuesToUpdate = new JSONObject();
        try {
            valuesToUpdate.put("StimmenAnzahl", newVoteCount);
        } catch (JSONException e) {
            Log.e("UpdateVote", "JSON-Fehler beim Erstellen von 'values'", e);
            return;
        }

        // UPDATE SpielVorschlaege SET StimmenAnzahl = newVoteCount WHERE Id = vorschlagId
        updateRow("SpielVorschlaege", valuesToUpdate, "Id", vorschlagId).whenComplete((response, ex) -> {
            if (ex != null) {
                Log.e("UpdateVote", "Fehler beim DB-Update für " + spielName, ex);
            } else {
                Log.i("UpdateVote", "DB-Update erfolgreich für " + spielName + ": " + response);
            }
        });
    }


    // Verarbeitet einen Klick auf ein Spielvorschlag (eine Abstimmung).
    private void onGameVoted(String terminID, String selectedGame, RadioGroup radioGroupSpiele) {

        // Lokale Daten holen
        HashMap<String, Integer> gameVotes = mapGameVotes.get(terminID);
        String lastVotedGame = mapLastVotedGame.get(terminID);

        if (lastVotedGame != null && lastVotedGame.equals(selectedGame)) {
            // Wenn User auf das bereits gewählte Spiel klickt -> Stimme entfernen
            lastVotedGame = null;
            int currentVotes = gameVotes.getOrDefault(selectedGame, 0);

            if (currentVotes > 0) {
                int newVotes = currentVotes - 1;
                gameVotes.put(selectedGame, newVotes);
                updateDatabaseVote(terminID, selectedGame, newVotes);
            }

        } else {
            // Wenn User auf ein anderes Spiel klickt -> Stimme verschieben bzw nur neue Stimme hinzufügen

            // Alte Stimme entfernen
            if (lastVotedGame != null && gameVotes.containsKey(lastVotedGame)) {
                int oldVotes = gameVotes.getOrDefault(lastVotedGame, 0);
                if (oldVotes > 0) {
                    int newOldVotes = oldVotes - 1;
                    gameVotes.put(lastVotedGame, newOldVotes);
                    updateDatabaseVote(terminID, lastVotedGame, newOldVotes);
                }
            }

            // Neue Stimme hinzufügen
            int currentVotes = gameVotes.getOrDefault(selectedGame, 0);
            int newVotes = currentVotes + 1;
            gameVotes.put(selectedGame, newVotes);
            lastVotedGame = selectedGame;
            updateDatabaseVote(terminID, selectedGame, newVotes);
        }


        // Lokal speichern und UI aktualisieren
        mapLastVotedGame.put(terminID, lastVotedGame);
        updatePollUI(radioGroupSpiele, terminID);
    }

    // Erstellt Kartenansichten basierend auf den 'terminIDs'
    private void createCardForDate() {

        putCardViewLayoutHereInside.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        // Iteriere über die terminIDs Liste
        for (final String terminID : terminIDs) {

            View itemView = inflater.inflate(R.layout.cardview_per_date_item, putCardViewLayoutHereInside, false);

            // Elemente der Karte finden
            final TextView textViewDate = itemView.findViewById(R.id.textViewWannNaechsterTermin);
            final TextView textViewAddress = itemView.findViewById(R.id.textViewAdresse);
            final EditText localEditTextText = itemView.findViewById(R.id.editTextText);
            final Button buttonVorschlagEingeben = itemView.findViewById(R.id.buttonVorschlagEingeben);
            final RadioGroup localRadioGroupSpiele = itemView.findViewById(R.id.radioGroupSpiele);


            // CardView Inhalte aus der DB laden

            // Datum laden
            fetchSpiele("Spieleabend2", "Id", terminID, "Datum").whenComplete((datum, ex) -> {
                handler.post(() -> {
                    if (ex != null) textViewDate.setText("Fehler beim Laden des Datums");
                    else textViewDate.setText("Datum: " + datum);
                });
            });
            // Ort und Name laden
            fetchSpiele("Spieleabend2", "Id", terminID, "Name").whenComplete((name, exName) -> {
                fetchSpiele("Spieleabend2", "Id", terminID, "Ort").whenComplete((ort, exOrt) -> {
                    handler.post(() -> {
                        if (exName != null || exOrt != null) {
                            textViewAddress.setText("Fehler beim Laden der Adresse");
                        } else {
                            textViewAddress.setText("Wo: " + name + ", " + ort);
                        }
                    });
                });
            });


            // RatingBars
            final RatingBar cardRatingBarGastgeberIn = itemView.findViewById(R.id.ratingBarGastgeberIn);
            final RatingBar cardRatingBarEssen = itemView.findViewById(R.id.ratingBarEssen);
            final RatingBar cardRatingBarAbend = itemView.findViewById(R.id.ratingBar);

            // Zugehörige TextViews (zum Aktualisieren der Feedback-Texte)
            final TextView cardTextGastgeberInBewerten = itemView.findViewById(R.id.textGastgeberInBewerten);
            final TextView cardTextWieWarDasEssen = itemView.findViewById(R.id.textWieWarDasEssen);
            final TextView cardTextWieWarDerAbend = itemView.findViewById(R.id.textWieWarDerAbend);


            // Lade existierenden Ratings aus der DB
            fetchSpiele("Spieleabend2", "Id", terminID, "EssenSterne").whenComplete((essenSterneStr, exEssen) -> {
                fetchSpiele("Spieleabend2", "Id", terminID, "GastgeberSterne").whenComplete((gastgeberSterneStr, exGastgeber) -> {
                    fetchSpiele("Spieleabend2", "Id", terminID, "AbendSterne").whenComplete((abendSterneStr, exAbend) -> {

                        handler.post(() -> {

                            // Anfangswerte der Ratings setzen -> aus der DB geladen
                            try {
                                if (exEssen == null && essenSterneStr != null && !essenSterneStr.isEmpty() && !essenSterneStr.equals("Keine Einträge")) {
                                    cardRatingBarEssen.setRating(Float.parseFloat(essenSterneStr));
                                } else {
                                    cardRatingBarEssen.setRating(0);
                                }

                                if (exGastgeber == null && gastgeberSterneStr != null && !gastgeberSterneStr.isEmpty() && !gastgeberSterneStr.equals("Keine Einträge")) {
                                    cardRatingBarGastgeberIn.setRating(Float.parseFloat(gastgeberSterneStr));
                                } else {
                                    cardRatingBarGastgeberIn.setRating(0);
                                }

                                if (exAbend == null && abendSterneStr != null && !abendSterneStr.isEmpty() && !abendSterneStr.equals("Keine Einträge")) {
                                    cardRatingBarAbend.setRating(Float.parseFloat(abendSterneStr));
                                } else {
                                    cardRatingBarAbend.setRating(0);
                                }

                            } catch (NumberFormatException e) {
                                Log.e("RatingLoad", "Fehler beim Parsen der Sterne-Strings für TerminID: " + terminID, e);
                                cardRatingBarEssen.setRating(0);
                                cardRatingBarGastgeberIn.setRating(0);
                                cardRatingBarAbend.setRating(0);
                            }


                            // Listener hinzufügen
                            // Essen Rating
                            cardRatingBarEssen.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                                @Override
                                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                                    if (fromUser) {
                                        String feedback;
                                        if (rating == 5.0f) {
                                            feedback = "Kulinarisch 1A! 🍽️";
                                        } else if (rating >= 4.0f) {
                                            feedback = "Sehr lecker, gerne wieder!";
                                        } else if (rating >= 2.5f) {
                                            feedback = "War okay.";
                                        } else {
                                            feedback = "Das nächste mal gerne wo anders bestellen!";
                                        }

                                        cardTextWieWarDasEssen.setText(feedback);
                                        Toast.makeText(MainActivity.this, feedback, Toast.LENGTH_SHORT).show();

                                        // TODO--- DATENBANK (SCHREIBEN / UPDATE) ---
                                        // Speichere die Bewertung für diesen 'terminID'
                                        // z.B. updateRatingInDB(terminID, "EssenSterne", rating);
                                    }
                                }
                            });

                            // Gastgeber Rating
                            cardRatingBarGastgeberIn.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                                @Override
                                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                                    if (fromUser) {
                                        String feedback;
                                        if (rating == 5.0f) {
                                            feedback = "Absolut perfekte Gastgeber:in! ✨";
                                        } else if (rating >= 4.0f) {
                                            feedback = "Tolle Gastgeber:in!";
                                        } else if (rating >= 3.0f) {
                                            feedback = "Gute Erfahrung. Danke!";
                                        } else {
                                            feedback = "Da ist noch Luft nach oben.";
                                        }

                                        cardTextGastgeberInBewerten.setText(feedback);
                                        Toast.makeText(MainActivity.this, feedback, Toast.LENGTH_SHORT).show();

                                        // TODO--- DATENBANK (SCHREIBEN / UPDATE) ---
                                        // z.B. updateRatingInDB(terminID, "GastgeberSterne", rating);
                                    }
                                }
                            });

                            // Abend Rating
                            cardRatingBarAbend.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                                @Override
                                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                                    if (fromUser) {
                                        String feedback;
                                        if (rating == 5.0f) {
                                            feedback = "Ein perfekter Spieleabend! 🎲";
                                        } else if (rating >= 4.0f) {
                                            feedback = "Ein sehr guter Spieleabend!";
                                        } else if (rating >= 3.0f) {
                                            feedback = "Guter Abend.";
                                        } else {
                                            feedback = "Da ist noch Potenzial.";
                                        }

                                        cardTextWieWarDerAbend.setText(feedback);
                                        Toast.makeText(MainActivity.this, feedback, Toast.LENGTH_SHORT).show();

                                        // TODO--- DATENBANK (SCHREIBEN / UPDATE) ---
                                        // z.B. updateRatingInDB(terminID, "AbendSterne", rating);
                                    }
                                }
                            });

                        });
                    });
                });
            });


            // Starte den Ladevorgang für die Spielvorschläge
            fetchSpielVorschlaegeAsync("SpielVorschlaege", "Spieleabend_Id", terminID).whenComplete((vorschlaegeList, ex) -> {
                handler.post(() -> {
                    if (ex != null) {
                        Log.e("LoadVorschlaege", "Fehler beim Laden der Vorschläge für " + terminID, ex);
                        return;
                    }

                    // Lade die Listen für einen Termin
                    ArrayList<String> vorschlaege = listenSpielVorschlaege.get(terminID);
                    HashMap<String, Integer> votes = mapGameVotes.get(terminID);
                    HashMap<String, Integer> nameToIdMap = mapSpielNameToVorschlagId.get(terminID);

                    // Sicherstellen, dass die Listen leer sind, um sie dann befüllen zu können
                    if (vorschlaege != null) vorschlaege.clear();
                    if (votes != null) votes.clear();
                    if (nameToIdMap != null) nameToIdMap.clear();

                    if (vorschlaegeList != null && !vorschlaegeList.isEmpty() && vorschlaege != null && votes != null && nameToIdMap != null) {
                        for (SpielVorschlagItem item : vorschlaegeList) {
                            vorschlaege.add(item.getSpielName());
                            votes.put(item.getSpielName(), item.getStimmenAnzahl());
                            nameToIdMap.put(item.getSpielName(), item.getId());
                        }
                    }

                    // Umfrage-UI füllen mit den DB Daten
                    updatePollUI(localRadioGroupSpiele, terminID);
                });
            });


            buttonVorschlagEingeben.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String eingabe = localEditTextText.getText().toString().trim();

                    ArrayList<String> vorschlaege = listenSpielVorschlaege.get(terminID);
                    HashMap<String, Integer> votes = mapGameVotes.get(terminID);

                    if (!eingabe.isEmpty() && vorschlaege != null && !vorschlaege.contains(eingabe)) {

                        JSONObject newVorschlagValues = new JSONObject();

                        try {
                            newVorschlagValues.put("Spieleabend_Id", Integer.parseInt(terminID));
                            newVorschlagValues.put("SpielName", eingabe);
                            newVorschlagValues.put("StimmenAnzahl", 0);
                        } catch (Exception e) {
                            Log.e("JSON", "Fehler beim Erstellen des Vorschlag-JSON", e);
                            Toast.makeText(MainActivity.this, "JSON-Fehler", Toast.LENGTH_SHORT).show();
                            return; // Abbruch
                        }

                        // insertRow
                        insertRow("SpielVorschlaege", newVorschlagValues).whenComplete((response, exInsert) -> {
                            handler.post(() -> {
                                if (exInsert != null) {
                                    Toast.makeText(MainActivity.this, "Fehler beim Speichern", Toast.LENGTH_SHORT).show();
                                    Log.e("InsertRow", "Fehler", exInsert);
                                    return;
                                }

                                Log.i("InsertRow", "Antwort: " + response);

                                vorschlaege.add(eingabe);
                                if (votes != null) {
                                    votes.put(eingabe, 0);
                                }

                                // UI aktualisieren und Textfeld leeren
                                updatePollUI(localRadioGroupSpiele, terminID);
                                localEditTextText.setText("");
                            });
                        });
                    } else if (eingabe.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Bitte einen Namen eingeben", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Vorschlag existiert bereits", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            putCardViewLayoutHereInside.addView(itemView);
        }
    }


    // Aktualisiert eine RadioGroup einer CardView mit den Daten des jeweiligen Termins
    private void updatePollUI(final RadioGroup radioGroupSpiele, final String terminID) {
        radioGroupSpiele.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        ArrayList<String> listeSpielVerschlaege = listenSpielVorschlaege.get(terminID);
        HashMap<String, Integer> gameVotes = mapGameVotes.get(terminID);
        String lastVotedGame = mapLastVotedGame.get(terminID);

        if (listeSpielVerschlaege == null || gameVotes == null) {
            Log.e("UpdatePollUI", "Keine Daten für Termin-Key gefunden: " + terminID);
            return;
        }

        int totalVotes = 0;
        for (int votes : gameVotes.values()) {
            totalVotes += votes;
        }

        for (final String spiel : listeSpielVerschlaege) {

            View itemView = inflater.inflate(R.layout.poll_item, radioGroupSpiele, false);

            RadioButton radioButton = itemView.findViewById(R.id.poll_radio_button);
            ProgressBar progressBar = itemView.findViewById(R.id.poll_progress_bar);
            TextView voteCount = itemView.findViewById(R.id.poll_vote_count);

            int votes = gameVotes.getOrDefault(spiel, 0);
            radioButton.setText(spiel);
            voteCount.setText(String.valueOf(votes));

            if (totalVotes > 0) {
                int progress = (int) (((double) votes / totalVotes) * 100);
                progressBar.setProgress(progress);
            } else {
                progressBar.setProgress(0);
            }

            radioButton.setChecked(spiel.equals(lastVotedGame));

            radioButton.setOnClickListener(v -> {
                onGameVoted(terminID, spiel, radioGroupSpiele);
            });

            radioGroupSpiele.addView(itemView);
        }
    }

    // in MainActivity.java (ExecutorService + HttpURLConnection)
    // über httpURLConnection
    private CompletableFuture<String> fetchSpiele(String tabelle, String filterCol, String filterVal, String selectCol) {

        CompletableFuture<String> future = new CompletableFuture<>();

        executor.execute(() -> {
            String baseUrl = null;
            try {
                baseUrl = "http://10.0.2.2:3000/getColumn?table=" + URLEncoder.encode(tabelle, "UTF-8") + "&filterCol=" + URLEncoder.encode(filterCol, "UTF-8") + "&filterVal=" + URLEncoder.encode(filterVal, "UTF-8") + "&selectCol=" + URLEncoder.encode(selectCol, "UTF-8");

            } catch (UnsupportedEncodingException e) {
                future.completeExceptionally(e);
                return;
            }
            HttpURLConnection conn = null;
            String result = null;
            try {
                URL url = new URL(baseUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                result = sb.toString();
            } catch (Exception e) {
                result = "{\"error\":\"" + e.getMessage() + "\"}";
            } finally {
                if (conn != null) conn.disconnect();
            }
            try {
                JSONArray arr = new JSONArray(result);
                if (arr.length() > 0) {
                    String spiele = arr.getJSONObject(0).optString(selectCol, "");
                    if (spiele.isEmpty()) future.complete("Keine Einträge");
                    else future.complete(spiele);
                } else {
                    future.complete("Keine Einträge");
                }
            } catch (JSONException e) {
                try {
                    JSONObject err = new JSONObject(result);
                    String msg = "Serverfehler: " + err.optString("error", e.getMessage());
                    future.complete(msg);
                } catch (JSONException ex) {
                    future.complete("Parsefehler");
                }
            }
        });
        return future;
    }

    private CompletableFuture<List<Spieleaband2Item>> getCompleteTableAsync(String table) {
        CompletableFuture<List<Spieleaband2Item>> future = new CompletableFuture<>();
        executor.execute(() -> {
            List<Spieleaband2Item> list = new ArrayList<>();
            String baseUrl;
            try {
                baseUrl = "http://10.0.2.2:3000/completeTable?table=" + URLEncoder.encode(table, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                future.completeExceptionally(e);
                return;
            }

            HttpURLConnection conn = null;
            String responseString;
            try {
                URL url = new URL(baseUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                responseString = sb.toString();

                JSONArray arr = new JSONArray(responseString);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Spieleaband2Item item = new Spieleaband2Item();
                    item.setId(o.optInt("Id", 0));
                    item.setName(o.optString("Name", ""));
                    item.setOrt(o.optString("Ort", ""));
                    item.setDatum(o.optString("Datum", ""));

                    item.setEssenSterne((float) o.optDouble("EssenSterne", 0.0));
                    item.setGastgeberSterne((float) o.optDouble("GastgeberSterne", 0.0));
                    item.setAbendSterne((float) o.optDouble("AbendSterne", 0.0));

                    list.add(item);
                }
                future.complete(list);
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
        return future;
    }


    private CompletableFuture<List<SpielVorschlagItem>> fetchSpielVorschlaegeAsync(String table, String filterCol, String filterVal) {
        CompletableFuture<List<SpielVorschlagItem>> future = new CompletableFuture<>();
        executor.execute(() -> {
            List<SpielVorschlagItem> list = new ArrayList<>();
            String baseUrl;
            try {
                baseUrl = "http://10.0.2.2:3000/getFilteredRows?table=" + URLEncoder.encode(table, "UTF-8") + "&filterCol=" + URLEncoder.encode(filterCol, "UTF-8") + "&filterVal=" + URLEncoder.encode(filterVal, "UTF-8");

            } catch (UnsupportedEncodingException e) {
                future.completeExceptionally(e);
                return;
            }

            HttpURLConnection conn = null;
            String responseString;
            try {
                URL url = new URL(baseUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                responseString = sb.toString();

                JSONArray arr = new JSONArray(responseString);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    SpielVorschlagItem item = new SpielVorschlagItem();

                    item.setId(o.optInt("Id"));
                    item.setSpieleabend_Id(o.optInt("Spieleabend_Id"));
                    item.setSpielName(o.optString("SpielName", ""));
                    item.setStimmenAnzahl(o.optInt("StimmenAnzahl", 0));
                    list.add(item);
                }
                future.complete(list);
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
        return future;
    }


    private CompletableFuture<String> insertRow(String tabelle, JSONObject values) {
        CompletableFuture<String> future = new CompletableFuture<>();

        executor.execute(() -> {
            String baseUrl = "http://10.0.2.2:3000/insert";
            HttpURLConnection conn = null;

            JSONObject body = new JSONObject();
            try {
                body.put("table", tabelle);
                body.put("values", values);
                URL url = new URL(baseUrl);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);


                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                String result = sb.toString();

                try {
                    JSONObject resp = new JSONObject(result);
                    if (resp.has("insertedId")) {
                        future.complete("InsertedId: " + resp.optString("insertedId", "null"));
                    } else if (resp.has("error")) {
                        future.complete("Serverfehler: " + resp.optString("error"));
                    } else {
                        future.complete(result);
                    }
                } catch (JSONException e) {
                    future.complete("Parsefehler");
                }

            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
        return future;
    }

    private CompletableFuture<String> updateRow(String tabelle, JSONObject values, String whereKey, Object whereValue) {
        CompletableFuture<String> future = new CompletableFuture<>();

        executor.execute(() -> {
            String baseUrl = "http://10.0.2.2:3000/update";
            HttpURLConnection conn = null;
            try {
                JSONObject where = new JSONObject();
                where.put("key", whereKey);
                // whereValue kann Number, Boolean oder String sein
                if (whereValue instanceof Number)
                    where.put("value", ((Number) whereValue).longValue());
                else if (whereValue instanceof Boolean) where.put("value", (Boolean) whereValue);
                else where.put("value", String.valueOf(whereValue));

                JSONObject body = new JSONObject();
                body.put("table", tabelle);
                body.put("values", values);
                body.put("where", where);

                URL url = new URL(baseUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                String result = sb.toString();

                try {
                    JSONObject resp = new JSONObject(result);
                    if (resp.has("affected")) {
                        future.complete("Affected rows: " + resp.optInt("affected", 0));
                    } else if (resp.has("error")) {
                        future.complete("Serverfehler: " + resp.optString("error"));
                    } else {
                        future.complete(result);
                    }
                } catch (JSONException e) {
                    future.complete("Parsefehler");
                }

            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });

        return future;
    }
}