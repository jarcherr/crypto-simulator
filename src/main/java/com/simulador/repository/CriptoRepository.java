package com.simulador.repository;

import com.simulador.model.Criptomoneda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CriptoRepository extends JpaRepository<Criptomoneda, Long> {
    Criptomoneda findBySimbolo(String simbolo);
}