package net.kprod.mn.controller;

import net.kprod.mn.data.dto.DtoNamedEntity;
import net.kprod.mn.data.entity.EntityNamedEntity;
import net.kprod.mn.data.repository.RepositoryNamedEntity;
import net.kprod.mn.service.AuthService;
import net.kprod.mn.data.enums.NamedEntityVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class NamedEntityController {
    private Logger LOG = LoggerFactory.getLogger(NamedEntityController.class);

    @Autowired
    private RepositoryNamedEntity repositoryNamedEntity;

    @Autowired
    private AuthService authService;

    @GetMapping("/ne/verb/{verb}")
    public ResponseEntity<List<DtoNamedEntity>> getEntities(@PathVariable NamedEntityVerb verb) {

        List<EntityNamedEntity> list = repositoryNamedEntity.findByVerb(authService.getUsernameFromContext(), verb);

        List<DtoNamedEntity> dtoList = list.stream()
                .map(DtoNamedEntity::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok().body(dtoList);
    }


}