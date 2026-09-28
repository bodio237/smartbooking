package com.smartbooking.api.controller;

import com.smartbooking.api.dto.ResourceRequest;
import com.smartbooking.api.dto.ResourceResponse;
import com.smartbooking.domain.model.Resource;
import com.smartbooking.domain.repository.ResourceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceRepository resourceRepository;

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getActiveResources() {

        List<ResourceResponse> resources =
                resourceRepository.findByIsActiveTrue()
                        .stream()
                        .map(resource -> new ResourceResponse(
                                resource.getId(),
                                resource.getName(),
                                resource.getType(),
                                resource.getDescription(),
                                resource.getCapacity(),
                                resource.getIsActive()
                        ))
                        .toList();

        return ResponseEntity.ok(resources);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> createResource(
            @Valid @RequestBody ResourceRequest request
    ) {

        Resource resource = Resource.builder()
                .name(request.name())
                .type(request.type())
                .description(request.description())
                .capacity(request.capacity())
                .isActive(true)
                .build();

        Resource savedResource = resourceRepository.save(resource);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedResource);
    }
}