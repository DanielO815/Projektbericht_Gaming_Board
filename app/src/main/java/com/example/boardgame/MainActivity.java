package com.example.boardgame;

import android.os.Bundle;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private RatingBar ratingBarGastgeberIn;
    private RatingBar ratingBarEssen;
    private RatingBar ratingBarAbend;

    private TextView textGastgeberInBewerten;
    private TextView textWieWarDasEssen;
    private TextView textWieWarDerAbend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // View ini
        initializeViews();

        //  Logik der Ratingbar
        setupGastgeberRating();
        setupEssenRating();
        setupAbendRating();


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

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