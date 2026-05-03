package com.example.oligarchrating.repository;

import com.example.oligarchrating.domain.Oligarch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OligarchRepository extends JpaRepository<Oligarch, Long> {
}
