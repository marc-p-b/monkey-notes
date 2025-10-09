package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.data.ViewOptions;
import fr.monkeynotes.mn.data.dto.DtoNamedEntity;
import fr.monkeynotes.mn.data.dto.DtoNamedEntityIndex;
import fr.monkeynotes.mn.data.enums.NamedEntityVerb;
import fr.monkeynotes.mn.data.repository.RepositoryNamedEntity;
import fr.monkeynotes.mn.data.repository.RepositoryNamedEntityIndex;
import fr.monkeynotes.mn.service.AuthService;
import fr.monkeynotes.mn.service.ViewService;
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
    private AuthService authService;

    @Autowired
    private ViewService viewService;

    @Autowired
    private RepositoryNamedEntityIndex repositoryNamedEntityIndex;

    @Autowired
    private RepositoryNamedEntity repositoryNamedEntity;

    @GetMapping("/ne/verb/{verb}")
    public ResponseEntity<List<DtoNamedEntityIndex>> getEntities(@PathVariable NamedEntityVerb verb) {
        List<DtoNamedEntityIndex> l = repositoryNamedEntityIndex.findByVerb(authService.getUsernameFromContext(), verb).stream()
                .map(DtoNamedEntityIndex::fromEntity)
                .map(d -> {
                    long count = repositoryNamedEntity.countByValue(d.getValue());
                    d.setCount(count);
                    return d;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok().body(l);
    }

    @GetMapping("/ne/values/{verb}/{value}")
    public ResponseEntity<List<DtoNamedEntity>> getValues(@PathVariable NamedEntityVerb verb, @PathVariable String value) {
        List<DtoNamedEntity> l = repositoryNamedEntity.findByVerbAndValue(authService.getUsernameFromContext(), verb, value)
                .stream()
                .map(DtoNamedEntity::fromEntity)
                .map(ne -> {
                    //TODO check for exc
                    String fileName = viewService.getTranscript(ne.getFileId(), ViewOptions.all()).getName();
                    ne.setFileName(fileName);
                    return ne;
                })
                .toList();


        return ResponseEntity.ok().body(l);
    }
}