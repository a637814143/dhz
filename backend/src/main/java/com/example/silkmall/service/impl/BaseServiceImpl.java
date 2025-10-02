package com.example.silkmall.service.impl;

import com.example.silkmall.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public abstract class BaseServiceImpl<T, ID> implements BaseService<T, ID> {
    protected final JpaRepository<T, ID> repository;
    
    protected BaseServiceImpl(JpaRepository<T, ID> repository) {
        this.repository = repository;
    }
    
    @Override
    public T save(T entity) {
        return repository.save(entity);
    }
    
    @Override
    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }
    
    @Override
    public List<T> findAll() {
        return repository.findAll();
    }
    
    @Override
    public void deleteById(ID id) {
        repository.deleteById(id);
    }
    
    @Override
    public boolean existsById(ID id) {
        return repository.existsById(id);
    }
}