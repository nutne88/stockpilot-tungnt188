package vn.edu.fpt.repository;

import java.util.List;
import java.util.Optional;

public interface Repository<T, ID> {
    void save(T entity);
    List<T> findAll();
    Optional<T> findById(ID id);
    void update(T entity);
    void deleteById(ID id);
}