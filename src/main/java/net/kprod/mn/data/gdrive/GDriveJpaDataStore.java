package net.kprod.mn.data.gdrive;

import com.google.api.client.util.store.AbstractDataStore;
import com.google.api.client.util.store.DataStore;
import net.kprod.mn.data.entity.EntityGDriveCredential;
import net.kprod.mn.data.repository.RepositoryGDriveCredential;

import java.io.*;
import java.util.*;

public class GDriveJpaDataStore<V extends Serializable> extends AbstractDataStore<V> {

    private final RepositoryGDriveCredential repository;

    protected GDriveJpaDataStore(GDriveJpaDataStoreFactory dataStoreFactory, RepositoryGDriveCredential repository, String id) {
        super(dataStoreFactory, id);
        this.repository = repository;
    }

    @Override
    public Set<String> keySet() {
        return new HashSet<>(repository.findAll().stream().map(EntityGDriveCredential::getId).toList());
    }

    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (EntityGDriveCredential entity : repository.findAll()) {
            values.add(deserialize(entity.getCredentialData()));
        }
        return values;
    }

    @Override
    public V get(String key) {
        return repository.findById(key)
                .map(entity -> deserialize(entity.getCredentialData()))
                .orElse(null);
    }

    @Override
    public DataStore<V> set(String key, V value) {
        EntityGDriveCredential entity = new EntityGDriveCredential();
        entity.setId(key);
        entity.setCredentialData(serialize(value));
        repository.save(entity);
        return this;
    }

    @Override
    public DataStore<V> delete(String key) {
        repository.deleteById(key);
        return this;
    }

    @Override
    public DataStore<V> clear() {
        repository.deleteAll();
        return this;
    }

    private byte[] serialize(V value) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(value);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize credential", e);
        }
    }

    private V deserialize(byte[] data) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (V) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to deserialize credential", e);
        }
    }
}
