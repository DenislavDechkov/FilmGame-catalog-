-- Създаване и избиране на базата данни
DROP DATABASE IF EXISTS filmgame_catalog;
CREATE DATABASE filmgame_catalog
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE filmgame_catalog;

-- ============================================================
-- ТАБЛИЦА: genres (жанрове)
-- ============================================================
CREATE TABLE genres (
    id          INT         AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- ТАБЛИЦА: movies (филми)
-- ============================================================
CREATE TABLE movies (
    id           INT           AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200)  NOT NULL,
    year         YEAR          NOT NULL,
    genre_id     INT           NOT NULL,
    rating       DECIMAL(3,1)  NOT NULL CHECK (rating BETWEEN 1.0 AND 10.0),
    director     VARCHAR(100),
    duration_min INT           CHECK (duration_min > 0),
    description  TEXT,
    emoji        VARCHAR(10)   DEFAULT '🎬',
    created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (genre_id) REFERENCES genres(id),
    INDEX idx_movies_year   (year),
    INDEX idx_movies_rating (rating),
    INDEX idx_movies_genre  (genre_id)
);

-- ============================================================
-- ТАБЛИЦА: games (игри)
-- ============================================================
CREATE TABLE games (
    id          INT           AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    year        YEAR          NOT NULL,
    genre_id    INT           NOT NULL,
    rating      DECIMAL(3,1)  NOT NULL CHECK (rating BETWEEN 1.0 AND 10.0),
    developer   VARCHAR(100),
    platform    VARCHAR(100),
    description TEXT,
    emoji       VARCHAR(10)   DEFAULT '🎮',
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (genre_id) REFERENCES genres(id),
    INDEX idx_games_year   (year),
    INDEX idx_games_rating (rating),
    INDEX idx_games_genre  (genre_id)
);

-- ============================================================
-- ТАБЛИЦА: users (потребители) – за бъдещо разширение
-- ============================================================
CREATE TABLE users (
    id           INT          AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    email        VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- ТАБЛИЦА: reviews (ревюта) – за бъдещо разширение
-- ============================================================
CREATE TABLE reviews (
    id         INT     AUTO_INCREMENT PRIMARY KEY,
    user_id    INT     NOT NULL,
    item_type  ENUM('movie','game') NOT NULL,
    item_id    INT     NOT NULL,
    rating     TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 10),
    comment    TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY unique_review (user_id, item_type, item_id)
);

-- ============================================================
-- ПРИМЕРНИ ДАННИ: жанрове
-- ============================================================
INSERT INTO genres (name) VALUES
    ('Екшън'),
    ('Sci-Fi'),
    ('Драма'),
    ('Трилър'),
    ('RPG'),
    ('Стратегия'),
    ('Приключение');

-- ============================================================
-- ПРИМЕРНИ ДАННИ: филми
-- ============================================================
INSERT INTO movies (title, year, genre_id, rating, director, duration_min, description, emoji)
VALUES
    ('Dune: Part Two',       2024, 2, 8.8, 'Denis Villeneuve',  166, 'Пол Атрейдес обединява силите си с фременките срещу Харконените.', '🏜️'),
    ('Oppenheimer',          2023, 3, 8.9, 'Christopher Nolan', 180, 'Биографичен трилър за създателя на атомната бомба.', '☢️'),
    ('Deadpool & Wolverine', 2024, 1, 7.8, 'Shawn Levy',        128, 'Дедпул и Върколакът се сдружават за мисия в мултивселената.', '⚔️'),
    ('Interstellar',         2014, 2, 8.7, 'Christopher Nolan', 169, 'Астронавти търсят нов дом за човечеството отвъд галактиката.', '🌌'),
    ('Joker',                2019, 3, 8.4, 'Todd Phillips',      122, 'Артур Флек се превръща в прочутия Жокер.', '🃏'),
    ('Alien: Romulus',       2024, 4, 7.3, 'Fede Álvarez',       119, 'Млади колонисти намират изоставена космическа станция.', '👽'),
    ('The Batman',           2022, 1, 7.9, 'Matt Reeves',         176, 'Брус Уейн разследва серия убийства в Готъм Сити.', '🦇'),
    ('Mad Max: Fury Road',   2015, 1, 8.1, 'George Miller',      120, 'Макс и Фюриоза бягат от тиранина Безумния Джо.', '🔥');

-- ============================================================
-- ПРИМЕРНИ ДАННИ: игри
-- ============================================================
INSERT INTO games (title, year, genre_id, rating, developer, platform, description, emoji)
VALUES
    ('Elden Ring',         2022, 5, 9.4, 'FromSoftware',     'PC / PS5 / Xbox',   'Отворен свят RPG – изследвай Земите Между.', '⚔️'),
    ("Baldur's Gate 3",    2023, 5, 9.6, 'Larian Studios',   'PC / PS5',          'Тактически RPG базиран на D&D с пълна свобода.', '🧙'),
    ('Cyberpunk 2077',     2020, 5, 8.2, 'CD Projekt Red',   'PC / PS5 / Xbox',   'Дистопичен Найт Сити – хакери и корпорации.', '🌆'),
    ('Hades',              2020, 1, 9.5, 'Supergiant Games', 'PC / Switch / PS5', 'Roguelite в митологична Гърция.', '🔱'),
    ('Hollow Knight',      2017, 7, 9.1, 'Team Cherry',      'PC / Switch / PS4', 'Метроидвания в тъмното подземно кралство.', '🦋'),
    ('Civilization VI',    2016, 6, 8.8, 'Firaxis Games',    'PC / Mobile',       'Изгради цивилизация от каменната ера до космоса.', '🏛️'),
    ('God of War: Ragnarök',2022,7, 9.0, 'Santa Monica',     'PS5 / PS4',         'Кратос и Атреус пътуват из деветте царства.', '🪓'),
    ('The Last of Us I',   2022, 7, 9.2, 'Naughty Dog',      'PS5 / PC',          'Постапокалиптично пътуване на Джоул и Елий.', '🌿');

-- ============================================================
-- ИЗГЛЕДИ (VIEWS) – удобни заявки
-- ============================================================

-- Обединен изглед: всички елементи от каталога
CREATE VIEW v_catalog AS
    SELECT
        m.id,
        'movie'      AS type,
        m.title,
        m.year,
        g.name       AS genre,
        m.rating,
        m.director   AS extra_field_1,
        CAST(m.duration_min AS CHAR) AS extra_field_2,
        m.description,
        m.emoji
    FROM movies m
    JOIN genres g ON m.genre_id = g.id

    UNION ALL

    SELECT
        ga.id,
        'game'       AS type,
        ga.title,
        ga.year,
        g.name       AS genre,
        ga.rating,
        ga.developer AS extra_field_1,
        ga.platform  AS extra_field_2,
        ga.description,
        ga.emoji
    FROM games ga
    JOIN genres g ON ga.genre_id = g.id;
