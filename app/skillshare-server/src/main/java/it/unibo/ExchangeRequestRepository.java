package it.unibo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.Serializer;

public class ExchangeRequestRepository {

    private final ConcurrentMap<String, ExchangeRequest> requests;

    @SuppressWarnings("unchecked")
    public ExchangeRequestRepository() {
        DB db = DatabaseCore.getDB();
        requests = (ConcurrentMap<String, ExchangeRequest>) (ConcurrentMap<?, ?>) db
                .hashMap("exchangeRequests", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();
    }

    public boolean save(ExchangeRequest request) {
        ExchangeRequest existing = requests.putIfAbsent(request.getId(), request);
        if (existing != null) {
            return false;
        }
        DatabaseCore.commit();
        return true;
    }

    public ExchangeRequest findById(String id) {
        return requests.get(id);
    }

    public List<ExchangeRequest> listAll() {
        return new ArrayList<>(requests.values());
    }

    public void update(ExchangeRequest request) {
        requests.put(request.getId(), request);
        DatabaseCore.commit();
    }
}