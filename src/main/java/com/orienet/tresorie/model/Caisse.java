package com.orienet.tresorie.model;

import javax.persistence.*;

import java.math.BigDecimal;

@Entity
public class Caisse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String nom;
    private BigDecimal encaissement;   // total des encaissements validés
    private BigDecimal decaissement;
    private BigDecimal creance;
    private BigDecimal dette;
    private BigDecimal cashDisponible;

    // ─── Getters ───────────────────────────────────────────────────
    public int getId() { return id; }
    public String getNom() { return nom; }
    public BigDecimal getEncaissement() { return encaissement; }
    public BigDecimal getDecaissement() { return decaissement; }
    public BigDecimal getCreance() { return creance; }
    public BigDecimal getDette() { return dette; }
    public BigDecimal getCashDisponible() { return cashDisponible; }

    // ─── Setters ───────────────────────────────────────────────────
    public void setId(int id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setEncaissement(BigDecimal encaissement) { this.encaissement = encaissement; }
    public void setDecaissement(BigDecimal decaissement) { this.decaissement = decaissement; }
    public void setCreance(BigDecimal creance) { this.creance = creance; }
    public void setDette(BigDecimal dette) { this.dette = dette; }
    public void setCashDisponible(BigDecimal cashDisponible) { this.cashDisponible = cashDisponible; }
}