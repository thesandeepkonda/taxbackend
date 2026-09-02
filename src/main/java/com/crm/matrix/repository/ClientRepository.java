package com.crm.matrix.repository;

import com.crm.matrix.entity.Client;
import com.crm.matrix.enums.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface ClientRepository
        extends JpaRepository<Client, Long> {

    Optional<Client> findByPhone(String phone);

    Optional<Client> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    Page<Client> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );

    Page<Client> findByStatus(
            ClientStatus status,
            Pageable pageable
    );

     //Optional<Client> findByUserId(Long id);
}