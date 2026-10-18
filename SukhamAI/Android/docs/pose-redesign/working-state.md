# Replacement pass status

435 of 568 images are generated, visually reviewed, optimized, and wired into the Android Programs cards. 133 remain pending because the built-in image generation service returned usage_limit_reached (HTTP 429). Service-reported reset: October 18, 2026 at approximately 23:39 Asia/Kolkata.

No image-generation workers remain running. All originals are preserved. Pending images continue to render their original illustration. See progress.json and remaining.json for exact per-pose coverage.

Continue only pending entries with the built-in image tool after availability returns, or use the skill CLI fallback only if the user explicitly chooses it and has a locally configured OPENAI_API_KEY. Never silently switch workflows. Refer to art-direction.md for the approved prompt and reference conventions. Original PNGs and the approved Ankle Stretch style reference remain under src/app/src/main/res/drawable-nodpi.

Run scripts/optimize_premium_pose_images.py using the bundled Python (Pillow), then scripts/refresh_premium_pose_images.py after generation. Source and output contact sheets can be built with scripts/pose_review_sheet.py.

Archer and inverted Embryo received geometry corrections and are reviewed. Firefly preparation and Cow succeeded after neutral exercise-focused retries. There are no remaining moderation-related gaps; pending entries are due to the generation quota.

Validation: :app:compileDebugKotlin passed with Android Studio Java 21. All 568 mappings resolve, premium resource stems are unique, and git diff --check passes. The 435 premium card assets total approximately 11.9 MB. See premium-preview.jpg for a six-pose sample.
