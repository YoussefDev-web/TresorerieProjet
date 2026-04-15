<style>
  body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; }
  h1 { color: #2d5a9a; border-bottom: 2px solid #2d5a9a; padding-bottom: 0.2em; font-size: 24px; }
  h2 { color: #1e3a50; margin-top: 1.5em; font-size: 20px; }
  p, li { font-size: 14px; }
  .layer { background: #f8f9fa; border-left: 4px solid #2d5a9a; padding: 12px; margin: 15px 0; border-radius: 4px; }
  .code-term { background: #eef; color: #c00; padding: 2px 5px; font-family: monospace; border-radius: 3px; font-weight: bold; }
</style>

# Architecture Spring Boot : Controller, Service et Repository

Dans le développement Java avec Spring Boot, l'architecture standard suit le modèle **MVC (Modèle-Vue-Contrôleur)** structuré en "couches". L'objectif principal est de séparer la base de données, la logique algorithmique et l'affichage web pour qu'ils soient indépendants.

Voici l'explication des 3 couches clés de votre projet.

---

## 1. Le Repository (Couche d'Accès aux Données)

<div class="layer">
<strong>Exemple :</strong> <span class="code-term">OperationRepository.java</span>
</div>

Le **Repository** est la seule porte d'entrée et de sortie vers votre base de données MySQL. Il ne contient strictement *aucune* logique métier.

- **Que fait-il ?** Il hérite de `JpaRepository`, l'outil Spring qui lui confère automatiquement des méthodes prêtes à l'emploi (`save()`, `findAll()`, `findById()`, `delete()`).
- **Création de requêtes :** C’est dans ce fichier que l'on déclare les requêtes spécifiques à la main via JPQL, par exemple : `@Query("SELECT SUM(o.montant) FROM Operation o WHERE o.etat = 'Validé'")` pour calculer la somme des caisses. 

---

## 2. Le Service (Couche Logique Métier)

<div class="layer">
<strong>Exemple :</strong> <span class="code-term">OperationService.java</span> ou <span class="code-term">CaisseService.java</span>
</div>

Le **Service** agit comme le *cerveau* de l'application. C’est la couche la plus importante conceptuellement, puisqu’elle contient toutes les règles de gestion de votre projet.

- **Que fait-il ?** Il orchestre l'application. Lorsqu'une opération est modifiée, c'est le Service qui recalcule automatiquement les variables des caisses impactées (Encaissement, Décaissement, etc.), qui gère l'extraction des champs d'une base JSON, et qui manipule les objets en mémoire.
- **Pourquoi isoler cette couche ?** L'avantage est la durabilité : si votre application Web PFE devient plus tard une Application Mobile, toute la logique métier enfermée dans votre Service restera identique et ne nécessitera d'être recodée.

---

## 3. Le Controller (Couche de Routage Web)

<div class="layer">
<strong>Exemple :</strong> <span class="code-term">OperationController.java</span>
</div>

Le **Controller** est l'aiguilleur du trafic. Son rôle unique est d'intercepter les requêtes HTTP (clics) du navigateur web de l'utilisateur et d'organiser la réponse correspondante.

- **Comment fonctionne-t-il ?** 
  1. Il "écoute" une URL spécifique (ex: `@PostMapping("/operations/ajouter")`).
  2. Il réceptionne les données du formulaire rempli par l'utilisateur.
  3. Il **délègue** la demande au Service, sans faire de calcul compliqué lui-même.
  4. Dès que le Service termine, il sélectionne la vue Thymeleaf associée (`nouvelle-operation.html` par exemple) tout en lui transmettant un `Model` contenant les données requises pour le rendu web.
- Le Controller ne fait aucune mathématique interne et de communication SQL directe : il sert uniquement de point de liaison Web ↔ Logique.

---

## Cas Pratique : Le Cycle d'une Requête

Imaginons que l'utilisateur ajoute une nouvelle entrée dans le **Flux de Trésorerie** :

1. L'utilisateur clique sur **Sauvegarder** depuis l'interface web HTTP.
2. Le **<span class="code-term">Controller</span>** réceptionne ce click.
3. Il envoie immédiatement les données brutes au **<span class="code-term">Service</span>**. Ce dernier va parser le JSON des champs personnalisés dynamiques, calculer les ajustements financiers nécessaires pour la caisse, tout conformer à vos règles comptables.
4. Une fois l'entité `Operation` finalisée mathématiquement, ce Service ordonne au **<span class="code-term">Repository</span>** de la persister définitivement.
5. Le Repository dialogue silencieusement avec le serveur MySQL via JPA.
6. Le Controller est averti de la fin du programme par le Service et orchestre un message de Succès sur l'écran final de l'utilisateur en retournant la vue.

*Structurer votre projet Web PFE de la sorte garantit qu'il est testable, évolutif et modulaire !*
