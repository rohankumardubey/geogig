/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Victor Olaya (Boundless) - initial implementation
 */
package org.locationtech.geogig.di;

import org.locationtech.geogig.plumbing.merge.ConflictsCheckOp;
import org.locationtech.geogig.porcelain.ConflictsException;
import org.locationtech.geogig.repository.Command;
import org.locationtech.geogig.repository.impl.AbstractGeoGigOp;

import lombok.NonNull;

/**
 * Intercepts all {@link AbstractGeoGigOp commands} to avoid incompatible running commands while
 * merge or rebase conflicts exist.
 * 
 */
class ConflictInterceptor implements Decorator {

    public @Override boolean canDecorate(Object subject) {
        if (!(subject instanceof Command)) {
            return false;
        }
        // TODO: this is not a very clean way of doing this...
        Class<?> clazz = subject.getClass();
        final boolean canRunDuringConflict = clazz.isAnnotationPresent(CanRunDuringConflict.class);
        return !(clazz.getPackage().getName().contains("plumbing") || canRunDuringConflict);
    }

    @SuppressWarnings("unchecked")
    public @Override Command<?> decorate(@NonNull Object subject) {
        AbstractGeoGigOp<?> operation = (AbstractGeoGigOp<?>) subject;

        Boolean conflicts = operation.command(ConflictsCheckOp.class).call();
        if (conflicts.booleanValue()) {
            throw new ConflictsException(
                    "Cannot run operation while merge or rebase conflicts exist.");
        }
        return (Command<?>) subject;
    }

}
