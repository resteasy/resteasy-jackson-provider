/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.mapper.resources;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import jakarta.ws.rs.NotFoundException;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class GenericRepository<T extends IdEntry> {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicLong idGenerator = new AtomicLong();
    private final Set<T> repository;

    protected GenericRepository() {
        repository = new TreeSet<>();
    }

    Set<T> get() {
        lock.readLock().lock();
        try {
            return Set.copyOf(repository);
        } finally {
            lock.readLock().unlock();
        }
    }

    void add(final T entry) {
        lock.writeLock().lock();
        try {
            beforeAdd(idGenerator.incrementAndGet(), entry);
            repository.add(entry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void update(final T entry) {
        lock.writeLock().lock();
        try {
            if (!repository.contains(entry)) {
                throw new NotFoundException("Not found: " + entry);
            }
            repository.remove(entry);
            beforeUpdate(entry);
            repository.add(entry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    T findById(final long id) {
        lock.readLock().lock();
        try {
            return repository.stream().filter(findByIdFilter(id)).findFirst().orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    protected abstract Predicate<T> findByIdFilter(long id);

    protected abstract void beforeAdd(long id, T entry);

    protected abstract void beforeUpdate(T entry);
}
