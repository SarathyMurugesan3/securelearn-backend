package com.example.demo.tenant.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.demo.tenant.model.Tenant;
import java.util.Optional;

public interface TenantRepository extends MongoRepository<Tenant, String> {
    Optional<Tenant> findByName(String name);
}
