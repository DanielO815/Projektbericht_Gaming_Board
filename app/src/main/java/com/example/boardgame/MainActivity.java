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

    // card view auslagern
    final ArrayList<String> listeDatenInDatenbank = new ArrayList<>();
    private LinearLayout putCardViewLayoutHereInside;

    // spiele vorschlagen und voten
    final ArrayList<String> listeSpielVerschlaege = new ArrayList<>();
    final HashMap<String, Integer> gameVotes = new HashMap<>();
    private RadioGroup radioGroupSpiele;
    private EditText editTextText;
    private String lastVotedGame = null;


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
/*
        // View ini
        initializeViews();

        //  Logik der Ratingbar
        setupGastgeberRating();
        setupEssenRating();
        setupAbendRating();
*/

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // initialisieren des Card View Elements
        putCardViewLayoutHereInside = findViewById(R.id.putCardViewLayoutHereInside);






        Button button = findViewById(R.id.button2);
        // TextView textView = findViewById(R.id.textViewWannNaechsterTermin);
        // TextView textViewAdress = findViewById(R.id.textViewAdresse);


        button.setOnClickListener(v -> {

            createCardForDate();
/*
            fetchSpiele("Spieleabend","Name","Sebastian","Datum").whenComplete((res,ex) ->
            {handler.post(() -> {
                if (ex != null) {
                    textView.setText("Fehler: " + ex.getMessage());
                }else{
                    textView.setText("Datum: " + res);
                }
            });
            });
            fetchSpiele("Spieleabend","Name","Sebastian","Ort").whenComplete((res,ex) ->
            {handler.post(() -> {
                if (ex != null) {
                    textViewAdress.setText("Fehler: " + ex.getMessage());
                }else{
                    textViewAdress.setText("Wo: Sebastian " + res);
                }
            });
            }); */
        });


        // Testing add some stuff to listeDatenInDatenbank
        listeDatenInDatenbank.add("TerminDatumEins");
        listeDatenInDatenbank.add("Zweites Termindatum");
        listeDatenInDatenbank.add("Drittes Termindatum ");
        listeDatenInDatenbank.add("Viertes Termindatum");



    }

    private void onGameVoted(String selectedGame) {

        // TODO--- DATENBANK (AKTUALISIEREN / UPDATE) ---

        if (lastVotedGame != null && lastVotedGame.equals(selectedGame)) {
            lastVotedGame = null;
            int currentVotes = gameVotes.get(selectedGame);
            if (currentVotes > 0) {
                gameVotes.put(selectedGame, currentVotes - 1);

                // TODO--- DB-AUFRUF: Melde die entfernte Stimme
            }
            updatePollUI();
            return;
        }

        if (lastVotedGame != null && gameVotes.containsKey(lastVotedGame)) {
            int oldVotes = gameVotes.get(lastVotedGame);
            if (oldVotes > 0) {
                gameVotes.put(lastVotedGame, oldVotes - 1);

                // TODO--- DB-AUFRUF: Melde die entfernte Stimme vom alten Spiel
            }
        }

        int currentVotes = gameVotes.get(selectedGame);
        gameVotes.put(selectedGame, currentVotes + 1);
        lastVotedGame = selectedGame; // Auswahl speichern

        // TODO--- DB-AUFRUF: Melde die hinzugefügte Stimme für das neue Spiel

        updatePollUI();
    }

    private void createCardForDate() {

        // putCardViewLayoutHereInside.removeView(putCardViewLayoutHereInside(R.layout.cardview_per_date_item));
        putCardViewLayoutHereInside.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);


        for (String termin : listeDatenInDatenbank) {

            View itemView = inflater.inflate(R.layout.cardview_per_date_item, putCardViewLayoutHereInside, false);

            TextView textViewDate = itemView.findViewById(R.id.textViewWannNaechsterTermin);

            textViewDate.setText(termin);



            editTextText = itemView.findViewById(R.id.editTextText);
            final Button buttonVorschlagEingeben = itemView.findViewById(R.id.buttonVorschlagEingeben);
            radioGroupSpiele = itemView.findViewById(R.id.radioGroupSpiele);

            // TODO--- DATENBANK (LESEN) und falls Einträge vorhanden, liste befüllen und aktuallisieren


            buttonVorschlagEingeben.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String eingabe = editTextText.getText().toString().trim();

                    if (!eingabe.isEmpty() && !listeSpielVerschlaege.contains(eingabe)) {

                        listeSpielVerschlaege.add(eingabe);
                        gameVotes.put(eingabe, 0);

                        // TODO--- DATENBANK (SCHREIBEN / INSERT) ---

                        updatePollUI();
                    }
                    editTextText.setText("");
                }
            });








            // add new created Card
            putCardViewLayoutHereInside.addView(itemView);


        }


    }


    private void updatePollUI() {
        radioGroupSpiele.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        int totalVotes = 0;
        for (int votes : gameVotes.values()) {
            totalVotes += votes;
        }

        for (String spiel : listeSpielVerschlaege) {

            View itemView = inflater.inflate(R.layout.poll_item, radioGroupSpiele, false);

            RadioButton radioButton = itemView.findViewById(R.id.poll_radio_button);
            ProgressBar progressBar = itemView.findViewById(R.id.poll_progress_bar);
            TextView voteCount = itemView.findViewById(R.id.poll_vote_count);

            int votes = gameVotes.get(spiel);
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
                onGameVoted(spiel);
            });

            radioGroupSpiele.addView(itemView);
        }



    }
    // in MainActivity.java (ExecutorService + HttpURLConnection)

    // über httpURLConnection
    private CompletableFuture<String> fetchSpiele(String tabelle,String filterCol,String filterVal,String selectCol) {

        CompletableFuture<String> future = new CompletableFuture<>();

        executor.execute(() -> {
            String baseUrl = null;
            try {
                baseUrl = "http://10.0.2.2:3000/getColumn?table=" + URLEncoder.encode(tabelle,"UTF-8")
                        + "&filterCol=" + URLEncoder.encode(filterCol,"UTF-8")
                        + "&filterVal=" + URLEncoder.encode(filterVal,"UTF-8")
                        + "&selectCol=" + URLEncoder.encode(selectCol,"UTF-8");

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
    } */
}