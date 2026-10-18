# Progress history

The Progress tab reads `sukham_progress.db`, an app-private SQLite database.
It follows the app's existing SQLite approach and does not change the profile DB.
`ProgressRepository` owns a serial background writer and observable state; leaving
an individual screen does not cancel its final save.

Tables:
- `activity_sessions`: stable UUID, activity type, title, timestamps, captured
  timezone, status, active/planned duration, sampled video coverage and coaching notes.
- `practice_days`: active milliseconds per session and local calendar day.
- `session_poses`: pose labels and coaching/review counts per session.

Checkpoints update one session, never append another copy. Daily time is added
only for the increase in active milliseconds. Terminal states ignore stale
pause/progress updates. Startup marks unfinished sessions interrupted. Midnight
splitting uses calendar-zone boundaries, including daylight-saving changes.

Sources:
- Breathing: a new UUID for each run; checkpoints every five seconds and on pause,
  backgrounding, completion, end or navigation. A resumed run retains its UUID.
- Live Coach: starts only after model initialization; monotonic foreground session
  time excludes background pauses. Leaving/stopping completes that session.
  Highlights count completed coaching checks, not inferred pose hold durations.
- Photo: one UUID per inference, saved at completion or failure/interruption.
- Video: one UUID per audit, frame checkpoints and terminal status; coverage is
  based on successfully processed sampled-frame intervals. Video footage is
  separate from active practice minutes.
- Programs: the current builder only chooses poses; it cannot honestly generate
  completed practice sessions. Its card stays empty until a guided program runner
  records `PROGRAM` sessions. Selecting/adding poses never counts as practice.

Summary minutes include positive active time in Programs/Live/Breathing, including
partial sessions. Completed counts include only COMPLETED records. Reviews never
add practice minutes. Daily bars use recorded local days; session/review counts
are assigned to their start day. Cross-midnight time can therefore contribute to a
period without adding a session that started in the preceding period.

Settings stay separate:
- `progress_preferences`: selected Week / Month / All time period.
- Existing `breathing` preferences: sound and last duration (1/2/3/5 minutes).
- Theme continues using the existing DataStore-backed profile setting.

No historical/sample activity is seeded. Photo/video media and camera frames are
not copied into this DB; only text feedback and pose labels are retained.

Verification: JVM clock/aggregation/date-boundary tests plus isolated Android
SQLite tests for checkpoint idempotence, terminal-state protection, reopen
persistence, and separation of reviews from practice time.
