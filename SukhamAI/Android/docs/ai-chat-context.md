# AI tab and local context

The AI navigation item opens `SukhamAiScreen`, which uses `LlmChatScreen` and the existing model download/initialization flow. The Home Live Coach card still opens the camera coach. The chat tab preserves the Home / Programs / AI / Progress / Profile navigation.

Before each inference and retry, chat reads a fresh, bounded snapshot on the IO dispatcher:

- DataStore: saved appearance preference.
- SharedPreferences (`breathing`): last selected breathing duration, if saved.
- Profile SQLite: edited name, focus, height, weight and gender only. Fields matching the demonstration profile are omitted because their provenance is unknown. Seeded scores, streaks and device values are excluded.
- Progress SQLite repository: actual practice time, active days, completed sessions, and review counts for the last seven days; up to five recent activity records with pose names and up to 400 characters of saved coaching notes each.

Only the AI tab enables this context. Image/audio task prompts remain unchanged. The user's original message is shown in the transcript and input history; context is added only to the model input. Secrets, authentication tokens, media and unrelated input history are never read by this context builder.

The prompt distinguishes partial practice from completed sessions, coaching frequency from accuracy, and unavailable history from zero history. Saved text is presented as JSON data, not instructions. Program selection is not described as completed guided practice. The assistant is instructed not to infer medical conditions or invent missing personal facts.

This supplies useful recorded context; it does not guarantee model correctness. End-to-end answers require a downloaded, compatible chat model.
