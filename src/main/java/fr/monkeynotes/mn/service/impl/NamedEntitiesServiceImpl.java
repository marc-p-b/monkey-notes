package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.data.CompletionResponse;
import fr.monkeynotes.mn.data.dto.DtoNamedEntity;
import fr.monkeynotes.mn.data.entity.EntityNamedEntity;
import fr.monkeynotes.mn.data.entity.EntityNamedEntityIndex;
import fr.monkeynotes.mn.data.entity.IdNamedEntityIndex;
import fr.monkeynotes.mn.data.repository.RepositoryNamedEntity;
import fr.monkeynotes.mn.data.repository.RepositoryNamedEntityIndex;
import fr.monkeynotes.mn.service.AuthService;
import fr.monkeynotes.mn.service.NamedEntitiesService;
import fr.monkeynotes.mn.utils.TranscriptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NamedEntitiesServiceImpl implements NamedEntitiesService {
    private Logger LOG = LoggerFactory.getLogger(NamedEntitiesServiceImpl.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private RepositoryNamedEntity repositoryNamedEntity;

    @Autowired
    private RepositoryNamedEntityIndex repositoryNamedEntityIndex;

    @Override
    public void identifyNamedEntities(String fileId, List<CompletionResponse> listCompletionResponse) {

        List<EntityNamedEntity> namedEntities = new ArrayList<>();
        for (CompletionResponse completionResponse : listCompletionResponse) {
//            //remove namedEntities associated to this page
//            repositoryNamedEntity.delete(authService.getUsernameFromContext(), completionResponse.getFileId(), completionResponse.getPageNumber());
//
//            List<DtoNamedEntity> listNE = new ArrayList<>();
//            listNE.addAll(TranscriptUtils.identifyNamedIdentities(completionResponse.getTranscript()));
//            listNE.addAll(TranscriptUtils.identifyTitles(completionResponse.getTranscript()));
//
//            for (DtoNamedEntity namedEntity : listNE) {
//                LOG.info("Pages {} command {}", completionResponse.getPageNumber(), namedEntity);
//                namedEntities.add(namedEntity.toEntity(authService.getUsernameFromContext(), completionResponse.getFileId(), completionResponse.getPageNumber()));
//                indexNamedEntity(namedEntity);
//            }
            identifyNamedEntities2(completionResponse.getFileId(), completionResponse.getPageNumber(), completionResponse.getTranscript());
        }
        repositoryNamedEntity.saveAll(namedEntities);
    }

    @Override
    public void identifyNamedEntities2(String fileId, int pageNumber, String content) {
        List<EntityNamedEntity> namedEntities = new ArrayList<>();
        //remove namedEntities associated to this page
        repositoryNamedEntity.delete(authService.getUsernameFromContext(), fileId, pageNumber);

        List<DtoNamedEntity> listNE = new ArrayList<>();
        listNE.addAll(TranscriptUtils.identifyNamedIdentities(content));
        listNE.addAll(TranscriptUtils.identifyTitles(content));

        for (DtoNamedEntity namedEntity : listNE) {
            LOG.info("Pages {} namedentity {}", pageNumber, namedEntity);
            namedEntities.add(namedEntity.toEntity(authService.getUsernameFromContext(), fileId, pageNumber));
            indexNamedEntity(namedEntity);
        }

        repositoryNamedEntity.saveAll(namedEntities);
    }

    private void indexNamedEntity(DtoNamedEntity dtoNamedEntity) {
        if(dtoNamedEntity.getVerb().isIndexable() == false) {
            return;
        }
        IdNamedEntityIndex idNamedEntityIndex = new IdNamedEntityIndex()
                .setUsername(authService.getUsernameFromContext())
                .setVerb(dtoNamedEntity.getVerb())
                .setValue(dtoNamedEntity.getValue());

        repositoryNamedEntityIndex.findById(idNamedEntityIndex)
                .orElse(repositoryNamedEntityIndex.save(new EntityNamedEntityIndex()
                        .setIdNamedEntityIndex(idNamedEntityIndex).setCreatedAt(OffsetDateTime.now())));
    }
}
