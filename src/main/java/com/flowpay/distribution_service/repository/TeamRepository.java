package com.flowpay.distribution_service.repository;

import com.flowpay.distribution_service.entity.Team;
import com.flowpay.distribution_service.enums.TeamName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(TeamName name);
}
