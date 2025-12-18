package com.example.silkmall.controller;

import com.example.silkmall.dto.SupplierCreateRequest;
import com.example.silkmall.dto.SupplierDTO;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController extends BaseController {
    private final SupplierService supplierService;
    
    @Autowired
    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SupplierDTO>> listSuppliers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "supplierLevel", required = false) String supplierLevel,
            @RequestParam(value = "status", required = false) String status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String sanitizedKeyword = keyword != null ? keyword.trim() : null;
        if (sanitizedKeyword != null && sanitizedKeyword.isEmpty()) {
            sanitizedKeyword = null;
        }
        Page<SupplierDTO> result = supplierService
                .search(sanitizedKeyword, enabled, normalize(supplierLevel, null), normalize(status, null), pageable)
                .map(this::toDto);
        return success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPPLIER') and #id == principal.id)")
    public ResponseEntity<?> getSupplierById(@PathVariable Long id) {
        Optional<Supplier> supplier = supplierService.findById(id);
        if (supplier.isPresent()) {
            return success(toDto(supplier.get()));
        } else {
            return notFound("供应商不存在");
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSupplier(@Valid @RequestBody SupplierCreateRequest request) {
        Supplier supplier = new Supplier();
        supplier.setUsername(normalize(request.getUsername(), null));
        supplier.setPassword(request.getPassword());
        supplier.setEmail(normalize(request.getEmail(), null));
        supplier.setPhone(normalize(request.getPhone(), null));
        supplier.setAddress(normalize(request.getAddress(), null));
        supplier.setCompanyName(normalize(request.getCompanyName(), null));
        supplier.setBusinessLicense(normalize(request.getBusinessLicense(), null));
        supplier.setContactPerson(normalize(request.getContactPerson(), null));
        supplier.setSupplierLevel(normalize(request.getSupplierLevel(), null));
        supplier.setStatus(normalize(request.getStatus(), null));
        supplier.setRole("supplier");
        supplier.setEnabled(Boolean.TRUE.equals(request.getEnabled()));

        Supplier saved = supplierService.register(supplier);

        if (Boolean.FALSE.equals(request.getEnabled())) {
            supplierService.disable(saved.getId());
            saved = supplierService.findById(saved.getId()).orElse(saved);
        }

        return created(toDto(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPPLIER') and #id == principal.id)")
    public ResponseEntity<?> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierDTO request) {
        Supplier existing = supplierService.findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));

        applyUpdates(request, existing);

        Supplier saved = supplierService.save(existing);
        return success(toDto(saved));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteById(id);
        return success();
    }
    
    @PostMapping("/register")
    public ResponseEntity<Supplier> register(@RequestBody Supplier supplier) {
        return created(supplierService.register(supplier));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Supplier>> getSuppliersByStatus(@PathVariable String status) {
        return success(supplierService.findByStatus(status));
    }
    
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Supplier>> getSuppliersByLevel(@PathVariable String level, Pageable pageable) {
        return success(supplierService.findBySupplierLevel(level));
    }
    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveSupplier(@PathVariable Long id) {
        supplierService.approveSupplier(id);
        return success();
    }
    
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectSupplier(@PathVariable Long id, @RequestParam String reason) {
        supplierService.rejectSupplier(id, reason);
        return success();
    }
    
    @PutMapping("/{id}/level")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateSupplierLevel(@PathVariable Long id, @RequestParam String level) {
        supplierService.updateSupplierLevel(id, level);
        return success();
    }

    private void applyUpdates(SupplierDTO source, Supplier target) {
        target.setUsername(normalize(source.getUsername(), target.getUsername()));
        target.setEmail(normalize(source.getEmail(), target.getEmail()));
        target.setPhone(normalize(source.getPhone(), target.getPhone()));
        target.setAddress(normalize(source.getAddress(), target.getAddress()));

        target.setCompanyName(normalize(source.getCompanyName(), target.getCompanyName()));
        target.setBusinessLicense(normalize(source.getBusinessLicense(), target.getBusinessLicense()));
        target.setContactPerson(normalize(source.getContactPerson(), target.getContactPerson()));
        target.setSupplierLevel(normalize(source.getSupplierLevel(), target.getSupplierLevel()));
        target.setStatus(normalize(source.getStatus(), target.getStatus()));
        if (source.getEnabled() != null) {
            target.setEnabled(source.getEnabled());
        }
    }

    private SupplierDTO toDto(Supplier supplier) {
        SupplierDTO dto = new SupplierDTO();
        dto.setId(supplier.getId());
        dto.setUsername(supplier.getUsername());
        dto.setEmail(supplier.getEmail());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setCompanyName(supplier.getCompanyName());
        dto.setBusinessLicense(supplier.getBusinessLicense());
        dto.setContactPerson(supplier.getContactPerson());
        dto.setSupplierLevel(supplier.getSupplierLevel());
        dto.setStatus(supplier.getStatus());
        dto.setEnabled(supplier.isEnabled());
        return dto;
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
