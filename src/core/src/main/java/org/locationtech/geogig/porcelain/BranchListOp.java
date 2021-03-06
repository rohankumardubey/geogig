/* Copyright (c) 2012-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.porcelain;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.plumbing.ForEachRef;
import org.locationtech.geogig.repository.impl.AbstractGeoGigOp;

/**
 * Creates a new head ref (branch) pointing to the specified tree-ish or the current HEAD if no
 * tree-ish was specified.
 * <p>
 */
public class BranchListOp extends AbstractGeoGigOp<List<Ref>> {

    private boolean remotes;

    private boolean locals;

    public BranchListOp() {
        locals = true;
        remotes = false;
    }

    public BranchListOp setRemotes(boolean remotes) {
        this.remotes = remotes;
        return this;
    }

    public BranchListOp setLocal(boolean locals) {
        this.locals = locals;
        return this;
    }

    protected List<Ref> _call() {

        final Predicate<Ref> filter = input -> {

            if (locals && input.getName().startsWith(Ref.HEADS_PREFIX)) {
                return true;
            }
            if (remotes && input.getName().startsWith(Ref.REMOTES_PREFIX)) {
                return true;
            }
            return false;
        };

        List<Ref> refs = command(ForEachRef.class).setFilter(filter).call().stream()
                .collect(Collectors.toList());
        Collections.sort(refs);
        return refs;
    }

}
