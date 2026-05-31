package support;

import com.edwards.orm.annotation.Column;
import com.edwards.orm.annotation.Id;
import com.edwards.orm.annotation.Table;

import java.time.LocalDate;

@Table(name = "books")
public class Book {
    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column
    private int publicationYear;

    @Column
    private boolean available;

    @Column
    private double rating;

    @Column
    private LocalDate releaseDate;

    public Book() {}

    public Book(String title, String author, int publicationYear, boolean available, double rating, LocalDate releaseDate) {
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
        this.available = available;
        this.rating = rating;
        this.releaseDate = releaseDate;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getPublicationYear() { return publicationYear; }
    public boolean isAvailable() { return available; }
    public double getRating() { return rating; }
    public LocalDate getReleaseDate() { return releaseDate; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setAvailable(boolean available) { this.available = available; }
}
