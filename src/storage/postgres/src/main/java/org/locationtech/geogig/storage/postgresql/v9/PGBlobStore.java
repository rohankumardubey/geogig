/* Copyright (c) 2015-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.storage.postgresql.v9;

import static java.lang.String.format;
import static org.locationtech.geogig.storage.postgresql.config.PGStorage.log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import org.locationtech.geogig.storage.postgresql.config.Environment;
import org.locationtech.geogig.transaction.TransactionBlobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

class PGBlobStore implements TransactionBlobStore {

    private static final Logger LOG = LoggerFactory.getLogger(PGBlobStore.class);

    private static final String NO_TRANSACTION = "";

    private String blobsTable;

    private int repositoryId;

    private Environment env;

    PGBlobStore(final Environment env) {
        this.env = env;
        this.blobsTable = env.getTables().blobs();
        this.repositoryId = env.getRepositoryId();
    }

    public @Override Optional<byte[]> getBlob(String path) {
        return getBlob(NO_TRANSACTION, path);
    }

    public @Override Optional<InputStream> getBlobAsStream(String path) {
        return getBlobAsStream(NO_TRANSACTION, path);
    }

    public @Override void putBlob(String path, byte[] blob) {
        putBlob(NO_TRANSACTION, path, blob);
    }

    public @Override void putBlob(String path, InputStream blob) {
        putBlob(NO_TRANSACTION, path, blob);
    }

    public @Override void removeBlob(String path) {
        removeBlob(NO_TRANSACTION, path);
    }

    public @Override Optional<byte[]> getBlob(final String namespace, final String path) {
        Objects.requireNonNull(namespace, "namespace can't be null");

        final String sql = format(
                "SELECT blob FROM %s WHERE repository = ? AND namespace = ? AND path = ?",
                blobsTable);

        byte[] bytes = null;
        try (Connection cx = env.getConnection()) {
            try (PreparedStatement ps = cx
                    .prepareStatement(log(sql, LOG, repositoryId, namespace, path))) {
                ps.setInt(1, repositoryId);
                ps.setString(2, namespace);
                ps.setString(3, path);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        bytes = rs.getBytes(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.ofNullable(bytes);
    }

    public @Override Optional<InputStream> getBlobAsStream(final String namespace,
            final String path) {
        Objects.requireNonNull(namespace, "namespace can't be null");
        Optional<byte[]> blob = getBlob(namespace, path);
        InputStream in = null;
        if (blob.isPresent()) {
            in = new ByteArrayInputStream(blob.get());
        }
        return Optional.ofNullable(in);
    }

    public @Override void putBlob(final String namespace, final String path, final byte[] blob) {
        Objects.requireNonNull(namespace, "namespace can't be null");

        String delete = format("DELETE FROM %s WHERE repository = ? AND namespace = ? AND path = ?",
                blobsTable);
        String insert = format(
                "INSERT INTO %s (repository, namespace, path, blob) VALUES (?, ?, ?, ?)",
                blobsTable);
        try (Connection cx = env.getConnection()) {
            cx.setAutoCommit(false);
            try {
                try (PreparedStatement d = cx.prepareStatement(delete)) {
                    d.setInt(1, repositoryId);
                    d.setString(2, namespace);
                    d.setString(3, path);
                    d.executeUpdate();
                }
                try (PreparedStatement i = cx.prepareStatement(insert)) {
                    i.setInt(1, repositoryId);
                    i.setString(2, namespace);
                    i.setString(3, path);
                    i.setBytes(4, blob);
                    i.executeUpdate();
                }
                cx.commit();
            } catch (SQLException e) {
                cx.rollback();
                throw e;
            } finally {
                cx.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public @Override void putBlob(String namespace, String path, InputStream blob) {
        Objects.requireNonNull(namespace, "namespace can't be null");
        byte[] bytes;
        try {
            bytes = ByteStreams.toByteArray(blob);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        putBlob(namespace, path, bytes);
    }

    public @Override void removeBlob(final String namespace, final String path) {
        Objects.requireNonNull(namespace, "namespace can't be null");
        final String delete = format(
                "LOCK TABLE %s IN SHARE ROW EXCLUSIVE MODE;"
                        + "DELETE FROM %s WHERE repository = ? AND namespace = ? AND path = ?",
                blobsTable, blobsTable);

        try (Connection cx = env.getConnection()) {
            cx.setAutoCommit(false);
            try {
                try (PreparedStatement d = cx.prepareStatement(delete)) {
                    d.setInt(1, repositoryId);
                    d.setString(2, namespace);
                    d.setString(3, path);
                    d.executeUpdate();
                }
                cx.commit();
            } catch (SQLException e) {
                cx.rollback();
                throw e;
            } finally {
                cx.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public @Override void removeBlobs(final String namespace) {
        Objects.requireNonNull(namespace, "namespace can't be null");
        final String delete = format("DELETE FROM %s WHERE repository = ? AND namespace = ?",
                blobsTable);

        try (Connection cx = env.getConnection()) {
            cx.setAutoCommit(false);
            try {
                try (PreparedStatement d = cx.prepareStatement(delete)) {
                    d.setInt(1, repositoryId);
                    d.setString(2, namespace);
                    d.executeUpdate();
                }
                cx.commit();
            } catch (SQLException e) {
                cx.rollback();
                throw e;
            } finally {
                cx.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
