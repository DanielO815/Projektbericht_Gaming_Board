package com.example.boardgame;

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

    final ArrayList<String> listeSpielVerschlaege = new ArrayList<>();
    final HashMap<String, Integer> gameVotes = new HashMap<>();

    private RadioGroup radioGroupSpiele;
    private EditText editTextText;

    private String lastVotedGame = null;

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

        editTextText = findViewById(R.id.editTextText);
        final Button buttonVorschlagEingeben = findViewById(R.id.buttonVorschlagEingeben);
        radioGroupSpiele = findViewById(R.id.radioGroupSpiele);

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
}