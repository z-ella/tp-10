# TP2 – Formulaire de login personnalisé avec Spring Security (corrigé)

Projet Spring Boot 3.3 / Spring Security 6 / Thymeleaf / Java 17.

## Lancer

```bash
mvn spring-boot:run
```
Puis ouvrir http://localhost:8080/login

## Comptes (authentification en mémoire)

| Utilisateur | Mot de passe | Rôle  |
|-------------|--------------|-------|
| `user`      | `1111`       | USER  |
| `admin`     | `1234`       | ADMIN |

## Tests automatisés (scénarios des étapes 7 et 8)

```bash
mvn test
```

## Structure

```
pom.xml
docs/COMPTE_RENDU.md                      <- TP résolu : explications, tests, réponses
src
 ├─ main
 │   ├─ java/ma/fstg/security
 │   │   ├─ SecurityLoginApplication.java
 │   │   ├─ config/SecurityConfig.java    <- SecurityFilterChain + UserDetailsService
 │   │   └─ web/AuthController.java       <- routes des vues
 │   └─ resources
 │       ├─ application.properties
 │       └─ templates/ login, home, admin-dashboard, user-dashboard, access-denied
 └─ test/java/ma/fstg/security/SecurityFlowTests.java
```

## Corrections apportées par rapport à l'énoncé

1. Déconnexion des dashboards en **formulaire POST** (le lien GET ne marche pas en Spring Security 6).
2. Balises `<p>` fermées, `<label for>` liés aux champs, `<meta charset>` dans toutes les pages.
3. Ajout de `GET /` (redirection vers `/home`) pour éviter une 404.
4. Ajout d'une page **403 personnalisée** (`/access-denied`).
5. Variante bonus commentée dans `SecurityConfig` : redirection selon le rôle.
