package com.realtorbackend.demo;

import java.util.List;
import java.util.Optional;

public interface EntryRepository {
    List<Entry> findAll();
    Optional<Entry> findById(Long id);
    Entry save(Entry entry);
    boolean deleteById(Long id);
}
