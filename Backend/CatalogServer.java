package catalog;

import catalog.models.CatalogItem;
import catalog.models.Movie;
import catalog.models.Game;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Прост HTTP REST API сървър за каталога.
 *
 * Endpoints:
 *   GET  /api/catalog           – всички записи (JSON масив)
 *   GET  /api/catalog?type=movie – само филми
 *   GET  /api/catalog?type=game  – само игри
 *   GET  /api/catalog?genre=RPG  – по жанр
 *   POST /api/catalog           – добавяне (body: JSON)
 *   DELETE /api/catalog?id=5&type=movie – изтриване
 */
public class CatalogServer {

    private static final int PORT = 8080;
    private static DatabaseConnection db;

    // ---- main() – входна точка ----
    public static void main(String[] args) throws Exception {

        // Инициализиране на базата данни
        try {
            db = new DatabaseConnection();
        } catch (Exception e) {
            System.err.println("⚠️  Базата данни е недостъпна: " + e.getMessage());
            System.err.println("    Сървърът ще работи с demo данни в паметта.");
            db = null;
        }

        // Стартиране на HTTP сървъра
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Регистриране на маршрутите
        server.createContext("/api/catalog", CatalogServer::handleCatalog);
        server.createContext("/api/health",  CatalogServer::handleHealth);

        server.setExecutor(null); // използва default executor
        server.start();

        System.out.println("🚀 Сървърът работи на http://localhost:" + PORT);
        System.out.println("   Endpoints:");
        System.out.println("   GET  http://localhost:" + PORT + "/api/catalog");
        System.out.println("   POST http://localhost:" + PORT + "/api/catalog");
    }

    // ============================================================
    // Главен handler за /api/catalog
    // ============================================================
    /**
     * @param ex
     * @throws IOException
     */
    private static void handleCatalog(HttpExchange ex) throws IOException {

        // CORS хедъри (необходими за браузъра)
        ex.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");

        // OPTIONS preflight заявка
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            sendResponse(ex, 204, "");
            return;
        }

        String method = ex.getRequestMethod().toUpperCase();

        try {
            switch (method) {
                case "GET"    -> handleGet(ex);
                case "POST"   -> handlePost(ex);
                case "DELETE" -> handleDelete(ex);
                default       -> sendResponse(ex,405, "{\"error\":\"Методът не е позволен\"}");
            }
        } catch (Exception e) {
            System.err.println("Грешка: " + e.getMessage());
            sendResponse(ex, 500, "{\"error\":\"Вътрешна сървърна грешка\"}");
        }
    }

    // ---- GET: връща JSON масив с всички записи ----
    private static void handleGet(HttpExchange ex) throws Exception {
        Map<String, String> params = parseQuery(ex.getRequestURI().getQuery());
        String genre     = params.getOrDefault("genre", "");
        String type      = params.getOrDefault("type",  "");
        double minRating = Double.parseDouble(params.getOrDefault("minRating", "0"));

        List<CatalogItem> items;
        if (db != null) {
            items = db.getFiltered(genre, minRating);
        } else {
            items = getDemoData();  // fallback
        }

        // Филтриране по тип (movie / game)
        if (!type.isEmpty()) {
            items.removeIf(i -> !i.getType().equals(type));
        }

        // Строим JSON масив
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) json.append(",");
            json.append(items.get(i).toJson());
        }
        json.append("]");

        sendResponse(ex, 200, json.toString());
    }

    // ---- POST: добавя нов запис ----
    private static void handlePost(HttpExchange ex) throws Exception {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("📥 POST body: " + body);

        // Прост JSON парсинг (без библиотека)
        String type  = extractJson(body, "type");
        String title = extractJson(body, "title");

        if (title == null || title.isEmpty()) {
            sendResponse(ex, 400, "{\"error\":\"Заглавието е задължително\"}");
            return;
        }

        int newId = -1;
        if (db != null && "movie".equals(type)) {
            Movie m = new Movie(0, title,
                parseIntJson(body, "year"), extractJson(body, "genre"),
                parseDoubleJson(body, "rating"), extractJson(body, "desc"),
                extractJson(body, "emoji"), extractJson(body, "director"),
                parseIntJson(body, "duration"));
            newId = db.insertMovie(m);
        } else if (db != null && "game".equals(type)) {
            Game g = new Game(0, title,
                parseIntJson(body, "year"), extractJson(body, "genre"),
                parseDoubleJson(body, "rating"), extractJson(body, "desc"),
                extractJson(body, "emoji"), extractJson(body, "developer"),
                extractJson(body, "platform"));
            newId = db.insertGame(g);
        } else {
            newId = (int)(System.currentTimeMillis() % 100000);
        }

        sendResponse(ex, 201, "{\"id\":" + newId + ",\"message\":\"Записът е добавен успешно\"}");
    }

    // ---- DELETE: изтрива запис ----
    private static void handleDelete(HttpExchange ex) throws Exception {
        Map<String, String> params = parseQuery(ex.getRequestURI().getQuery());
        int    id   = Integer.parseInt(params.getOrDefault("id", "0"));
        String type = params.getOrDefault("type", "movie");

        if (db != null && db.deleteItem(type, id)) {
            sendResponse(ex, 200, "{\"message\":\"Изтрито успешно\"}");
        } else {
            sendResponse(ex, 404, "{\"error\":\"Записът не е намерен\"}");
        }
    }

    // ---- Health check endpoint ----
    private static void handleHealth(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        sendResponse(ex, 200, "{\"status\":\"ok\",\"server\":\"FilmGame Catalog API\",\"port\":" + PORT + "}");
    }

    // ---- Помощни методи ----
    private static void sendResponse(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    // Прост regex-free JSON парсинг за низове
    private static String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? "" : json.substring(start, end);
    }

    private static int parseIntJson(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return 0;
        start += search.length();
        while (start < json.length() && json.charAt(start) == '"') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (Exception e) { return 0; }
    }

    private static double parseDoubleJson(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return 0;
        start += search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try { return Double.parseDouble(json.substring(start, end)); } catch (Exception e) { return 0; }
    }

    // Demo данни (когато няма DB)
    private static List<CatalogItem> getDemoData() {
        List<CatalogItem> list = new ArrayList<>();
        list.add(new Movie(1, "Dune: Part Two",   2024, "Sci-Fi", 8.8, "Епична сага в пустинен свят.",      "🏜️", "Denis Villeneuve",  166));
        list.add(new Movie(2, "Oppenheimer",       2023, "Драма",  8.9, "Историята на атомната бомба.",      "☢️", "Christopher Nolan", 180));
        list.add(new Movie(3, "Interstellar",      2014, "Sci-Fi", 8.7, "Пътуване отвъд галактиката.",       "🌌", "Christopher Nolan", 169));
        list.add(new Game(4,  "Elden Ring",        2022, "RPG",    9.4, "Изследвай Земите Между.",           "⚔️", "FromSoftware",    "PC / PS5"));
        list.add(new Game(5,  "Baldur's Gate 3",   2023, "RPG",    9.6, "D&D RPG с пълна свобода на избор.", "🧙", "Larian Studios",  "PC / PS5"));
        list.add(new Game(6,  "Hades",             2020, "Екшън",  9.5, "Roguelite в митологична Гърция.",   "🔱", "Supergiant",      "PC / Switch"));
        return list;
    }
}
