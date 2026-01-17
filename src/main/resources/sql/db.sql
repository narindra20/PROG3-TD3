-- Création de la base de données
CREATE DATABASE mini_dish_db;

-- Création de l'utilisateur avec mot de passe
CREATE USER mini_dish_db_manager WITH PASSWORD '123456';

-- Connexion à la base de données (à exécuter après création)
\c mini_dish_db;

-- Attribution des privilèges sur la base de données
GRANT CONNECT ON DATABASE mini_dish_db TO mini_dish_db_manager;

-- Attribution des privilèges sur le schéma public
GRANT USAGE ON SCHEMA public TO mini_dish_db_manager;
GRANT CREATE ON SCHEMA public TO mini_dish_db_manager;

-- Privilèges sur toutes les tables futures dans le schéma public
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT ALL PRIVILEGES ON TABLES TO mini_dish_db_manager;

-- droits sur les séquences (pour les auto-incréments)
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO mini_dish_db_manager;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO mini_dish_db_manager;