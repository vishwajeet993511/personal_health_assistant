# Programs pose library

Source: [Pocket Yoga pose dictionary](https://www.pocketyoga.com/pose/), retrieved October 17, 2026.

The bundled catalog contains 568 entries, including pose variations, from the public `/poses.json` catalog. It stores English and Sanskrit names, category, difficulty, illustration resource name, and the source page URL. Descriptions and benefit claims are not copied.

Original downloaded illustrations are Pocket Yoga artwork, © Rainfrog, LLC. Premium sibling assets are newly generated Sukham illustrations based on the pose geometry and the approved Ankle Stretch art direction. The resource map selects a premium sibling when present and retains the original for unfinished replacements. Exact coverage is recorded in `pose-redesign/progress.json`. Source attribution remains in the library footer and each pose links to its source guide.

Images are bundled at up to 560 pixels per side for offline display. Cards use `ContentScale.Fit` so limbs stay visible. `ProgramPoseImages.kt` references resources explicitly so resource shrinking preserves them.

To refresh, download the public catalog to a local JSON file and run `python3 scripts/import_pose_catalog.py /path/to/poses.json`. The script downloads and resizes the illustrations using curl and macOS sips, then regenerates the catalog and resource map.

## Sukham Ankle Stretch redesign

The Ankle Stretch card uses `program_pose_anklestretch_premium.png`, created with the built-in image generation tool using the previous illustration only as a pose reference. The same art direction is being applied across the full library; see `pose-redesign/progress.json` for current replacement coverage. The original asset remains available. The import script preserves premium sibling overrides.

Prompt direction: preserve the left-facing kneeling ankle-stretch body position; redesign as a large, full-body premium editorial studio illustration, warm medium-brown skin, dark low bun, matte forest-sage activewear, realistic anatomy and fabric, soft studio lighting, white background, subtle contact shadow, no text or logos.
