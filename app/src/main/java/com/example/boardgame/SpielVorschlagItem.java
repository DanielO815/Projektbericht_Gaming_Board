package com.example.boardgame;

public class SpielVorschlagItem {

    private int Id; // Annahme: Es gibt eine ID-Spalte
    private int Spieleabend_Id; // Wichtig: Fremdschlüssel
    private String SpielName;
    private int StimmenAnzahl;

    // Getter und Setter
    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public int getSpieleabend_Id() {
        return Spieleabend_Id;
    }

    public void setSpieleabend_Id(int spieleabend_Id) {
        Spieleabend_Id = spieleabend_Id;
    }

    public String getSpielName() {
        return SpielName;
    }

    public void setSpielName(String spielName) {
        SpielName = spielName;
    }

    public int getStimmenAnzahl() {
        return StimmenAnzahl;
    }

    public void setStimmenAnzahl(int stimmenAnzahl) {
        StimmenAnzahl = stimmenAnzahl;
    }
}