/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Victor Olaya (Boundless) - initial implementation
 */
package org.locationtech.geogig.plumbing.merge;

import java.nio.charset.StandardCharsets;

import org.locationtech.geogig.porcelain.MergeOp;
import org.locationtech.geogig.repository.impl.AbstractGeoGigOp;
import org.locationtech.geogig.storage.BlobStore;

public class SaveMergeCommitMessageOp extends AbstractGeoGigOp<Void> {

    private String message;

    public SaveMergeCommitMessageOp setMessage(String message) {
        this.message = message;
        return this;
    }

    protected @Override Void _call() {
        BlobStore blobStore = context().blobStore();
        byte[] blob = message.getBytes(StandardCharsets.UTF_8);
        blobStore.putBlob(MergeOp.MERGE_MSG, blob);
        return null;
    }

}
