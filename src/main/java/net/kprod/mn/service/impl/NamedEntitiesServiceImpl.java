package net.kprod.mn.service.impl;

import net.kprod.mn.data.CompletionResponse;
import net.kprod.mn.data.dto.DtoNamedEntity;
import net.kprod.mn.data.entity.EntityNamedEntity;
import net.kprod.mn.data.entity.EntityNamedEntityIndex;
import net.kprod.mn.data.entity.IdNamedEntity;
import net.kprod.mn.data.entity.IdNamedEntityIndex;
import net.kprod.mn.data.repository.RepositoryNamedEntity;
import net.kprod.mn.data.repository.RepositoryNamedEntityIndex;
import net.kprod.mn.service.AuthService;
import net.kprod.mn.service.NamedEntitiesService;
import net.kprod.mn.utils.TranscriptUtils;
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
            //remove namedEntities associated to this page
            repositoryNamedEntity.delete(authService.getUsernameFromContext(), completionResponse.getFileId(), completionResponse.getPageNumber());

            List<DtoNamedEntity> listNE = new ArrayList<>();
            listNE.addAll(TranscriptUtils.identifyNamedIdentities(completionResponse.getTranscript()));
            listNE.addAll(TranscriptUtils.identifyTitles(completionResponse.getTranscript()));

            for (DtoNamedEntity namedEntity : listNE) {
                LOG.info("Pages {} command {}", completionResponse.getPageNumber(), namedEntity);
                namedEntities.add(namedEntity.toEntity(authService.getUsernameFromContext(), completionResponse.getFileId(), completionResponse.getPageNumber()));
                indexNamedEntity(namedEntity);
            }
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
