package com.jcode.authidentityservice.service;

import com.jcode.authidentityservice.dto.TenantSummary;

import java.util.List;
import java.util.UUID;

public interface TenantService {

    TenantSummary getById(UUID id);

    TenantSummary getByCode(String code);

    List<TenantSummary> getAllForUser(UUID userId);
}
