package catalog.models;

/**
 * Базов клас за всеки запис в каталога.
 * Филмите и игрите наследяват от него.
 */
public abstract class CatalogItem {

    private int    id;
    private String type;      // "movie" или "game"
    private String title;
    private int    year;
    private String genre;
    private double rating;
    private String description;
    private String emoji;

    // ---- Конструктор ----
    public CatalogItem(int id, String type, String title, int year,
                       String genre, double rating, String description, String emoji) {
        this.id          = id;
        this.type        = type;
        this.title       = title;
        this.year        = year;
        this.genre       = genre;
        this.rating      = rating;
        this.description = description;
        this.emoji       = emoji;
    }

    // ---- Getters ----
    public int    getId()          { return id; }
    public String getType()        { return type; }
    public String getTitle()       { return title; }
    public int    getYear()        { return year; }
    public String getGenre()       { return genre; }
    public double getRating()      { return rating; }
    public String getDescription() { return description; }
    public String getEmoji()       { return emoji; }

    // ---- Setters ----
    public void setId(int id)                   { this.id = id; }
    public void setRating(double rating)        { this.rating = rating; }
    public void setDescription(String desc)     { this.description = desc; }

    // ---- Абстрактен метод: връща JSON низ ----
    public abstract String toJson();

    @Override
    public String toString() {
        return String.format("[%s] %s (%d) – Оценка: %.1f", type, title, year, rating);
    }
}