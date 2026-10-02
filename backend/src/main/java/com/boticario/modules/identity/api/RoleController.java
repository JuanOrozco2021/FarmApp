package com.boticario.modules.identity.api;

import com.boticario.modules.identity.api.dto.RoleSummaryDTO;
import com.boticario.modules.identity.infrastructure.RoleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public List<RoleSummaryDTO> findAll() {
        return roleRepository.findAll().stream().map(RoleSummaryDTO::from).toList();
    }
}
