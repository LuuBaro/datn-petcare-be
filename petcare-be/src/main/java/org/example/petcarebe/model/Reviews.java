package org.example.petcarebe.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "reviews")
public class Reviews {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private long reviewsId;
    private Float rating;
    private String comment;
    private Date reviewDate;

    @ManyToOne
    @JoinColumn (name = "userId")
    private User user;

    @ManyToOne
    @JoinColumn(name = "orderDetailsId")
    private OrderDetails orderDetails;

}
