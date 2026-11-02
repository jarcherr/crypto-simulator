package com.simulador.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    private String email;
    private String password;
    private double saldoFicticio = 10000.0; // Saldo inicial de $10,000
    private boolean activo = true; // Para suspensión de cuentas
    private boolean admin = false;

    @OneToMany(mappedBy = "usuario")
    private List<Transaccion> transacciones;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public double getSaldoFicticio() { return saldoFicticio; }
    public void setSaldoFicticio(double saldoFicticio) { this.saldoFicticio = saldoFicticio; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
}