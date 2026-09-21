package com.crm.matrix.repository;

import com.crm.matrix.entity.Client;
import com.crm.matrix.enums.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
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
    List<Client> findByStatusOrderByCreatedAtDesc(
            ClientStatus status
    );


    //Optional<Client> findByUserId(Long id);
    @Query("SELECT c FROM Client c WHERE NOT EXISTS (SELECT a FROM ClientAssignment a WHERE a.client = c AND a.active = true)")
    Page<Client> findUnassignedClients(Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.currentStage = :stage")
    Page<Client> findAllClientsByStage(@Param("stage") String stage, Pageable pageable);}
