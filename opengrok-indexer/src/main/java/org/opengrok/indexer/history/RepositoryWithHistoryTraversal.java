/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 */

package org.opengrok.indexer.history;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;

public abstract class RepositoryWithHistoryTraversal extends RepositoryWithPerPartesHistory {
    private static final long serialVersionUID = -1L;

    public static class CommitInfo {
        String revision;
        Date date;
        String authorName;
        String authorEmail;
        String message;

        CommitInfo(String revision, Date date, String authorName, String authorEmail, String message) {
            this.revision = revision;
            this.date = date;
            this.authorName = authorName;
            this.authorEmail = authorEmail;
            this.message = message;
        }
    }

    public static class ChangesetInfo {
        CommitInfo commit;
        public SortedSet<String> files;
        public Set<String> renamedFiles;
        public Set<String> deletedFiles;

        ChangesetInfo(CommitInfo commit) {
            this.commit = commit;
        }

        ChangesetInfo(CommitInfo commit, SortedSet<String> files, Set<String> renamedFiles, Set<String> deletedFiles) {
            this.commit = commit;
            this.files = files;
            this.renamedFiles = renamedFiles;
            this.deletedFiles = deletedFiles;
        }
    }

    /**
     * TODO: document
     * @param file
     * @param sinceRevision
     * @param tillRevision
     * @param numCommits
     * @param visitor
     * @param getAll
     * @throws HistoryException
     */
    public abstract void traverseHistory(File file, String sinceRevision, @Nullable String tillRevision,
                         Integer numCommits, Consumer<ChangesetInfo> visitor, boolean getAll) throws HistoryException;
}
