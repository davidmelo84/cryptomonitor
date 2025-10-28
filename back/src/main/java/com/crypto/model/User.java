// back/src/main/java/com/crypto/model/User.java
package com.crypto.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    private String role = "USER";

    // ✅ NOVO: Campo para verificação
    @Column(nullable = false)
    private Boolean enabled = false; // Inicia como false
}