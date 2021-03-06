/* Copyright (c) 2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.plumbing.merge;

import static java.util.Objects.requireNonNull;
import static org.locationtech.geogig.base.Preconditions.checkArgument;
import static org.locationtech.geogig.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.locationtech.geogig.feature.PropertyDescriptor;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevFeature;
import org.locationtech.geogig.model.RevFeatureBuilder;
import org.locationtech.geogig.model.RevFeatureType;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.plumbing.DiffFeature;
import org.locationtech.geogig.plumbing.diff.AttributeDiff;
import org.locationtech.geogig.plumbing.diff.FeatureDiff;
import org.locationtech.geogig.plumbing.merge.DiffMergeFeaturesOp.DiffMergeFeatureResult;
import org.locationtech.geogig.repository.impl.AbstractGeoGigOp;
import org.locationtech.geogig.storage.BulkOpListener;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.locationtech.jts.geom.Geometry;

import com.google.common.collect.Maps;

/**
 * Takes a feature reference to a common ancestor and the two ends of a feature merge and produces a
 * {@link DiffMergeFeatureResult result} that indicates whether the features conflict, or can be
 * merged, and in that case can create the merged feature.
 * <p>
 * 
 * @implNote this operation is like combining two {@link DiffFeature} and one
 *           {@link MergeFeaturesOp} commands into one, with the benefit of less calls the
 *           {@link ObjectDatabase}, and was created to improve the performance of
 *           {@link ReportMergeScenarioOp} when dealing with features that changed at both ends of
 *           the merge.
 * 
 * @see ReportMergeScenarioOp
 */
public class DiffMergeFeaturesOp extends AbstractGeoGigOp<DiffMergeFeatureResult> {

    /**
     * The result of creating the {@link FeatureDiff diff} between the "ours" and "theirs" versions
     * of a feature while merging a branch.
     *
     */
    public static class DiffMergeFeatureResult {

        private FeatureDiff mergeIntoDiff;

        private FeatureDiff toMergeDiff;

        public DiffMergeFeatureResult(FeatureDiff mergeIntoDiff, FeatureDiff toMergeDiff) {
            this.mergeIntoDiff = mergeIntoDiff;
            this.toMergeDiff = toMergeDiff;
        }

        /**
         * @return {@code true} if the result is a merged feature
         */
        public boolean isMerge() {
            return !toMergeDiff.equals(mergeIntoDiff);
        }

        /**
         * Returns {@code true} if there's a conflict in merging the two features.
         * <p>
         * A conflict can happen if the feature types are incompatible or both changed the same
         * feature attribute with different values.
         * 
         * @return {@code true} if the features can't be merged due to a conflict.
         */
        public boolean isConflict() {
            if (mergeIntoDiff.conflicts(toMergeDiff)) {
                return true;
            }
            if (!featureTypesMatch()) {
                return true;
            }
            return false;
        }

        private boolean featureTypesMatch() {
            // if the feature types are different we report a conflict and do not
            // try to perform automerge
            RevFeatureType oursft = mergeIntoDiff.getNewFeatureType();
            RevFeatureType theirsft = toMergeDiff.getNewFeatureType();
            return oursft.equals(theirsft);
        }

        /**
         * @return the merged feature.
         * @precondition the feature types are compatible.
         */
        public RevFeature mergedFeature() {
            checkState(featureTypesMatch());
            return merge(mergeIntoDiff, toMergeDiff);
        }

    }

    private NodeRef commonAncestor;

    private NodeRef mergeInto;

    private NodeRef toMerge;

    public DiffMergeFeaturesOp setCommonAncestor(NodeRef commonAncestor) {
        this.commonAncestor = commonAncestor;
        return this;
    }

    /**
     * @param ours the ref that points to the "old" version of the feature to compare
     * @return {@code this}
     */
    public DiffMergeFeaturesOp setMergeInto(NodeRef ours) {
        this.mergeInto = ours;
        return this;
    }

    /**
     * @param mergeIntoNodeRef the ref that points to the "old" version of the feature to compare
     * @return {@code this}
     */
    public DiffMergeFeaturesOp setToMerge(NodeRef theirs) {
        this.toMerge = theirs;
        return this;
    }

    protected @Override DiffMergeFeatureResult _call() {
        checkPreconditions(commonAncestor, mergeInto, toMerge);

        final Map<ObjectId, RevObject> objects = getObjects();

        RevFeature ancestorF = (RevFeature) objects.get(commonAncestor.getObjectId());
        RevFeature mergeIntoF = (RevFeature) objects.get(mergeInto.getObjectId());
        RevFeature toMergeF = (RevFeature) objects.get(toMerge.getObjectId());

        RevFeatureType ancestorT = (RevFeatureType) objects.get(commonAncestor.metadataId());
        RevFeatureType mergetIntoT = (RevFeatureType) objects.get(mergeInto.metadataId());
        RevFeatureType toMergeT = (RevFeatureType) objects.get(toMerge.metadataId());

        FeatureDiff mergeIntoDiff = compare(ancestorF, mergeIntoF, ancestorT, mergetIntoT);
        FeatureDiff toMergeDiff = compare(ancestorF, toMergeF, ancestorT, toMergeT);

        return new DiffMergeFeatureResult(mergeIntoDiff, toMergeDiff);
    }

    private Map<ObjectId, RevObject> getObjects() {

        final ObjectId ancestorMetadataId = commonAncestor.metadataId();
        final ObjectId mergetIntoMetadataId = mergeInto.metadataId();
        final ObjectId toMergeMetadataId = toMerge.metadataId();

        final ObjectId ancestorFeatureId = commonAncestor.getObjectId();
        final ObjectId featureAId = mergeInto.getObjectId();
        final ObjectId featureBId = toMerge.getObjectId();

        Set<ObjectId> ids = Arrays
                .asList(ancestorMetadataId, mergetIntoMetadataId, toMergeMetadataId,
                        ancestorFeatureId, featureAId, featureBId)
                .stream().collect(Collectors.toSet());

        Iterator<RevObject> objsit = objectDatabase().getAll(ids, BulkOpListener.NOOP_LISTENER);
        Map<ObjectId, RevObject> map = Maps.uniqueIndex(objsit, RevObject::getId);

        if (ids.size() != map.size()) {
            ids.forEach((id) -> checkState(map.containsKey(id), "Invalid reference: %s", id));
        }
        return map;
    }

    private void checkPreconditions(NodeRef commonAncestor, NodeRef mergeInto, NodeRef toMerge) {

        requireNonNull(commonAncestor, "common ancestor version not specified");
        requireNonNull(mergeInto, "old version not specified");
        requireNonNull(toMerge, "new version not specified");

        checkArgument(commonAncestor.path().equals(mergeInto.path())
                && commonAncestor.path().equals(toMerge.path()));
    }

    static RevFeature merge(FeatureDiff mergeIntoDiff, FeatureDiff toMergeDiff) {

        if (!mergeIntoDiff.getNewFeatureType().equals(toMergeDiff.getNewFeatureType())) {
            throw new IllegalArgumentException(
                    String.format("Non-matching feature types. Cannot merge. Left: %s, right: %s",
                            mergeIntoDiff.getNewFeatureType().getId(),
                            toMergeDiff.getNewFeatureType().getId()));
        }

        RevFeatureType featureType = mergeIntoDiff.getNewFeatureType();

        Map<PropertyDescriptor, AttributeDiff> leftDiffs = mergeIntoDiff.getDiffs();
        Map<PropertyDescriptor, AttributeDiff> rightDiffs = toMergeDiff.getDiffs();

        List<PropertyDescriptor> descriptors = new ArrayList<>(featureType.type().getDescriptors());

        final List<Object> ancestorValues;
        ancestorValues = getAncestorValues(mergeIntoDiff, toMergeDiff, descriptors);

        RevFeatureBuilder mergedValues = RevFeature.builder();

        for (int i = 0; i < descriptors.size(); i++) {
            final PropertyDescriptor descriptor = descriptors.get(i);
            final boolean isGeom = descriptor.isGeometryDescriptor();

            @Nullable
            Object ancestorValue = ancestorValues.get(i);
            @Nullable
            AttributeDiff leftAttDiff = leftDiffs.get(descriptor);
            @Nullable
            AttributeDiff rightAttDiff = rightDiffs.get(descriptor);

            Object valueA = leftAttDiff == null ? null : leftAttDiff.getNewValue();
            Object valueB = rightAttDiff == null ? null : rightAttDiff.getNewValue();

            Object merged;

            if (leftAttDiff == null) {
                merged = rightAttDiff == null ? ancestorValue : valueB;
            } else if (rightAttDiff == null) {
                merged = valueA;
            } else if (valueEquals(isGeom, valueA, valueB)) {
                merged = valueA;
            } else {
                // both modified the attribute and didn't set the same value
                merged = valueA;
            }
            mergedValues.addValue(merged);
        }

        return mergedValues.build();
    }

    private static List<Object> getAncestorValues(FeatureDiff mergeIntoDiff,
            FeatureDiff toMergeDiff, List<PropertyDescriptor> descriptors) {
        final List<Object> ancestorValues;
        {
            RevFeature ancestor = mergeIntoDiff.getOldFeature() == null
                    ? toMergeDiff.getOldFeature()
                    : mergeIntoDiff.getOldFeature();
            if (ancestor == null) {
                Object[] array = new Optional[descriptors.size()];
                ancestorValues = Arrays.asList(array);
            } else {
                ancestorValues = new ArrayList<>(ancestor.size());
                ancestor.forEach((v) -> ancestorValues.add(v));
            }
        }
        return ancestorValues;
    }

    private static boolean valueEquals(boolean isGeom, @Nullable Object v1, @Nullable Object v2) {
        return isGeom ? geomEquals((Geometry) v1, (Geometry) v2) : Objects.equals(v1, v2);
    }

    private static boolean geomEquals(@Nullable Geometry g1, @Nullable Geometry g2) {
        if (g1 == null || g2 == null) {
            return g1 == null && g2 == null;
        }
        return g1.equalsExact(g2);
    }

    private FeatureDiff compare(RevFeature oldRevFeature, RevFeature newRevFeature,
            RevFeatureType oldRevFeatureType, RevFeatureType newRevFeatureType) {

        return new FeatureDiff(mergeInto.path(), newRevFeature, oldRevFeature, newRevFeatureType,
                oldRevFeatureType, false);
    }

}
