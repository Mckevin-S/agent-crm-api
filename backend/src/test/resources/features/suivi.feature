# language: fr
Fonctionnalité: Traitement des recommandations
  Une recommandation acceptée devient une tâche ; une recommandation écartée disparaît
  pendant 90 jours, avec un motif tracé.

  Contexte:
    Soit nous sommes le "2026-11-20"
    Et le conseiller "Mckevin Kamguem" gère le client "C-10001" né le "1990-06-01"
    Et le client a les contrats suivants :
      | numéro | produit | statut | échéance dans |
      | AUT-1  | AUTO    | ACTIVE | 30            |
    Et le dernier échange avec le client date de 20 jours

  Scénario: Accepter une recommandation crée une tâche et la retire de la liste
    Quand le conseiller accepte la recommandation "RENEWAL_SOON" sur "AUT-1"
    Alors une tâche ouverte "Préparer le renouvellement – Auto AUT-1" est assignée au conseiller
    Et la recommandation "RENEWAL_SOON" n'est plus proposée

  Scénario: Écarter une recommandation exige un motif
    Quand le conseiller écarte la recommandation "HOME_CROSS_SELL" sans motif
    Alors l'action est refusée avec le code "REASON_REQUIRED"

  Scénario: Une recommandation écartée ne réapparaît pas
    Quand le conseiller écarte la recommandation "HOME_CROSS_SELL" avec le motif "Logé chez ses parents"
    Alors la recommandation "HOME_CROSS_SELL" n'est plus proposée

  Scénario: Une recommandation déjà traitée ne peut pas être acceptée
    Quand le conseiller accepte la recommandation "RENEWAL_SOON" sur "AUT-1"
    Et le conseiller accepte la recommandation "RENEWAL_SOON" sur "AUT-1"
    Alors l'action est refusée avec le code "RECOMMENDATION_NOT_AVAILABLE"
