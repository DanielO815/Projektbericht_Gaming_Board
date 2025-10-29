package com.example.boardgame;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ListView listView;
    private static final String TAG = "MainActivity";               // für Log-Ausgaben
    private final List<Spieleaband2Item> cachedItems = new ArrayList<>(); // gespeicherte Liste

    private ArrayAdapter<String> arrayAdapter;
    private final List<String> items = new ArrayList<>();
    final ArrayList<String> listeSpielVerschlaege = new ArrayList<>();
    final HashMap<String, Integer> gameVotes = new HashMap<>();

    private RadioGroup radioGroupSpiele;
    private EditText editTextText;

    private String lastVotedGame = null;


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

        // View ini
        /*initializeViews();

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

        editTextText = findViewById(R.id.editTextText);
        final Button buttonVorschlagEingeben = findViewById(R.id.buttonVorschlagEingeben);
        radioGroupSpiele = findViewById(R.id.radioGroupSpiele);

        // TODO--- DATENBANK (LESEN) und falls Einträge vorhanden, liste befüllen und aktuallisieren

        /*
        buttonVorschlagEingeben.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String eingabe = editTextText.getText().toString().trim();

                if (!eingabe.isEmpty() && !listeSpielVerschlaege.contains(eingabe)) {

                    listeSpielVerschlaege.add(eingabe);
                    gameVotes.put(eingabe, 0);

                    // TODO--- DATENBANK (SCHREIBEN / INSERT) ---

                    //updatePollUI();
                }
                editTextText.setText("");
            }
        });
        */



        Button button = findViewById(R.id.button2);
        TextView textView = findViewById(R.id.textViewWannNaechsterTermin);
        TextView textViewAdress = findViewById(R.id.textViewAdresse);


        button.setOnClickListener(v -> {
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
            });
            //----------Insert--------------
            JSONObject valsInsert = new JSONObject();
            try {
                valsInsert.put("Name", "Max");
                valsInsert.put("Ort", "Mannheimerstr 14");
                valsInsert.put("Datum", "2023-08-15");
                valsInsert.put("Spiele", "COD2");
                valsInsert.put("Essen", "Hamburger");
            } catch (JSONException ignored) {}

            insertRow("Spieleabend", valsInsert).whenComplete((res, ex) -> {

            });

            //----------Update--------------
            JSONObject valsUpdate = new JSONObject();
            try {
                valsUpdate.put("Ort", "xxx");
            } catch (JSONException ignored) {}

            updateRow("Spieleabend", valsUpdate, "Name", "Sebastian").whenComplete((res, ex) -> {

            });



           /* getCompleteTableAsync("Spieleabend2")
                    .thenAccept(list -> {
                        // läuft im executor-Thread -> UI-Updates auf Main-Thread
                        runOnUiThread(() -> {
                            // z.B. in Feld speichern oder UI aktualisieren
                            cachedItems.clear();
                            cachedItems.addAll(list);
                            arrayAdapter.notifyDataSetChanged();
                            // oder Konsolenausgabe
                            for (Spieleaband2Item it : list) {
                                Log.d(TAG, it.getName() + " | " + it.getOrt() + " | " + it.getDatum());
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        runOnUiThread(() -> Toast.makeText(this, "Fehler: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                        return null;
                    });
            */
        });
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
                if (whereValue instanceof Number) where.put("value", ((Number) whereValue).longValue());
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




    // in MainActivity.java (ExecutorService + HttpURLConnection)
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

    public class Spieleaband2Item {
        private int id;
        private String name;
        private String ort;
        private String datum;
        public int getId() {return id;}
        public void setId(int id){this.id = id;}
        public String getName() {return name;}
        public void setName(String name){this.name = name;}
        public String getOrt() {return ort;}
        public void setOrt(String ort){this.ort = ort;}
        public String getDatum(){return datum;}
        public void setDatum(String datum){this.datum = datum;} }

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
    }
}
*/