package fr.monkeynotes.mn.controller;

import fr.monkeynotes.mn.data.dto.DtoQuickNote;
import fr.monkeynotes.mn.service.QuickNoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Every endpoint resolves the owner from the security context. The uuid is never trusted as an
 * identity on its own — unlike ImageController, a caller cannot name someone else's note.
 */
@RestController
public class QuickNoteController {
    private Logger LOG = LoggerFactory.getLogger(QuickNoteController.class);

    @Autowired
    private QuickNoteService quickNoteService;

    @PostMapping("/quicknote")
    public ResponseEntity<DtoQuickNote> create(@RequestBody DtoQuickNote dtoQuickNote) {
        if (isBlankBody(dtoQuickNote)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(quickNoteService.save(dtoQuickNote));
    }

    @PutMapping("/quicknote/{uuid}")
    public ResponseEntity<DtoQuickNote> update(@PathVariable UUID uuid,
                                              @RequestBody DtoQuickNote dtoQuickNote) {
        if (isBlankBody(dtoQuickNote)) {
            return ResponseEntity.badRequest().build();
        }
        //the path wins over the payload, so a mismatched body can't write to another note
        return ResponseEntity.ok(quickNoteService.save(dtoQuickNote.setUuid(uuid)));
    }

    @DeleteMapping("/quicknote/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        if (quickNoteService.delete(uuid) == false) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/quicknote/list")
    public ResponseEntity<List<DtoQuickNote>> list() {
        return ResponseEntity.ok(quickNoteService.list());
    }

    @GetMapping("/quicknote/{uuid}")
    public ResponseEntity<DtoQuickNote> get(@PathVariable UUID uuid) {
        return quickNoteService.get(uuid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isBlankBody(DtoQuickNote dtoQuickNote) {
        if (dtoQuickNote == null || dtoQuickNote.getBody() == null || dtoQuickNote.getBody().isBlank()) {
            LOG.warn("refusing quicknote with an empty body");
            return true;
        }
        return false;
    }
}
