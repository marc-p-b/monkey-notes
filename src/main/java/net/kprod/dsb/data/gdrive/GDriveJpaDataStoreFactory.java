package net.kprod.dsb.data.gdrive;

import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import net.kprod.dsb.data.repository.RepositoryGDriveCredential;

import java.io.Serializable;

public class GDriveJpaDataStoreFactory implements DataStoreFactory {

    private final RepositoryGDriveCredential repository;

    public GDriveJpaDataStoreFactory(RepositoryGDriveCredential repository) {
        this.repository = repository;
    }

    @Override
    public <V extends Serializable> DataStore<V> getDataStore(String id) {
        return new GDriveJpaDataStore<>(this, repository, id);
    }
}