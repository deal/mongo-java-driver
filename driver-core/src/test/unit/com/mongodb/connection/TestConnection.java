/*
 * Copyright (c) 2008-2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.connection;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.WriteConcernResult;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.DeleteRequest;
import com.mongodb.bulk.InsertRequest;
import com.mongodb.bulk.UpdateRequest;
import org.bson.BsonDocument;
import org.bson.FieldNameValidator;
import org.bson.codecs.Decoder;

import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
class TestConnection implements Connection, AsyncConnection {
    private final InternalConnection internalConnection;
    private final ProtocolExecutor executor;
    private LegacyProtocol enqueuedLegacyProtocol;
    private CommandProtocol enqueuedCommandProtocol;

    TestConnection(final InternalConnection internalConnection, final ProtocolExecutor executor) {
        this.internalConnection = internalConnection;
        this.executor = executor;
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public TestConnection retain() {
        return this;
    }

    @Override
    public void release() {

    }

    @Override
    public ConnectionDescription getDescription() {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public WriteConcernResult insert(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                     final List<InsertRequest> inserts) {
        return executeEnqueuedLegacyProtocol();
    }

    @Override
    public void insertAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                            final List<InsertRequest> inserts, final SingleResultCallback<WriteConcernResult> callback) {
        executeEnqueuedLegacyProtocolAsync(callback);
    }

    @Override
    public WriteConcernResult update(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                     final List<UpdateRequest> updates) {
        return executeEnqueuedLegacyProtocol();
    }

    @Override
    public void updateAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                            final List<UpdateRequest> updates, final SingleResultCallback<WriteConcernResult> callback) {
        executeEnqueuedLegacyProtocolAsync(callback);
    }

    @Override
    public WriteConcernResult delete(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                     final List<DeleteRequest> deletes) {
        return executeEnqueuedLegacyProtocol();
    }

    @Override
    public void deleteAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                            final List<DeleteRequest> deletes,
                            final SingleResultCallback<WriteConcernResult> callback) {
        executeEnqueuedLegacyProtocolAsync(callback);
    }

    @Override
    public BulkWriteResult insertCommand(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                         final List<InsertRequest> inserts) {
        throw new UnsupportedOperationException("Deprecated method called directly - this should have been updated");
    }

    @Override
    public BulkWriteResult insertCommand(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                         final Boolean bypassDocumentValidation, final List<InsertRequest> inserts) {
        throw new UnsupportedOperationException("Deprecated method called directly - this should have been updated");
    }

    @Override
    public BulkWriteResult insertCommand(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                         final Boolean bypassDocumentValidation, final List<InsertRequest> inserts,
                                         final SessionContext sessionContext) {
        return executeEnqueuedCommandBasedProtocol(sessionContext);
    }

    @Override
    public void insertCommandAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                   final List<InsertRequest> inserts, final SingleResultCallback<BulkWriteResult> callback) {
        throw new UnsupportedOperationException("Deprecated method called directly - this should have been updated");
    }

    @Override
    public void insertCommandAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                   final Boolean bypassDocumentValidation, final List<InsertRequest> inserts,
                                   final SingleResultCallback<BulkWriteResult> callback) {
        throw new UnsupportedOperationException("Deprecated method called directly - this should have been updated");
    }

    @Override
    public void insertCommandAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                   final Boolean bypassDocumentValidation, final List<InsertRequest> inserts,
                                   final SessionContext sessionContext, final SingleResultCallback<BulkWriteResult> callback) {
        executeEnqueuedCommandBasedProtocolAsync(null, callback);
    }

    @Override
    public BulkWriteResult updateCommand(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                         final List<UpdateRequest> updates) {
        throw new UnsupportedOperationException("Deprecated method called directly - this should have been updated");
    }

    @Override
    public BulkWriteResult updateCommand(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                         final Boolean bypassDocumentValidation, final List<UpdateRequest> updates) {
        throw new UnsupportedOperationException("Deprecated method called directly - this should have been updated");
    }

    @Override
    public BulkWriteResult updateCommand(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                         final Boolean bypassDocumentValidation, final List<UpdateRequest> updates,
                                         final SessionContext sessionContext) {
        return executeEnqueuedCommandBasedProtocol(sessionContext);
    }

    @Override
    public void updateCommandAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                   final List<UpdateRequest> updates, final SingleResultCallback<BulkWriteResult> callback) {
        throw new UnsupportedOperationException("Deprecated method called directly - this should have been updated");
    }

    @Override
    public void updateCommandAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                   final Boolean bypassDocumentValidation, final List<UpdateRequest> updates,
                                   final SingleResultCallback<BulkWriteResult> callback) {
        throw new UnsupportedOperationException("Deprecated method called directly - this should have been updated");
    }

    @Override
    public void updateCommandAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                   final Boolean bypassDocumentValidation, final List<UpdateRequest> updates,
                                   final SessionContext sessionContext, final SingleResultCallback<BulkWriteResult> callback) {
        executeEnqueuedCommandBasedProtocolAsync(null, callback);
    }

    @Override
    public BulkWriteResult deleteCommand(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                         final List<DeleteRequest> deletes) {
        throw new UnsupportedOperationException("Deprecated method called directly - this should have been updated");
    }

    @Override
    public BulkWriteResult deleteCommand(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                         final List<DeleteRequest> deletes, final SessionContext sessionContext) {
        return executeEnqueuedCommandBasedProtocol(sessionContext);
    }

    @Override
    public void deleteCommandAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                   final List<DeleteRequest> deletes,
                                   final SingleResultCallback<BulkWriteResult> callback) {
        throw new UnsupportedOperationException("Deprecated method called directly - this should have been updated");
    }

    @Override
    public void deleteCommandAsync(final MongoNamespace namespace, final boolean ordered, final WriteConcern writeConcern,
                                   final List<DeleteRequest> deletes, final SessionContext sessionContext,
                                   final SingleResultCallback<BulkWriteResult> callback) {
        executeEnqueuedCommandBasedProtocolAsync(null, callback);
    }

    @Override
    public <T> T command(final String database, final BsonDocument command, final boolean slaveOk,
                         final FieldNameValidator fieldNameValidator,
                         final Decoder<T> commandResultDecoder) {
        return executeEnqueuedCommandBasedProtocol(null);
    }

    @Override
    public <T> T command(final String database, final BsonDocument command,
                         final ReadPreference readPreference, final FieldNameValidator fieldNameValidator,
                         final Decoder<T> commandResultDecoder, final SessionContext sessionContext) {
        return executeEnqueuedCommandBasedProtocol(sessionContext);
    }

    @Override
    public <T> void commandAsync(final String database, final BsonDocument command, final boolean slaveOk,
                                 final FieldNameValidator fieldNameValidator,
                                 final Decoder<T> commandResultDecoder, final SingleResultCallback<T> callback) {
        executeEnqueuedCommandBasedProtocolAsync(null, callback);
    }

    @Override
    public <T> void commandAsync(final String database, final BsonDocument command, final ReadPreference readPreference,
                                 final FieldNameValidator fieldNameValidator, final Decoder<T> commandResultDecoder,
                                 final SessionContext sessionContext, final SingleResultCallback<T> callback) {
        executeEnqueuedCommandBasedProtocolAsync(sessionContext, callback);
    }

    @Override
    public <T> QueryResult<T> query(final MongoNamespace namespace, final BsonDocument queryDocument, final BsonDocument fields,
                                    final int numberToReturn, final int skip,
                                    final boolean slaveOk, final boolean tailableCursor, final boolean awaitData,
                                    final boolean noCursorTimeout,
                                    final boolean partial, final boolean oplogReplay, final Decoder<T> resultDecoder) {
        return executeEnqueuedLegacyProtocol();
    }

    @Override
    public <T> QueryResult<T> query(final MongoNamespace namespace, final BsonDocument queryDocument, final BsonDocument fields,
                                    final int skip, final int limit,
                                    final int batchSize, final boolean slaveOk, final boolean tailableCursor, final boolean awaitData,
                                    final boolean noCursorTimeout,
                                    final boolean partial, final boolean oplogReplay, final Decoder<T> resultDecoder) {
        return executeEnqueuedLegacyProtocol();
    }

    @Override
    public <T> void queryAsync(final MongoNamespace namespace, final BsonDocument queryDocument, final BsonDocument fields,
                               final int numberToReturn, final int skip,
                               final boolean slaveOk, final boolean tailableCursor, final boolean awaitData, final boolean noCursorTimeout,
                               final boolean partial,
                               final boolean oplogReplay, final Decoder<T> resultDecoder,
                               final SingleResultCallback<QueryResult<T>> callback) {
        executeEnqueuedLegacyProtocolAsync(callback);
    }

    @Override
    public <T> void queryAsync(final MongoNamespace namespace, final BsonDocument queryDocument, final BsonDocument fields, final int skip,
                               final int limit,
                               final int batchSize, final boolean slaveOk, final boolean tailableCursor, final boolean awaitData,
                               final boolean noCursorTimeout,
                               final boolean partial, final boolean oplogReplay, final Decoder<T> resultDecoder,
                               final SingleResultCallback<QueryResult<T>> callback) {
        executeEnqueuedLegacyProtocolAsync(callback);
    }

    @Override
    public <T> QueryResult<T> getMore(final MongoNamespace namespace, final long cursorId, final int numberToReturn,
                                      final Decoder<T> resultDecoder) {
        return executeEnqueuedLegacyProtocol();
    }

    @Override
    public <T> void getMoreAsync(final MongoNamespace namespace, final long cursorId, final int numberToReturn,
                                 final Decoder<T> resultDecoder,
                                 final SingleResultCallback<QueryResult<T>> callback) {
        executeEnqueuedLegacyProtocolAsync(callback);
    }

    @Override
    public void killCursor(final List<Long> cursors) {
        executeEnqueuedLegacyProtocol();
    }

    @Override
    public void killCursor(final MongoNamespace namespace, final List<Long> cursors) {
        executeEnqueuedLegacyProtocol();
    }

    @Override
    public void killCursorAsync(final List<Long> cursors, final SingleResultCallback<Void> callback) {
        executeEnqueuedLegacyProtocolAsync(callback);
    }

    @Override
    public void killCursorAsync(final MongoNamespace namespace, final List<Long> cursors, final SingleResultCallback<Void> callback) {
        executeEnqueuedLegacyProtocolAsync(callback);
    }

    @SuppressWarnings("unchecked")
    private <T> T executeEnqueuedLegacyProtocol() {
        return (T) executor.execute(enqueuedLegacyProtocol, internalConnection);
    }

    @SuppressWarnings("unchecked")
    private <T> void executeEnqueuedLegacyProtocolAsync(final SingleResultCallback<T> callback) {
        executor.executeAsync(enqueuedLegacyProtocol, internalConnection, callback);
    }

    @SuppressWarnings("unchecked")
    private <T> T executeEnqueuedCommandBasedProtocol(final SessionContext sessionContext) {
        return (T) executor.execute(enqueuedCommandProtocol, internalConnection, sessionContext);
    }

    @SuppressWarnings("unchecked")
    private <T> void executeEnqueuedCommandBasedProtocolAsync(final SessionContext sessionContext, final SingleResultCallback<T> callback) {
        executor.executeAsync(enqueuedCommandProtocol, internalConnection, sessionContext, callback);
    }

    void enqueueProtocol(final LegacyProtocol protocol) {
        enqueuedLegacyProtocol = protocol;
    }

    void enqueueProtocol(final CommandProtocol protocol) {
        enqueuedCommandProtocol = protocol;
    }
}
