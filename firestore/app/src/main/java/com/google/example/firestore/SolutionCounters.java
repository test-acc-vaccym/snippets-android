package com.google.example.firestore;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * https://firebase.google.com/docs/firestore/solutions/counters
 */
public class SolutionCounters {

    private FirebaseFirestore db;

    // [START counter_classes]
    // counters/${ID}
    public class Counter {
        int numShards;

        public Counter(int numShards) {
            this.numShards = numShards;
        }
    }

    // counters/${ID}/shards/${NUM}
    public class Shard {
        int count;

        public Shard(int count) {
            this.count = count;
        }
    }
    // [END counter_classes]

    // [START create_counter]
    public Task<Void> createCounter(final DocumentReference ref, final int numShards) {
        // Initialize the counter document, then initialize each shard.
        return ref.set(new Counter(numShards))
                .continueWithTask(new Continuation<Void, Task<Void>>() {
                    @Override
                    public Task<Void> then(@NonNull Task<Void> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        List<Task<Void>> tasks = new ArrayList<>();

                        // Initialize each shard with count=0
                        for (int i = 0; i < numShards; i++) {
                            Task<Void> makeShard = ref.collection("shards")
                                    .document(String.valueOf(i))
                                    .set(new Shard(0));

                            tasks.add(makeShard);
                        }

                        return Tasks.whenAll(tasks);
                    }
                });
    }
    // [END create_counter]

    // [START increment_counter]
    public Task<Void> incrementCounter(final DocumentReference ref, final int numShards) {
        int shardId = (int) Math.floor(Math.random() * numShards);
        final DocumentReference shardRef = ref.collection("shards").document(String.valueOf(shardId));

        return db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                Shard shard = transaction.get(shardRef).toObject(Shard.class);
                shard.count += 1;

                transaction.set(shardRef, shard);
                return null;
            }
        });
    }
    // [END increment_counter]

}