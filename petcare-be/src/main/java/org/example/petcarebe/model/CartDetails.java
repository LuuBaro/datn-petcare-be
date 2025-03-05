package org.example.petcarebe.model;

import com.restfb.types.whatsapp.platform.send.interactive.Section;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "cart_detail")
public class CartDetails {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long cartDetailId;
    private int quantityItem;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn (name = "product_detail_id")
    private ProductDetails productDetails;

   
}
