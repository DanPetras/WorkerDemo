# Demo project to reproduce possible bug in androidx.work

## The problem

It seems that calling `CoroutineWorker.setForeground(foregroundInfo: ForegroundInfo)` from `CoroutineWorker` that was started with constrains set (`WorkRequest.setConstraints()`) cause multiple registrations in `androidx.work.impl.constraints.SharedNetworkCallback`.

This can in turn lead to "NetworkRequestConstraintController didn't receive neither onCapabilitiesChanged/onLost callback, sending `ConstraintsNotMet` after 1000 ms" error.

I added logging breakpoint to the `SharedNetworkCallback.addCallback(...)` function to verify it is being called multiple times (most often 2 times).
It is called exactly once if worker is NOT calling `setForeground()` function.

Attached is output from logcat with worker failing.

## App usage

1. Run the app in debug
2. Allow notifications request
3. Tap "Start worker" button
4. Worker takes 10 seconds to complete its dummy job
5. Observe logcat for error
6. If no error occur, repeat from step 3.

Optionally to reset the worker queue, Tap "Cancel all workers" button (work is enqueued with `enqueueUniqueWork` with `ExistingWorkPolicy.APPEND_OR_REPLACE` on single workId)
