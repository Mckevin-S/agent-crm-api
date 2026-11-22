# language: fr
Fonctionnalité: Mode dégradé quand le CRM est en panne
  Le conseiller doit pouvoir continuer à travailler, avec des données signalées comme non à jour.

  Contexte:
    Soit nous sommes le "2026-11-20"
    Et le conseiller "Mckevin Kamguem" gère le client "C-10001" né le "1985-03-10"
    Et le client a les contrats suivants :
      | numéro | produit | statut | échéance dans |
      | AUT-1  | AUTO    | ACTIVE | 10            |

  Scénario: Une fiche déjà consultée reste disponible pendant la panne
    Soit le conseiller a déjà ouvert la fiche du client
    Et le CRM tombe en panne
    Quand le conseiller ouvre la fiche du client
    Alors la fiche est affichée à partir de la copie locale
    Et la recommandation "RENEWAL_SOON" sur "AUT-1" est proposée en priorité "HIGH"

  Scénario: Une fiche jamais consultée ne peut pas être affichée pendant la panne
    Soit le CRM tombe en panne
    Quand le conseiller ouvre la fiche du client
    Alors l'erreur "CRM indisponible" est signalée

  Scénario: Un conseiller ne voit pas les clients d'un collègue
    Soit le client "C-10002" est géré par un autre conseiller
    Quand le conseiller ouvre la fiche du client "C-10002"
    Alors l'accès est refusé
