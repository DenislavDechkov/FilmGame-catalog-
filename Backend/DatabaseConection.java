package catalog;

import catalog.models.CatalogItem;
import catalog.models.Movie;
import catalog.models.Game;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Клас за управление на MySQL връзката и CRUD операции.
 * Използва JDBC драйвер (mysql-connector-java).
 */
public class DatabaseConnection {

    // ---- Конфигурация – промени спрямо твоя MySQL ----
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/filmgame_catalog?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "тук_постави_паролата_си";

    private Connection conn;

    // ---- Конструктор: отваря връзката ----
    public DatabaseConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");   // зарежда JDBC драйвера
        this.conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        System.out.println("✅ Успешна връзка с базата данни!");
    }

    // ============================================================
    // READ: всички записи от v_catalog
    // ============================================================
    public List<CatalogItem> getAllItems() throws SQLException {
        List<CatalogItem> items = new ArrayList<>();

        String sql = "SELECT * FROM v_catalog ORDER BY rating DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String type = rs.getString("type");

                if ("movie".equals(type)) {
                    items.add(new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("year"),
                        rs.getString("genre"),
                        rs.getDouble("rating"),
                        rs.getString("description"),
                        rs.getString("emoji"),
                        rs.getString("extra_field_1"),   // director
                        parseIntSafe(rs.getString("extra_field_2"))  // duration
                    ));
                } else {
                    items.add(new Game(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("year"),
                        rs.getString("genre"),
                        rs.getDouble("rating"),
                        rs.getString("description"),
                        rs.getString("emoji"),
                        rs.getString("extra_field_1"),   // developer
                        rs.getString("extra_field_2")    // platform
                    ));
                }
            }
        }
        return items;
    }

    // ============================================================
    // READ: филтриране по жанр и минимална оценка
    // ============================================================
    public List<CatalogItem> getFiltered(String genre, double minRating) throws SQLException {
        List<CatalogItem> all      = getAllItems();
        List<CatalogItem> filtered = new ArrayList<>();

        for (CatalogItem item : all) {
            boolean matchGenre  = genre == null || genre.isEmpty() || genre.equals(item.getGenre());
            boolean matchRating = item.getRating() >= minRating;
            if (matchGenre && matchRating) filtered.add(item);
        }
        return filtered;
    }

    // ============================================================
    // CREATE: добавя нов филм
    // ============================================================
    public int insertMovie(Movie m) throws SQLException {
        String sql = "INSERT INTO movies (title, year, genre_id, rating, director, duration_min, description, emoji) " +
                     "VALUES (?, ?, (SELECT id FROM genres WHERE name=?), ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getTitle());
            ps.setInt   (2, m.getYear());
            ps.setString(3, m.getGenre());
            ps.setDouble(4, m.getRating());
            ps.setString(5, m.getDirector());
            ps.setInt   (6, m.getDurationMin());
            ps.setString(7, m.getDescription());
            ps.setString(8, m.getEmoji());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ============================================================
    // CREATE: добавя нова игра
    // ============================================================
    public int insertGame(Game g) throws SQLException {
        String sql = "INSERT INTO games (title, year, genre_id, rating, developer, platform, description, emoji) " +
                     "VALUES (?, ?, (SELECT id FROM genres WHERE name=?), ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getTitle());
            ps.setInt   (2, g.getYear());
            ps.setString(3, g.getGenre());
            ps.setDouble(4, g.getRating());
            ps.setString(5, g.getDeveloper());
            ps.setString(6, g.getPlatform());
            ps.setString(7, g.getDescription());
            ps.setString(8, g.getEmoji());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ============================================================
    // DELETE: изтриване по тип и ID
    // ============================================================
    public boolean deleteItem(String type, int id) throws SQLException {
        String table = "movie".equals(type) ? "movies" : "games";
        String sql   = "DELETE FROM " + table + " WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ============================================================
    // Затваряне на връзката
    // ============================================================
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("🔌 Връзката с базата данни е затворена.");
            }
        } catch (SQLException e) {
            System.err.println("Грешка при затваряне: " + e.getMessage());
        }
    }

    // ---- Помощна функция: безопасен parseInt ----
    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return 0; }
    }
}
