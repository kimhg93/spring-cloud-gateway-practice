package com.practice.gateway.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DynamicRouteRepository extends JpaRepository<DynamicRoute, String> {
}
