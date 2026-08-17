package com.realtorbackend.demo;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class EntryService {
    private final EntryRepository entryRepository;

    public EntryService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    public List<Entry> findAll() {
        return entryRepository.findAll();
    }

    public Optional<Entry> findById(Long id) {
        return entryRepository.findById(id);
    }

    public Entry create(Entry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry cannot be null");
        }
        if (entry.getAddress() == null || entry.getAddress().isBlank()
                || entry.getCity() == null || entry.getCity().isBlank()
                || entry.getState() == null || entry.getState().isBlank()
                || entry.getZipcode() == null || entry.getZipcode().isBlank()
                || entry.getPrice() == null || entry.getPrice().signum() < 0
                || entry.getDescription() == null || entry.getDescription().isBlank()) {
            throw new IllegalArgumentException("Address, city, state, zipcode, description, and a valid price are required");
        }
        if (!"ACTIVE".equals(entry.getStatus()) && !"CLOSED".equals(entry.getStatus())) {
            throw new IllegalArgumentException("Listing status must be ACTIVE or CLOSED");
        }
        if (entry.getPictureUrls().isEmpty() && entry.getPictureUrl() != null) {
            entry.getPictureUrls().add(entry.getPictureUrl());
        }
        if (!entry.getPictureUrls().isEmpty()) {
            entry.setPictureUrl(entry.getPictureUrls().get(0));
        }
        return entryRepository.save(entry);
    }

    public boolean deleteById(Long id) {
        return entryRepository.deleteById(id);
    }

    public Optional<Entry> update(Long id, Entry changes) {
        if (changes == null) {
            throw new IllegalArgumentException("Listing cannot be null");
        }

        Entry existing = entryRepository.findById(id).orElse(null);
        if (existing == null) {
            return Optional.empty();
        }

        existing.setAddress(changes.getAddress());
        existing.setCity(changes.getCity());
        existing.setState(changes.getState());
        existing.setZipcode(changes.getZipcode());
        existing.setPrice(changes.getPrice());
        existing.setDescription(changes.getDescription());
        existing.setPictureUrl(changes.getPictureUrl());
        existing.setPictureUrls(changes.getPictureUrls());
        existing.setStatus(changes.getStatus());
        validate(existing);
        return Optional.of(entryRepository.save(existing));
    }

    private void validate(Entry entry) {
        if (entry.getAddress() == null || entry.getAddress().isBlank()
                || entry.getCity() == null || entry.getCity().isBlank()
                || entry.getState() == null || entry.getState().isBlank()
                || entry.getZipcode() == null || entry.getZipcode().isBlank()
                || entry.getPrice() == null || entry.getPrice().signum() < 0
                || entry.getDescription() == null || entry.getDescription().isBlank()) {
            throw new IllegalArgumentException("Address, city, state, zipcode, description, and a valid price are required");
        }
        if (!"ACTIVE".equals(entry.getStatus()) && !"CLOSED".equals(entry.getStatus())) {
            throw new IllegalArgumentException("Listing status must be ACTIVE or CLOSED");
        }
    }
}
