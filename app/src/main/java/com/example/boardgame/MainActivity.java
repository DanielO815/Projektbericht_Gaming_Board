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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    private LinearLayout putCardViewLayoutHereInside;

    private ScrollView scrollView;

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

    // Konstante für die Laufzeitberechtigung (WICHTIG!)
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;


    @SuppressLint("WrongViewCast")
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

        scrollView=findViewById(R.id.scrollView);
        putCardViewLayoutHereInside = findViewById(R.id.putCardViewLayoutHereInside);

        // 1. Logik für den Lade-Button (button2)
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

            //Tabelle Spieleabend aktualisieren
            updateSpieleabend();

            // terminIDs aus der DB laden
            getCompleteTableAsync("Spieleabend2").whenComplete((list, ex) -> {
                // Zurück auf dem UI-Thread
                handler.post(() -> {
                    if (ex != null) {
                        Log.e("LoadTermine", "Fehler beim Laden der Termine", ex);
                        Toast.makeText(MainActivity.this, "Fehler: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
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
                        createCardForDate(list);
                        Toast.makeText(MainActivity.this, "Termine geladen!", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(MainActivity.this, "Keine Termine gefunden.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        // 2. Logik für  Verspäte Mich!-Button
        Button contactButton = findViewById(R.id.contactButton); //
        contactButton.setOnClickListener(v -> {
            // Startet den Berechtigungscheck und sendet dann die SMS
            requestSmsPermissionAndSend();
        });
    }

    // SMS Methode, benötigt Berechtigung. Zum Test habe ich in Manifest Datei statische Berechtigung vergeben.

    private void requestSmsPermissionAndSend() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            // Berechtigung wenn nicht erteilt, anfordern
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
        } else {
            // Berechtigung da, senden
            sendSmsToContacts();
        }
    }

    // Test SMS. Dort könnte man Kontakt Nummern hinzufügen, für die Gruppe z.B.
    private void sendSmsToContacts() {
        // Die zu sendende Nachricht (wie gewünscht)
        final String SMS_MESSAGE = "Ich verspäte mich!";

        List<String> contactNumbers = new ArrayList<>();

        // TEST-Nummer für Emulator:
        contactNumbers.add("5554");
        // contactNumbers.add();


        sendSMS(contactNumbers, SMS_MESSAGE);
    }

    // Laufzeit Berechtigung für SMS
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Berechtigung erteilt! Senden der SMS.
                sendSmsToContacts();
            } else {
                // Berechtigung verweigert.
                Toast.makeText(this, "Berechtigung zum Senden von SMS verweigert. SMS wurde nicht gesendet.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Exeption falls keine Nmmer gefunden wurde.
    private void sendSMS(List<String> phoneNumbers, String message) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            Toast.makeText(this, "Keine Telefonnummern zum Senden gefunden.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();

            for (String phoneNumber : phoneNumbers) {
                // Senden der Nachricht
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }

            // Rückmeldung im UI-Thread
            handler.post(() -> {
                Toast.makeText(MainActivity.this, "TEST-SMS gesendet: " + message, Toast.LENGTH_LONG).show();
            });

        } catch (Exception e) {
            Log.e("SendSMS", "Fehler beim Senden der SMS", e);
            handler.post(() -> {
                Toast.makeText(MainActivity.this, "SMS-Fehler: Berechtigung, Nummer oder Dienst nicht verfügbar.", Toast.LENGTH_LONG).show();
            });
        }
    }


    

    private void updateSpieleabend(){
        getCompleteTableAsync("Spieleabend2").whenComplete((list, ex) -> {
            // Zurück auf dem UI-Thread
            handler.post(() -> {
                if (ex != null) {
                    Log.e("LoadTermine", "Fehler beim Laden der Termine", ex);
                    Toast.makeText(MainActivity.this, "Fehler: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                } else {
                    Toast.makeText(MainActivity.this, "Keine Termine gefunden.", Toast.LENGTH_SHORT).show();
                }
            });
            String lastElementDate=list.get(list.size()-1).getDatum().trim();
            String dayDiff=daysBetweenToday(lastElementDate);
            if (!dayDiff.equals("Error")) {
                List<String> namen=Arrays.asList("Michel","Roger","Daniel");
                List<String> orte = Arrays.asList("Magedeburgerstr. 10","Berliner Str. 50","Münchenerstr. 90");
                int indexName=namen.indexOf(list.get(list.size()-1).getName().trim());
                int dayValue = Integer.parseInt(dayDiff.trim());
                if (dayValue<14) {
                    //Toast.makeText(MainActivity.this, "Neue Spielabende werden erzeugt", Toast.LENGTH_SHORT).show();
                    int dayCounter=7;
                    String name;
                    String ort;
                    String lastDay=list.get(list.size()-1).getDatum().trim();
                    for (int i = 0; (i+  dayValue) <14; i+=7)
                    {
                        name=namen.get(((i+indexName+1)%3));
                        ort=orte.get(((i+indexName+1)%3));
                        //einen neuen Spieleabend hinzufügen
                        addNewSpieleabend(name,ort,lastDay,dayCounter);
                        dayCounter+=7;
                    }
                }
            }
        });
    }


    private void addNewSpieleabend(String name, String ort, String letztesDatum,int dayCounter){
        String newDate;
        JSONObject newSpieleabendValues = new JSONObject();
        try {
            newSpieleabendValues.put("Name", name);
            newSpieleabendValues.put("Ort", ort);
            newDate=dateCalculater(letztesDatum,dayCounter);
            newSpieleabendValues.put("Datum", newDate);
            newSpieleabendValues.put("EssenSterne", "0");
            newSpieleabendValues.put("GastgeberSterne","0");
            newSpieleabendValues.put("AbendSterne","0");
            //Test
            /*newSpieleabendValues.put("Name", "Michel");
            newSpieleabendValues.put("Ort", "Magedeburgerstr. 10");
            newSpieleabendValues.put("Datum", "29.12.2023");
            newSpieleabendValues.put("Spiele","TestSpiel");
            */
        } catch (Exception e) {
            Log.e("JSON", "Fehler beim Spieleabend erzeugen", e);
            Toast.makeText(MainActivity.this, "Fehler beim Spieleabend erzeugen", Toast.LENGTH_SHORT).show();
            return; // Abbruch
        }

        // insertRow
        insertRow("Spieleabend2", newSpieleabendValues).whenComplete((response, exInsert) -> {
            handler.post(() -> {
                if (exInsert != null) {
                    Toast.makeText(MainActivity.this, "Fehler beim Erzeugen neuer Termine", Toast.LENGTH_SHORT).show();
                    Log.e("InsertRow", "Fehler", exInsert);
                    return;
                }
                Log.i("InsertRow", "Antwort: " + response);
            });
        });

    }

    private String dateCalculater(String dateStr,int plusDays){
        String result = "";
        String datePattern="dd.MM.uuuu";
        LocalDate date;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(datePattern)
                    .withResolverStyle(ResolverStyle.STRICT);
            date=LocalDate.parse(dateStr,fmt);
            date=date.plusDays(plusDays);
            result=date.format(fmt);
        }
        return result;
    }

    // gibt positive Tage für Zukunft, negative für Vergangenheit
    public static String daysBetweenToday(String dateStr) {
        DateTimeFormatter fmt = null; // strenge Prüfung (z.B. 31.02. ist ungültig)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fmt = DateTimeFormatter.ofPattern("dd.MM.uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);
        }

        ZoneId zone = null; // deine Geräte-Zeitzone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            zone = ZoneId.systemDefault();
        }
        LocalDate target = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            target = LocalDate.parse(dateStr, fmt);
        }
        LocalDate today  = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            today = LocalDate.now(zone);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Long.toString(ChronoUnit.DAYS.between(today, target));
        }else{
            return "Error";
        }
    }

    // Methode, um Rating in der DB zu aktualisieren
    private void updateRatingInDB(String terminID, String ratingColumn, float rating) {

        JSONObject valuesToUpdate = new JSONObject();
        try {
            valuesToUpdate.put(ratingColumn, rating);
        } catch (JSONException e) {
            Log.e("UpdateRating", "JSON-Fehler beim Erstellen von 'values' für " + ratingColumn, e);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(terminID);
        } catch (NumberFormatException e) {
            Log.e("UpdateRating", "Konnte terminID '" + terminID + "' nicht in eine Zahl umwandeln.", e);
            return;
        }

        updateRow("Spieleabend2", valuesToUpdate, "Id", id).whenComplete((response, ex) -> {
            if (ex != null) {
                Log.e("UpdateRating", "Fehler beim DB-Update für " + ratingColumn, ex);
            } else {
                Log.i("UpdateRating", "DB-Update erfolgreich für " + ratingColumn + ": " + response);
            }
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

    private int getListIndex(List<Spieleaband2Item> list, int filterString){
        int result = -1;
        for (int i=0;i<list.size();i++){
            Spieleaband2Item Spiel =list.get(i);
            if (Spiel.getId()==filterString){
                result=i;
                break;
            }
        }
        return result;
    }


    // Erstellt Kartenansichten basierend auf den 'terminIDs'
    private void createCardForDate(List<Spieleaband2Item> list) {

        putCardViewLayoutHereInside.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int dayCounter=-1;
        int listIndex;
        boolean dateInFuture=false;
        // Iteriere über die terminIDs Liste
        for (final String terminID : terminIDs) {

            View itemView = inflater.inflate(R.layout.cardview_per_date_item, putCardViewLayoutHereInside, false);

            // Elemente der Karte finden
            final TextView textViewDate = itemView.findViewById(R.id.textViewWannNaechsterTermin);
            final TextView textViewAddress = itemView.findViewById(R.id.textViewAdresse);
            final EditText localEditTextText = itemView.findViewById(R.id.editTextText);
            final Button buttonVorschlagEingeben = itemView.findViewById(R.id.buttonVorschlagEingeben);
            final RadioGroup localRadioGroupSpiele = itemView.findViewById(R.id.radioGroupSpiele);

            listIndex=getListIndex(list,Integer.parseInt(terminID));
            Spieleaband2Item Spiel=list.get(listIndex);
            if(istInDerVergangenheit(Spiel.getDatum())){
                dayCounter+=1;//Zähler erhöhen
                dateInFuture=false;
            }else{
                dateInFuture=true;
            }

            // CardView Inhalte aus der DB laden

            // Datum laden
            fetchSpiele("Spieleabend2", "Id", terminID, "Datum").whenComplete((datum, ex) -> {
                handler.post(() -> {
                    if (ex != null) textViewDate.setText("Fehler beim Laden des Datums");
                    else{
                        textViewDate.setText("Datum: " + datum.trim());

                    }
                });
            });
            // Ort und Name laden
            fetchSpiele("Spieleabend2", "Id", terminID, "Name").whenComplete((name, exName) -> {
                fetchSpiele("Spieleabend2", "Id", terminID, "Ort").whenComplete((ort, exOrt) -> {
                    handler.post(() -> {
                        if (exName != null || exOrt != null) {
                            textViewAddress.setText("Fehler beim Laden der Adresse");
                        } else {
                            textViewAddress.setText("Wo: " + name.trim() + ", " + ort.trim());
                        }
                    });
                });
            });


            // RatingBars
            final RatingBar cardRatingBarGastgeberIn = itemView.findViewById(R.id.ratingBarGastgeberIn);
            final RatingBar cardRatingBarEssen = itemView.findViewById(R.id.ratingBarEssen);
            final RatingBar cardRatingBarAbend = itemView.findViewById(R.id.ratingBar);

            if(dateInFuture){
                cardRatingBarGastgeberIn.setEnabled(false);
                cardRatingBarEssen.setEnabled(false);
                cardRatingBarAbend.setEnabled(false);
                cardRatingBarGastgeberIn.setIsIndicator(true);
                cardRatingBarEssen.setIsIndicator(true);
                cardRatingBarAbend.setIsIndicator(true);
            }else{
                buttonVorschlagEingeben.setEnabled(false);
            }

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

                                        updateRatingInDB(terminID, "EssenSterne", rating);
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

                                        updateRatingInDB(terminID, "GastgeberSterne", rating);
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

                                        updateRatingInDB(terminID, "AbendSterne", rating);
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
        if (dayCounter>-1) {
            showCardImmediatelyFullPage(dayCounter+1);
        }// Auf nächsten Termin setzen
    }

    public boolean istInDerVergangenheit(String datumString) {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        format.setLenient(false); // Strenge Prüfung des Datumsformats
        boolean result = false;
        try {
            Date datum = format.parse(datumString);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate gegeben=datum.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate gestern = LocalDate.now().minusDays(1);
                result= gegeben.isBefore(gestern);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return false; // oder Fehlerbehandlung je nach Bedarf
        }
        return result;
    }


    private void showCardImmediatelyFullPage(int desiredIndex) {
        if (scrollView == null) return;

        final ViewTreeObserver vto = scrollView.getViewTreeObserver();
        final int safeIndex = clampIndex(desiredIndex, putCardViewLayoutHereInside.getChildCount());

        final boolean[] heightsApplied = {false};
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                // 1. PreDraw: Höhe der ScrollView ist bekannt -> allen Cards diese Höhe geben
                if (!heightsApplied[0]) {
                    heightsApplied[0] = true;

                    int pageHeight = scrollView.getHeight();
                    for (int i = 0; i < putCardViewLayoutHereInside.getChildCount(); i++) {
                        View child = putCardViewLayoutHereInside.getChildAt(i);
                        ViewGroup.LayoutParams lp = child.getLayoutParams();
                        lp.width  = ViewGroup.LayoutParams.MATCH_PARENT;
                        lp.height = pageHeight; // fixiere die Höhe auf eine "Seite"
                        child.setLayoutParams(lp);
                    }
                    // Einmal neu layouten lassen, dann erneut PreDraw bekommen
                    putCardViewLayoutHereInside.requestLayout();
                    return false; // diesen Draw abbrechen, auf den nächsten warten
                }

                // 2. PreDraw: Höhen sind gesetzt -> jetzt deterministisch positionieren
                scrollView.getViewTreeObserver().removeOnPreDrawListener(this);
                int y = safeIndex * scrollView.getHeight();
                scrollView.scrollTo(0, y); // sofort, ohne Animation/Flackern
                return true;
            }
        });
    }

    private int clampIndex(int desired, int count) {
        if (count <= 0) return 0;
        return Math.max(0, Math.min(desired, count - 1));
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