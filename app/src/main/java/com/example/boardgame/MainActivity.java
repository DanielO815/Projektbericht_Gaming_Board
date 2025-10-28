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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class MainActivity extends AppCompatActivity {
    private ListView listView;
    private static final String TAG = "MainActivity";               // für Log-Ausgaben
    private final List<Spieleaband2Item> cachedItems = new ArrayList<>(); // gespeicherte Liste

    private ArrayAdapter<String> arrayAdapter;
    private final List<String> items = new ArrayList<>();
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
            getCompleteTableAsync("Spieleabend2")
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

        });
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

