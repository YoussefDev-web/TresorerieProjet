# Diagrammes de Séquence (Avec Base de Données)

Ces diagrammes mettent en évidence les interactions classiques entre l'utilisateur, l'application (Système) et le stockage (Base de données).

## 1. Authentification (Connexion au système)

```mermaid
sequenceDiagram
    autonumber
    actor User as Utilisateur / Admin
    participant System as Système (Application)
    participant DB as Base de Données

    User->>System: Saisit l'identifiant et le mot de passe
    System->>DB: Requête pour récupérer l'utilisateur (SELECT)
    
    alt Utilisateur non trouvé ou mot de passe incorrect
        DB-->>System: Retourne un résultat vide
        System-->>User: Affiche une erreur "Identifiants invalides"
    else Identifiants corrects
        DB-->>System: Retourne les données de l'utilisateur
        System->>System: Crée la session sécurisée
    end
```
