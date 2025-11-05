package fr.monkeynotes.mn.service.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.*;
import fr.monkeynotes.mn.data.dto.DtoTranscriptPage;
import fr.monkeynotes.mn.data.entity.EntityTranscriptPage;
import fr.monkeynotes.mn.data.entity.EntityTranscriptPageDiff;
import fr.monkeynotes.mn.data.entity.IdTranscriptPage;
import fr.monkeynotes.mn.data.entity.IdTranscriptPageDiff;
import fr.monkeynotes.mn.data.repository.RepositoryTranscriptPage;
import fr.monkeynotes.mn.data.repository.RepositoryTranscriptPageDiff;
import fr.monkeynotes.mn.service.EditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EditServiceImpl implements EditService {

    private Logger LOG = LoggerFactory.getLogger(EditServiceImpl.class);

    @Autowired
    private RepositoryTranscriptPage repositoryTranscriptPage;

    @Autowired
    private RepositoryTranscriptPageDiff repositoryTranscriptPageDiff;

    public record DeltaDTO(String type, int sourcePos, List<String> sourceLines, int targetPos, List<String> targetLines) {}

    public static String toJson(Patch<String> patch) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<DeltaDTO> list = patch.getDeltas().stream().map(d -> new DeltaDTO(
                d.getType().name(),
                d.getSource().getPosition(),
                d.getSource().getLines(),
                d.getTarget().getPosition(),
                d.getTarget().getLines()
        )).toList();

        return mapper.writeValueAsString(list);
    }

    public static Patch<String> fromJson(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<DeltaDTO> deltas = List.of(mapper.readValue(json, DeltaDTO[].class));
        Patch<String> patch = new Patch<>();

        for (DeltaDTO dto : deltas) {
            switch (dto.type()) {
                case "CHANGE" -> patch.addDelta(new ChangeDelta<>(
                        new Chunk<>(dto.sourcePos(), dto.sourceLines()),
                        new Chunk<>(dto.targetPos(), dto.targetLines())
                ));
                case "INSERT" -> patch.addDelta(new InsertDelta<>(
                        new Chunk<>(dto.sourcePos(), dto.sourceLines()),
                        new Chunk<>(dto.targetPos(), dto.targetLines())
                ));
                case "DELETE" -> patch.addDelta(new DeleteDelta<>(
                        new Chunk<>(dto.sourcePos(), dto.sourceLines()),
                        new Chunk<>(dto.targetPos(), dto.targetLines())
                ));
            }
        }
        return patch;
    }

    public DtoTranscriptPage applyPatch(DtoTranscriptPage page) {
        ObjectMapper mapper = new ObjectMapper();

        IdTranscriptPageDiff idTranscriptPageDiff = IdTranscriptPageDiff.createIdTranscriptPageDiff(
                page.getUsername(),
                page.getFileId(),
                page.getPageNumber(),
                page.getVersion());

        Optional<EntityTranscriptPageDiff> opt = repositoryTranscriptPageDiff.findById(idTranscriptPageDiff);
        if(opt.isPresent()) {

            EntityTranscriptPageDiff etpd = opt.get();

            try {

                System.out.println(etpd.getDiff());

                Patch<String> patch = fromJson(etpd.getDiff());

                List<String> lines = Arrays.stream(page.getTranscript().split("\n")).toList();
                lines = patch.applyTo(lines);

                page.setTranscript(lines.stream().collect(Collectors.joining("\n")));


            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (PatchFailedException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }
        return page;
    }

    @Override
    public void edit(String fileId, int pageNumber, String content) {
        IdTranscriptPage idTranscriptPage = IdTranscriptPage.createIdTranscriptPage("marc", "17zAMnepi8cVfRmsdi35IkIMFGKzeppup", pageNumber);

        Optional<EntityTranscriptPage> oetp = repositoryTranscriptPage.findById(idTranscriptPage);
        if(oetp.isPresent()) {

            EntityTranscriptPage etp = oetp.get();

            String original = etp.getTranscript();
            String target = content;


            Patch<String> patch = DiffUtils.diff(Arrays.stream(original.split("\n")).toList(), Arrays.stream(target.split("\n")).toList());


            //List<AbstractDelta<String>> deltas = patch.getDeltas();
            try {
                String json = toJson(patch);

                System.out.println(json);

                IdTranscriptPageDiff id = IdTranscriptPageDiff.fromIdTranscriptPage(idTranscriptPage, etp.getVersion());

                EntityTranscriptPageDiff etpd = new EntityTranscriptPageDiff()
                        .setIdTranscriptPageDiff(id)
                        .setCreatedAt(OffsetDateTime.now())
                        .setDiff(json);

                repositoryTranscriptPageDiff.save(etpd);


            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        }
    }
}
