# TP2 – Personnalisation de l'authentification Spring Security
**Formulaire de login personnalisé, redirections, déconnexion et rôles**

## 1. Objectif

Remplacer la page de connexion par défaut de Spring Security par un formulaire HTML (Thymeleaf), en gérant les erreurs de connexion, les redirections après authentification, la déconnexion et la différenciation des accès selon les rôles (`USER`, `ADMIN`). Le TP fait suite au TP1 (authentification en mémoire).

## 2. Concepts

Spring Security fournit une page de login basique. On peut la remplacer sans réécrire le mécanisme d'authentification : on branche un formulaire sur les endpoints contrôlés par le framework (`/authenticate`, `/logout`). Le contrat entre l'application et Spring tient en trois éléments : l'URL de traitement, les noms des champs (`username`, `password`) et les paramètres d'URL (`error`, `logout`).

## 3. Flux d'authentification (étape 2)

| # | Étape | Composant |
|---|-------|-----------|
| 1 | L'utilisateur demande `/user/dashboard` | Navigateur |
| 2 | La requête est interceptée | `SecurityFilterChain` / `AuthorizationFilter` |
| 3 | Non authentifié → redirection vers `/login` | `ExceptionTranslationFilter` + `LoginUrlAuthenticationEntryPoint` |
| 4 | Le formulaire envoie un POST vers `/authenticate` | `login.html` |
| 5 | Extraction de `username` / `password` | `UsernamePasswordAuthenticationFilter` |
| 6 | Chargement de l'utilisateur et comparaison des mots de passe | `AuthenticationManager` → `DaoAuthenticationProvider` → `UserDetailsService` |
| 7 | Stockage de l'objet `Authentication` | `SecurityContext` (en session) |
| 8 | Succès → `/home` ; échec → `/login?error=true` | `defaultSuccessUrl` / `failureUrl` |

## 4. Réalisation

### Étape 1 – Projet
Dépendances : `spring-boot-starter-web`, `-security`, `-thymeleaf`, `spring-boot-devtools` (+ `spring-boot-starter-test` et `spring-security-test` pour les tests). Structure : `config` (sécurité), `web` (contrôleur), `templates` (vues).

### Étape 3 – `SecurityConfig`
Voir `src/main/java/ma/fstg/security/config/SecurityConfig.java`.

| Instruction | Rôle |
|---|---|
| `authorizeHttpRequests` | Règles d'accès. L'ordre compte ; `anyRequest().authenticated()` en dernier |
| `hasRole("ADMIN")` | Vérifie l'autorité `ROLE_ADMIN` (le préfixe est ajouté automatiquement) |
| `loginPage("/login")` | Utilise notre page personnalisée (GET) |
| `loginProcessingUrl("/authenticate")` | URL traitée par Spring (POST), aucun contrôleur nécessaire |
| `defaultSuccessUrl("/home", true)` | Redirection systématique vers `/home` après succès |
| `failureUrl("/login?error=true")` | Retour au formulaire avec le paramètre `error` |
| `logoutUrl` / `logoutSuccessUrl` | Déconnexion en POST, puis `/login?logout=true` |
| `accessDeniedPage("/access-denied")` | Page 403 personnalisée |
| `{noop}` | Préfixe du `DelegatingPasswordEncoder` : mot de passe en clair (TP uniquement) |

### Étape 4 – `AuthController`
Chaque méthode renvoie le nom d'un template. Aucun mapping pour `/authenticate` ni `/logout` : ces requêtes sont traitées par des filtres avant le `DispatcherServlet`. Aucune vérification de rôle dans le contrôleur.

### Étape 5 – `login.html`
- `th:action="@{/authenticate}"` : génère l'URL et ajoute le jeton CSRF.
- Champs nommés `username` et `password`.
- `${param.error}` et `${param.logout}` affichent les messages.

### Étape 6 – Pages de redirection
`home.html`, `user-dashboard.html`, `admin-dashboard.html`. La déconnexion utilise un **formulaire POST** (voir §6).

## 5. Tests (étapes 7 et 8)

Lancement : `mvn spring-boot:run`, puis http://localhost:8080/login. Utiliser une fenêtre de navigation privée.

| Action | Comportement attendu | Mécanisme | Test automatisé |
|---|---|---|---|
| Accès à `/user/dashboard` sans login | Redirection vers `/login` | `ExceptionTranslationFilter` | `accesSansLogin_redirigeVersLogin` |
| `user` / `1111` | Redirection vers `/home` | `defaultSuccessUrl("/home", true)` | `connexionReussie_redirigeVersHome` |
| Mauvais mot de passe | `/login?error=true` + message rouge | `failureUrl` | `mauvaisMotDePasse_retourneSurLoginError` |
| Déconnexion (POST) | `/login?logout=true` + message vert | `logoutSuccessUrl` | `deconnexion_redirigeVersLoginLogout` |
| `user` sur `/admin/dashboard` | 403 – Accès refusé | `hasRole("ADMIN")` | `userSansRoleAdmin_obtient403` |
| `user` sur `/user/dashboard` | Accès autorisé | `hasAnyRole("USER","ADMIN")` | `userAccedeASonEspace` |
| `admin` sur les deux espaces | Accès autorisé | `hasAnyRole("USER","ADMIN")` | `adminAccedeAuxDeuxEspaces` |

**Observations utiles (F12, onglet Réseau)** : `POST /authenticate` → `302` vers `/home` → `200`. Le cookie `JSESSIONID` est créé à la connexion.

**401/302 vs 403** : un utilisateur non connecté est redirigé vers `/login` ; un utilisateur connecté sans le bon rôle reçoit un 403. Un utilisateur non connecté qui tape `/admin/dashboard` est d'abord redirigé vers `/login`.

## 6. Problèmes classiques et corrections

| Symptôme | Cause | Solution |
|---|---|---|
| Lien « Déconnexion » sans effet (404/405) | `<a href="/logout">` envoie un GET ; Spring Security 6 exige un POST | Formulaire `method="post"` avec `th:action` |
| 403 à la soumission du login | Jeton CSRF absent (`action` au lieu de `th:action`) | Utiliser `th:action` |
| Boucle de redirection sur `/login` | `/login` non autorisé | `permitAll()` sur `/login` et dans `formLogin` |
| 404 sur `/` | Pas de mapping | `@GetMapping("/")` → `redirect:/home` |
| Accents illisibles | `charset` non déclaré | `<meta charset="UTF-8">` |
| Whitelabel Error Page sur 403 | Pas de page d'erreur dédiée | `accessDeniedPage("/access-denied")` |

## 7. Réponses aux compétences visées

1. **Flux complet** : voir §3.
2. **Formulaire personnalisé** : `loginPage` + `loginProcessingUrl` + `login.html` respectant `username`, `password`, CSRF.
3. **Redirections** : `defaultSuccessUrl` (succès), `failureUrl` (échec), `logoutSuccessUrl` (déconnexion). Avec `true`, la redirection vers `/home` est systématique ; avec `false`, l'utilisateur retournerait d'abord à la page demandée.
4. **Rôle des composants** : `HttpSecurity` configure les règles ; `SecurityFilterChain` est la chaîne de filtres qui l'applique ; `UserDetailsService` fournit les utilisateurs (ici en mémoire) ; `PasswordEncoder` compare les mots de passe.
5. **Logout propre** : formulaire POST protégé par CSRF, invalidation de la session, redirection avec message de confirmation.

## 8. Points pédagogiques clés

1. **Découplage logique/vue** : le traitement reste côté Spring, la présentation est libre.
2. **Simplicité** : un formulaire suffit ; les filtres traitent `/authenticate` et `/logout`.
3. **Sécurité contrôlée** : session, cookies, CSRF et protection contre la fixation de session gérés par Spring (à condition de respecter `th:action` et le logout en POST).
4. **Flexibilité** : comportement dépendant du rôle via un `AuthenticationSuccessHandler` (variante commentée dans `SecurityConfig`).

## 9. Limites et améliorations

- `{noop}` stocke les mots de passe en clair : utiliser `BCryptPasswordEncoder` en production.
- Utilisateurs en mémoire : perdus au redémarrage ; prochaine étape = authentification via base de données (JPA).
- Masquer un lien selon le rôle (`sec:authorize`) est de l'ergonomie, pas de la sécurité : la protection réelle est dans `SecurityConfig`.
- Redirection selon le rôle : `AuthenticationSuccessHandler`.

## 10. Conclusion

Spring Security permet de contrôler entièrement l'apparence et le parcours de connexion tout en déléguant la partie sensible (authentification, session, CSRF) au framework. L'authentification (identité) est assurée par le `UserDetailsService`, l'autorisation (droits) par `authorizeHttpRequests`, et les redirections par la configuration de `formLogin` et `logout`.
