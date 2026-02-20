package com.mysawit.mysawit_pembayaran;
import jakarta.persistence.*;

@Entity
@Table(name = "db_ping")
public class DbPing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    public DbPing() {}

    public DbPing(String message) { this.message = message; }

    public Long getId() { return id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}