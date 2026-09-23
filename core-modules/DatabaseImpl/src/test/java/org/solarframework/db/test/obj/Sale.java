package org.solarframework.db.test.obj;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.solarframework.db.spring.DatabaseObject;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Spelled the way an imported dataset is - lowercase snake_case table and columns - to pin what reaches the database and a CSV export. */
@Entity
@Table(name = "csv_sale")
public class Sale extends DatabaseObject.ID_RECORD_OBJ<Long, Sale> {
    @Column(name = "sale_code")
    private String code;
    @Column(name = "price")
    private BigDecimal price;
    @Column(name = "weight")
    private Double weight;
    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    protected Sale() {}
    public Sale(Long id, String code, BigDecimal price, Double weight, LocalDateTime purchasedAt) {
        this.ID = id;
        this.code = code;
        this.price = price;
        this.weight = weight;
        this.purchasedAt = purchasedAt;
    }
}
