package com.example.boardgame;

public class Spieleaband2Item {
    private int Id;
    private String Name;
    private String Ort;
    private String Datum;
    private float EssenSterne;
    private float GastgeberSterne;
    private float AbendSterne;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getOrt() {
        return Ort;
    }

    public void setOrt(String ort) {
        Ort = ort;
    }

    public String getDatum() {
        return Datum;
    }

    public void setDatum(String datum) {
        Datum = datum;
    }

    public float getEssenSterne() {
        return EssenSterne;
    }

    public void setEssenSterne(float essenSterne) {
        EssenSterne = essenSterne;
    }

    public float getGastgeberSterne() {
        return GastgeberSterne;
    }

    public void setGastgeberSterne(float gastgeberSterne) {
        GastgeberSterne = gastgeberSterne;
    }

    public float getAbendSterne() {
        return AbendSterne;
    }

    public void setAbendSterne(float abendSterne) {
        AbendSterne = abendSterne;
    }
}