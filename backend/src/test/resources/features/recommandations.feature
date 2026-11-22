# language: fr
Fonctionnalité: Recommandations sur la fiche client
  Pour ne laisser passer aucune échéance ni opportunité,
  en tant que conseiller,
  je veux que la fiche d'un client me propose les prochaines actions à mener.

  Contexte:
    Soit nous sommes le "2026-11-20"
    Et le conseiller "Mckevin Kamguem" gère le client "C-10001" né le "1985-03-10"

  Scénario: Une échéance très proche est prioritaire
    Soit le client a les contrats suivants :
      | numéro | produit | statut    | échéance dans |
      | AUT-1  | AUTO    | ACTIVE    | 10            |
      | MRH-1  | HOME    | ACTIVE    | 200           |
    Et le dernier échange avec le client date de 20 jours
    Quand le conseiller ouvre la fiche du client
    Alors la recommandation "RENEWAL_SOON" sur "AUT-1" est proposée en priorité "HIGH"

  Scénario: Un sinistre qui traîne doit être suivi
    Soit le client a les contrats suivants :
      | numéro | produit | statut | échéance dans |
      | MRH-1  | HOME    | ACTIVE | 200           |
    Et le client a un sinistre "SIN-7" ouvert depuis 45 jours
    Et le dernier échange avec le client date de 20 jours
    Quand le conseiller ouvre la fiche du client
    Alors la recommandation "STALE_CLAIM" sur "SIN-7" est proposée en priorité "MEDIUM"

  Scénario: Un client sans nouvelles depuis six mois doit être recontacté
    Soit le client a les contrats suivants :
      | numéro | produit | statut | échéance dans |
      | MRH-1  | HOME    | ACTIVE | 200           |
    Et le dernier échange avec le client date de 200 jours
    Quand le conseiller ouvre la fiche du client
    Alors la recommandation "NO_RECENT_CONTACT" est proposée en priorité "MEDIUM"

  Scénario: Un échange tracé aujourd'hui fait disparaître la reprise de contact
    Soit le client a les contrats suivants :
      | numéro | produit | statut | échéance dans |
      | MRH-1  | HOME    | ACTIVE | 200           |
    Quand le conseiller trace un appel sortant "Point annuel"
    Et le conseiller ouvre la fiche du client
    Alors la recommandation "NO_RECENT_CONTACT" n'est plus proposée
