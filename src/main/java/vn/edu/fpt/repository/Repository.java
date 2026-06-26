package vn.edu.fpt.repository;

import java.util.List;

public interface Repository<T, ID> {
    void save(T entity);
    List<T> findAll();
}