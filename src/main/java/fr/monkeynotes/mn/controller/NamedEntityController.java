package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.data.dto.DtoNamedEntity;
import fr.monkeynotes.mn.data.dto.DtoNamedEntityIndex;
import fr.monkeynotes.mn.data.dto.DtoQuickNote;
import fr.monkeynotes.mn.data.entity.EntityFile;
import fr.monkeynotes.mn.data.entity.IdFile;
import fr.monkeynotes.mn.data.enums.NamedEntityVerb;
import fr.monkeynotes.mn.data.repository.RepositoryFile;
import fr.monkeynotes.mn.data.repository.RepositoryNamedEntity;
import fr.monkeynotes.mn.data.repository.RepositoryNamedEntityIndex;
import fr.monkeynotes.mn.service.AuthService;
import fr.monkeynotes.mn.service.QuickNoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class NamedEntityController {
    private Logger LOG = LoggerFactory.getLogger(NamedEntityController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private RepositoryNamedEntityIndex repositoryNamedEntityIndex;

    @Autowired
    private RepositoryNamedEntity repositoryNamedEntity;

    @Autowired
    private RepositoryFile repositoryFile;

    @Autowired
    private QuickNoteService quickNoteService;

    //TODO put in service
    @GetMapping("/ne/verbs")
    public ResponseEntity<Map<NamedEntityVerb, Map<String, List<DtoNamedEntity>>>> getVerbs() {

        Map<NamedEntityVerb, Map<String, List<DtoNamedEntity>>> map = new HashMap<>();
        for(NamedEntityVerb verb : NamedEntityVerb.values()) {
            //todo only indexable verbs should be indexed ! (check filter before insertion)
            //diagram and diagramNextPage are treated in the same list
            if(verb.isIndexable() && !verb.equals(NamedEntityVerb.diagramNextPage)) {
                List<DtoNamedEntity> listNe = new ArrayList<>();
                listNe.addAll(repositoryNamedEntity.findByVerb(authService.getUsernameFromContext(), verb).stream()
                    .map(ne -> DtoNamedEntity.fromEntity(ne))
                    .toList());
                if(verb.equals(NamedEntityVerb.diagram)) {
                    listNe.addAll(repositoryNamedEntity.findByVerb(authService.getUsernameFromContext(), NamedEntityVerb.diagramNextPage).stream()
                            .map(ne -> DtoNamedEntity.fromEntity(ne))
                            .map(ne -> ne.setVerb(NamedEntityVerb.diagram))
                            .toList());
                }
                Map<String, List<DtoNamedEntity>> map2 = listNe.stream()
                        .map(ne -> ne.setFileName(resolveOwnerLabel(ne)))
                        .collect(Collectors.groupingBy(DtoNamedEntity::getValue));
                map.put(verb, map2);
            }
        }

        return ResponseEntity.ok(map);
    }

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
                .map(ne -> ne.setFileName(resolveOwnerLabel(ne)))
                .toList();


        return ResponseEntity.ok().body(l);
    }

    /**
     * A named_entity row belongs either to a transcript page or to a quicknote (marked by
     * pageNumber == QUICKNOTE_PAGE_NUMBER, since a quicknote has no pages).
     *
     * Both display endpoints used to resolve this themselves and neither handled the other kind:
     * getValues() went through viewService.getTranscript(), which returns null for anything that
     * isn't a transcript and would NPE on a quicknote id, while getVerbs() looked the id up in
     * repositoryFile and would just label it "unknown". One resolver now covers both, and the
     * transcript path no longer builds a whole transcript DTO just to read a name.
     */
    private String resolveOwnerLabel(DtoNamedEntity ne) {
        if (ne.getPageNumber() == QuickNoteService.QUICKNOTE_PAGE_NUMBER) {
            return parseUuid(ne.getFileId())
                    .flatMap(uuid -> quickNoteService.get(uuid))
                    .map(DtoQuickNote::getTitle)
                    .filter(title -> !title.isBlank())
                    .orElse("Quick note");
        }

        IdFile idFile = IdFile.createIdFile(authService.getUsernameFromContext(), ne.getFileId());
        return repositoryFile.findById(idFile)
                .map(EntityFile::getName)
                .orElse("unknown");
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            LOG.warn("named entity flagged as a quicknote but its id is not a uuid: {}", value);
            return Optional.empty();
        }
    }
}
