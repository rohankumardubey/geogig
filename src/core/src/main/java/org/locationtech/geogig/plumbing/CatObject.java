/* Copyright (c) 2012-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.plumbing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.locationtech.geogig.base.Preconditions;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.repository.impl.AbstractGeoGigOp;
import org.locationtech.geogig.storage.text.TextRevObjectSerializer;

import lombok.NonNull;

/**
 * Provides content information for repository objects
 */
public class CatObject extends AbstractGeoGigOp<CharSequence> {

    private Supplier<? extends RevObject> object;

    public CatObject setObject(Supplier<? extends RevObject> object) {
        this.object = object;
        return this;
    }

    public CatObject setObject(@NonNull RevObject object) {
        return setObject(() -> object);
    }

    protected @Override CharSequence _call() {
        Preconditions.checkState(object != null);
        RevObject revObject = object.get();

        TextRevObjectSerializer serializer = TextRevObjectSerializer.INSTANCE;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String s = "id\t" + revObject.getId().toString() + "\n";
        OutputStreamWriter streamWriter = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        try {
            streamWriter.write(s);
            streamWriter.flush();
            serializer.write(revObject, output);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot print object: " + revObject.getId().toString(),
                    e);
        }
        return output.toString();
    }
}
