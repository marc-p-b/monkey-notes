package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.data.dto.DtoSearchResult;
import fr.monkeynotes.mn.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Controller
public class SearchController {
    private Logger LOG = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SearchService searchService;

    @PostMapping("/search")
    public ResponseEntity<List<DtoSearchResult>> search(@RequestBody String body) {
        return ResponseEntity.ok().body(searchService.search(body));
    }
}