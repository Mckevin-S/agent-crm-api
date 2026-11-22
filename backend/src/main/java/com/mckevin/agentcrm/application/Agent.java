package com.mckevin.agentcrm.application;

/** Utilisateur connecté : un conseiller voit son portefeuille, un manager voit toute l'agence. */
public record Agent(long id, String fullName, boolean manager) {
}
