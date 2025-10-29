package com.example.boardgame;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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


    // bewertung gastgeberIn essen abend
    private RatingBar ratingBarGastgeberIn;
    private RatingBar ratingBarEssen;
    private RatingBar ratingBarAbend;

    private TextView textGastgeberInBewerten;
    private TextView textWieWarDasEssen;
    private TextView textWieWarDerAbend;


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
            // In einer echten App: Hier zuerst die terminIDs aus der DB laden!
            createCardForDate();
        });


        // DATENBANK-SIMULATION FÜR TESTS
        // TODO--- DATENBANK (LESEN) ---
        // Lade alle Termine aus der DB Tabelle Spielabend2, um Liste 'terminIDs' zu füllen.
        if (terminIDs.isEmpty()) {
            terminIDs.add("1"); // Entspricht Id=1 in Tabelle Spieleabend2
            terminIDs.add("2"); // Entspricht Id=2 in Tabelle Spieleabend2
            terminIDs.add("3"); // Entspricht Id=3 in Tabelle Spieleabend2
        }


        // Initialisiert die Datenstrukturen für jeden Termin-Key in der Liste 'terminIDs'
        // TODO--- Daten aus Tabell SpielVorschlaege laden
        //
        for (String terminID : terminIDs) {
            if (!listenSpielVorschlaege.containsKey(terminID)) {
                listenSpielVorschlaege.put(terminID, new ArrayList<>());
                mapGameVotes.put(terminID, new HashMap<>());
                mapLastVotedGame.put(terminID, null);
            }
        }
        // Testdaten für den ersten Termin (Id="1") hinzufügen
        // Sollte aus der DB Tabelle SpielVorschlaege geladen werden
        ArrayList<String> vorschlaege1 = listenSpielVorschlaege.get("1");
        if (vorschlaege1.isEmpty()) {
            vorschlaege1.add("Monopoly");
            mapGameVotes.get("1").put("Monopoly", 2); // Simuliert 2 Stimmen
            vorschlaege1.add("UNO");
            mapGameVotes.get("1").put("UNO", 0); // Simuliert keine Stimme
        }
        ArrayList<String> vorschlaege2 = listenSpielVorschlaege.get("2");
        if (vorschlaege2.isEmpty()) {
            vorschlaege2.add("Uno");
            mapGameVotes.get("2").put("Uno", 1); // Simuliert 1 Stimme
            vorschlaege2.add("Berttspiel 123");
            mapGameVotes.get("2").put("Berttspiel 123", 2); // Simuliert 2 Stimmen
        }
        ArrayList<String> vorschlaege3 = listenSpielVorschlaege.get("3");
        if (vorschlaege3.isEmpty()) {
            vorschlaege3.add("Uno");
            mapGameVotes.get("3").put("Uno", 0); // Simuliert keine Stimme
            vorschlaege3.add("Berttspiel 123");
            mapGameVotes.get("3").put("Berttspiel 123", 0); // Simuliert keine Stimmen
            vorschlaege3.add("Monopoly");
            mapGameVotes.get("3").put("Monopoly", 3); // Simuliert 3 Stimmen
            vorschlaege3.add("Brettspiel 42");
            mapGameVotes.get("3").put("Brettspiel 42", 0); // Simuliert keine Stimmen
        }
    }


    // Verarbeitet einen Klick auf ein Spielvorschlag (eine Abstimmung).
    private void onGameVoted(String terminID, String selectedGame, RadioGroup radioGroupSpiele) {

        // TODO--- DATENBANK (SCHREIBEN / UPDATE) ---
        // 'UPDATE SpielVorschlaege SET StimmenAnzahl = ... WHERE Spieleabend_Id = terminID AND SpielName = selectedGame'
        // + (Logik zum Zählen der Stimmen)

        HashMap<String, Integer> gameVotes = mapGameVotes.get(terminID);
        String lastVotedGame = mapLastVotedGame.get(terminID);

        if (lastVotedGame != null && lastVotedGame.equals(selectedGame)) {
            lastVotedGame = null;
            int currentVotes = gameVotes.getOrDefault(selectedGame, 0);
            if (currentVotes > 0) {
                gameVotes.put(selectedGame, currentVotes - 1);
                // TODO: DB-Update (Stimme entfernen)
            }

        } else {
            if (lastVotedGame != null && gameVotes.containsKey(lastVotedGame)) {
                int oldVotes = gameVotes.getOrDefault(lastVotedGame, 0);
                if (oldVotes > 0) {
                    gameVotes.put(lastVotedGame, oldVotes - 1);
                    // TODO: DB-Update (Alte Stimme entfernen)
                }
            }
            int currentVotes = gameVotes.getOrDefault(selectedGame, 0);
            gameVotes.put(selectedGame, currentVotes + 1);
            lastVotedGame = selectedGame;
            // TODO: DB-Update (Neue Stimme hinzufügen)
        }

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


            // KARTENDETAILS DYNAMISCH AUS DB LADEN
            // fetchSpiele-Methode, um die Kartendetails zu füllen
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

                            // Anfangswerte setzen aus der DB geladen
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









            // VORSCHLAGS-LOGIK
            // TODO--- DATENBANK (LESEN) ---
            // Lade alle Vorschläge aus 'SpielVorschlaege' WHERE Spieleabend_Id = terminID
            // und fülle 'listenSpielVorschlaege.get(terminID)' und 'mapGameVotes.get(terminID)'

            // Fülle die Umfrage-UI mit den (simulierten) Daten, die zu diesem 'terminID' gehören
            updatePollUI(localRadioGroupSpiele, terminID);

            buttonVorschlagEingeben.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String eingabe = localEditTextText.getText().toString().trim();

                    ArrayList<String> vorschlaege = listenSpielVorschlaege.get(terminID);
                    HashMap<String, Integer> votes = mapGameVotes.get(terminID);

                    if (!eingabe.isEmpty() && vorschlaege != null && !vorschlaege.contains(eingabe)) {

                        // TODO--- DATENBANK (SCHREIBEN / INSERT) ---
                        // 'INSERT INTO SpielVorschlaege (Spieleabend_Id, SpielName, StimmenAnzahl) VALUES (terminID, eingabe, 0)'

                        vorschlaege.add(eingabe);
                        if (votes != null) {
                            votes.put(eingabe, 0);
                        }

                        updatePollUI(localRadioGroupSpiele, terminID);
                    }
                    localEditTextText.setText("");
                }
            });

            putCardViewLayoutHereInside.addView(itemView);
        }
    }


     // Aktualisiert eine RadioGroup EINER CardView mit den Daten eines Termins
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
                baseUrl = "http://10.0.2.2:3000/getColumn?table=" + URLEncoder.encode(tabelle, "UTF-8")
                        + "&filterCol=" + URLEncoder.encode(filterCol, "UTF-8")
                        + "&filterVal=" + URLEncoder.encode(filterVal, "UTF-8")
                        + "&selectCol=" + URLEncoder.encode(selectCol, "UTF-8");

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

/*
        private void initializeViews() {
        // RatingBars
        ratingBarGastgeberIn = findViewById(R.id.ratingBarGastgeberIn);
        ratingBarEssen = findViewById(R.id.ratingBarEssen);
        ratingBarAbend = findViewById(R.id.ratingBar);

        // Zugehörige TextViews (zum Aktualisieren der Feedback-Texte)
        textGastgeberInBewerten = findViewById(R.id.textGastgeberInBewerten);
        textWieWarDasEssen = findViewById(R.id.textWieWarDasEssen);
        textWieWarDerAbend = findViewById(R.id.textWieWarDerAbend);
    }

    // Gastgeber Rating
    private void setupGastgeberRating() {
        ratingBarGastgeberIn.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
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

                    textGastgeberInBewerten.setText(feedback);
                    Toast.makeText(MainActivity.this, feedback, Toast.LENGTH_SHORT).show();
                    // TODO--- DATENBANK (SCHREIBEN / UPDATE) ---
                    // Speichere die Bewertung in Datenbank: saveRating("gastgeber", rating);
                }
            }
        });
    }

   //Essen Rating!
    private void setupEssenRating() {
        ratingBarEssen.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
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

                    textWieWarDasEssen.setText(feedback);
                    Toast.makeText(MainActivity.this, feedback, Toast.LENGTH_SHORT).show();
                    // Speichere die Bewertung z.B. in einer Datenbank: saveRating("essen", rating);
                }
            }
        });
    }

    //Abend Rating
    private void setupAbendRating() {
        ratingBarAbend.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
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

                    textWieWarDerAbend.setText(feedback);
                    Toast.makeText(MainActivity.this, feedback, Toast.LENGTH_SHORT).show();
                    // Speichere die Bewertung in einer Datenbank: saveRating("abend", rating);
                }
            }
        });
    }
*/
}