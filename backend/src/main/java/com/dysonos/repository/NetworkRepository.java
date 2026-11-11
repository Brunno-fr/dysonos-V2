package com.dysonos.repository;

import com.dysonos.model.Network;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NetworkRepository extends JpaRepository<Network, UUID> {
}
