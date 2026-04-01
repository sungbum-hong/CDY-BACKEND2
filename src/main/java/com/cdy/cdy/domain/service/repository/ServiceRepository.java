package com.cdy.cdy.domain.service.repository;

import com.cdy.cdy.domain.service.entity.Service;
import com.cdy.cdy.domain.service.entity.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByStatusOrderByCreatedAtDesc(ServiceStatus status);
}
