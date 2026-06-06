# Diagramme de Séquence : Ajout d'une Opération de Trésorerie

Ce document présente le flux d'interactions lors de la création d'une nouvelle opération de trésorerie (comme un décaissement) incluant les validations métier et le recalcul automatique des totaux de la caisse impactée.

```mermaid
sequenceDiagram
    autonumber
    actor User as Utilisateur
    participant Ctrl as OperationController
    participant OpSvc as OperationService
    participant CaisseSvc as CaisseService
    participant OpRepo as OperationRepository
    participant CaisseRepo as CaisseRepository
    participant DB as Base de Données

    User->>Ctrl: POST /operations/save (montant, caisse, natureFlux="Décaissement")
    activate Ctrl
    
    Ctrl->>OpSvc: sauvegarder(operation)
    activate OpSvc
    
    %% Étape 1 : Vérification de la caisse
    OpSvc->>CaisseSvc: findByNom(operation.getCaisse().getNom())
    activate CaisseSvc
    CaisseSvc->>CaisseRepo: findByNom()
    CaisseRepo-->>CaisseSvc: Optional<Caisse>
    CaisseSvc-->>OpSvc: retourne la Caisse
    deactivate CaisseSvc
    
    %% Étape 2 : Règles Métier (Validation)
    alt Caisse introuvable
        OpSvc-->>Ctrl: throw EntityNotFoundException
        Ctrl-->>User: Affiche message d'erreur "Caisse invalide"
    else Si natureFlux == 'Décaissement'
        Note over OpSvc: Vérification du Cash Disponible
        alt Cash Disponible < Montant Opération
            OpSvc-->>Ctrl: throw InsufficientFundsException
            Ctrl-->>User: Affiche message d'erreur "Fonds insuffisants"
        end
    end
    
    %% Étape 3 : Sauvegarde de l'opération
    Note over OpSvc: Validation OK
    OpSvc->>OpRepo: save(operation)
    activate OpRepo
    OpRepo->>DB: INSERT INTO operation
    DB-->>OpRepo: OK
    OpRepo-->>OpSvc: operation sauvegardée
    deactivate OpRepo
    
    %% Étape 4 : Recalcul des totaux de la caisse impactée
    OpSvc->>CaisseSvc: recalculerTotaux(nomCaisse)
    activate CaisseSvc
    
    CaisseSvc->>OpRepo: sumEncaissementByCaisse(nomCaisse)
    OpRepo-->>CaisseSvc: totalEncaissement
    
    CaisseSvc->>OpRepo: sumDecaissementByCaisse(nomCaisse)
    OpRepo-->>CaisseSvc: totalDecaissement
    
    Note over CaisseSvc: cashDisponible = encaissement - decaissement
    
    CaisseSvc->>CaisseRepo: save(caisseMiseAJour)
    activate CaisseRepo
    CaisseRepo->>DB: UPDATE caisse
    DB-->>CaisseRepo: OK
    CaisseRepo-->>CaisseSvc: caisse mise à jour
    deactivate CaisseRepo
    
    CaisseSvc-->>OpSvc: recalcul terminé
    deactivate CaisseSvc
    
    OpSvc-->>Ctrl: operation sauvegardée
    deactivate OpSvc
    
    %% Étape 5 : Réponse à l'utilisateur
    Ctrl-->>User: Redirection (302) vers /operations avec message de succès
    deactivate Ctrl
```
